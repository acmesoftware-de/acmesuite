import { useEffect, useMemo, useRef, useState } from 'react'
import type { Contact, Customer, CustomerKind, CustomerStatus, CustomerWrite, Deal, MailThread } from './crmApi'
import { DealMiniList, ThreadPanel, nameInitials } from './DetailPanels'
import { amountOf, fmtTotal } from './pipelineModel'

const KINDS: CustomerKind[] = ['CUSTOMER', 'RESELLER']
const STATUSES: CustomerStatus[] = ['PROSPECT', 'ACTIVE', 'BLOCKED']
const kindLabel = (k: CustomerKind) => (k === 'RESELLER' ? 'Reseller' : 'Kunde')

type EditForm = {
  name: string
  kind: CustomerKind
  status: CustomerStatus
  email: string
  country: string
  parentResellerId: string
}

/** Kunden (companies) as master-detail: list left, selected company + all its data (editable) right. */
export function CustomersView({
  customers,
  contacts,
  deals,
  threads,
  canWrite,
  createTick,
  onCreate,
  onUpdate,
}: {
  customers: Customer[] | null
  contacts: Contact[] | null
  deals: Deal[] | null
  threads: MailThread[] | null
  canWrite: boolean
  createTick: number
  onCreate: (body: CustomerWrite) => Promise<void>
  onUpdate: (id: string, body: CustomerWrite) => Promise<void>
}) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ name: '', kind: 'CUSTOMER' as CustomerKind, status: 'PROSPECT' as CustomerStatus, country: '' })
  const [editing, setEditing] = useState(false)
  const [edit, setEdit] = useState<EditForm | null>(null)

  const seen = useRef(createTick)
  useEffect(() => {
    if (createTick !== seen.current) {
      seen.current = createTick
      if (canWrite) setCreating(true)
    }
  }, [createTick, canWrite])

  const selected = useMemo(
    () => customers?.find((c) => c.id === selectedId) ?? customers?.[0] ?? null,
    [customers, selectedId],
  )
  const selId = selected?.id
  useEffect(() => setEditing(false), [selId]) // leave edit mode when switching records

  const custContacts = useMemo(() => contacts?.filter((c) => c.customerId === selId) ?? [], [contacts, selId])
  const custDeals = useMemo(() => deals?.filter((d) => d.customerId === selId) ?? [], [deals, selId])
  const custThreads = useMemo(() => threads?.filter((t) => t.customerId === selId) ?? [], [threads, selId])
  const pipelineSum = custDeals.reduce((acc, d) => acc + amountOf(d), 0)
  const resellers = useMemo(() => customers?.filter((c) => c.kind === 'RESELLER') ?? [], [customers])
  const nameOf = (id?: string | null) => customers?.find((c) => c.id === id)?.name ?? null

  async function submit() {
    await onCreate({ name: form.name.trim(), kind: form.kind, status: form.status, country: form.country.trim() || null })
    setForm({ name: '', kind: 'CUSTOMER', status: 'PROSPECT', country: '' })
    setCreating(false)
  }

  function startEdit() {
    if (!selected) return
    setEdit({
      name: selected.name,
      kind: selected.kind,
      status: selected.status,
      email: selected.email ?? '',
      country: selected.country ?? '',
      parentResellerId: selected.parentResellerId ?? '',
    })
    setEditing(true)
  }

  async function saveEdit() {
    if (!selected || !edit) return
    await onUpdate(selected.id, {
      name: edit.name.trim(),
      kind: edit.kind,
      status: edit.status,
      email: edit.email.trim() || null,
      country: edit.country.trim() || null,
      parentResellerId: edit.parentResellerId || null,
      priceListId: selected.priceListId ?? null,
    })
    setEditing(false)
  }

  if (!customers) return <div className="acme-empty">Kunden werden geladen…</div>

  return (
    <div className="acme-md">
      <div className="acme-md-list">
        {creating && (
          <div className="acme-form-card">
            <label className="acme-field">
              <span className="acme-label">Firma</span>
              <input className="acme-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </label>
            <div className="acme-form-grid">
              <label className="acme-field">
                <span className="acme-label">Typ</span>
                <select className="acme-select" value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as CustomerKind })}>
                  {KINDS.map((k) => <option key={k} value={k}>{k}</option>)}
                </select>
              </label>
              <label className="acme-field">
                <span className="acme-label">Status</span>
                <select className="acme-select" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as CustomerStatus })}>
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </label>
            </div>
            <div className="acme-form-actions">
              <button className="acme-btn acme-btn--ghost" onClick={() => setCreating(false)}>Abbrechen</button>
              <button className="acme-btn" disabled={!form.name.trim()} onClick={submit}>Anlegen</button>
            </div>
          </div>
        )}

        {customers.map((c) => (
          <button
            key={c.id}
            className={`acme-md-item${c.id === selected?.id ? ' is-active' : ''}`}
            onClick={() => setSelectedId(c.id)}
          >
            <span className="acme-avatar acme-avatar--sm">{nameInitials(c.name)}</span>
            <div className="acme-md-item-main">
              <div className="acme-md-item-title">{c.name}</div>
              <div className="acme-md-item-sub">{kindLabel(c.kind)}{c.country ? ` · ${c.country}` : ''}</div>
            </div>
            <span className="acme-status-chip" data-status={c.status}>{c.status}</span>
          </button>
        ))}
        {customers.length === 0 && <div className="acme-empty">Keine Kunden.</div>}
      </div>

      <div className="acme-md-detail">
        {!selected ? (
          <div className="acme-empty">Firma auswählen.</div>
        ) : (
          <>
            <div className="acme-detail-head">
              <div>
                <div className="acme-detail-title">{selected.name}</div>
                <div className="acme-detail-sub">{kindLabel(selected.kind)}{selected.country ? ` · ${selected.country}` : ''}</div>
              </div>
              <div className="acme-detail-head-actions">
                <span className="acme-status-chip" data-status={selected.status}>{selected.status}</span>
                {canWrite && !editing && (
                  <button className="acme-btn acme-btn--ghost" onClick={startEdit}>Bearbeiten</button>
                )}
              </div>
            </div>

            {editing && edit ? (
              <div className="acme-form-card">
                <label className="acme-field">
                  <span className="acme-label">Firma</span>
                  <input className="acme-input" value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} />
                </label>
                <div className="acme-form-grid">
                  <label className="acme-field">
                    <span className="acme-label">Typ</span>
                    <select className="acme-select" value={edit.kind} onChange={(e) => setEdit({ ...edit, kind: e.target.value as CustomerKind })}>
                      {KINDS.map((k) => <option key={k} value={k}>{k}</option>)}
                    </select>
                  </label>
                  <label className="acme-field">
                    <span className="acme-label">Status</span>
                    <select className="acme-select" value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value as CustomerStatus })}>
                      {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </label>
                  <label className="acme-field">
                    <span className="acme-label">E-Mail</span>
                    <input className="acme-input" value={edit.email} onChange={(e) => setEdit({ ...edit, email: e.target.value })} />
                  </label>
                  <label className="acme-field">
                    <span className="acme-label">Land</span>
                    <input className="acme-input" value={edit.country} onChange={(e) => setEdit({ ...edit, country: e.target.value })} />
                  </label>
                  <label className="acme-field">
                    <span className="acme-label">Übergeordneter Reseller</span>
                    <select className="acme-select" value={edit.parentResellerId} onChange={(e) => setEdit({ ...edit, parentResellerId: e.target.value })}>
                      <option value="">—</option>
                      {resellers.filter((r) => r.id !== selected.id).map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                    </select>
                  </label>
                </div>
                <div className="acme-form-actions">
                  <button className="acme-btn acme-btn--ghost" onClick={() => setEditing(false)}>Abbrechen</button>
                  <button className="acme-btn" disabled={!edit.name.trim()} onClick={saveEdit}>Speichern</button>
                </div>
              </div>
            ) : (
              <div className="acme-detail-section">
                <div className="acme-detail-fields">
                  <div className="acme-field-ro"><span className="acme-label">Typ</span><span className="acme-field-val">{kindLabel(selected.kind)}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">Status</span><span className="acme-field-val">{selected.status}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">Land</span><span className="acme-field-val">{selected.country ?? '—'}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">E-Mail</span><span className="acme-field-val">{selected.email ?? '—'}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">Preisliste</span><span className="acme-field-val">{selected.priceListId ?? '—'}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">Reseller</span><span className="acme-field-val">{nameOf(selected.parentResellerId) ?? '—'}</span></div>
                  <div className="acme-field-ro"><span className="acme-label">Pipeline</span><span className="acme-field-val">{fmtTotal(pipelineSum)}</span></div>
                </div>
              </div>
            )}

            <div className="acme-detail-section">
              <div className="acme-section-label">Kontakte · {custContacts.length}</div>
              {custContacts.length === 0 ? (
                <div className="acme-empty">Keine Kontakte.</div>
              ) : (
                <div className="acme-contact-mini-list">
                  {custContacts.map((ct) => (
                    <div className="acme-contact-mini" key={ct.id}>
                      <span className="acme-avatar acme-avatar--sm">{nameInitials(ct.name)}</span>
                      <div className="acme-md-item-main">
                        <div className="acme-md-item-title">{ct.name}{ct.primary ? ' ·★' : ''}</div>
                        <div className="acme-md-item-sub">{[ct.role, ct.email].filter(Boolean).join(' · ')}</div>
                      </div>
                      {ct.newsletter && <span className="acme-tag-chip">Newsletter</span>}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="acme-detail-section">
              <div className="acme-section-label">Deals · {custDeals.length}</div>
              <DealMiniList deals={custDeals} showContact />
            </div>

            <div className="acme-detail-section">
              <div className="acme-section-label">Mail-Threads · {custThreads.length}</div>
              <ThreadPanel threads={custThreads} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
