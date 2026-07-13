import { useEffect, useMemo, useRef, useState } from 'react'
import type { Contact, Customer, CustomerKind, CustomerStatus, CustomerWrite, Deal, MailThread } from './crmApi'
import { DealMiniList, ThreadPanel, nameInitials } from './DetailPanels'

const KINDS: CustomerKind[] = ['CUSTOMER', 'RESELLER']
const STATUSES: CustomerStatus[] = ['PROSPECT', 'ACTIVE', 'BLOCKED']

/** Kunden (companies) as master-detail: list left, selected company + its contacts / deals / mail right. */
export function CustomersView({
  customers,
  contacts,
  deals,
  threads,
  canWrite,
  createTick,
  onCreate,
}: {
  customers: Customer[] | null
  contacts: Contact[] | null
  deals: Deal[] | null
  threads: MailThread[] | null
  canWrite: boolean
  createTick: number
  onCreate: (body: CustomerWrite) => Promise<void>
}) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ name: '', kind: 'CUSTOMER' as CustomerKind, status: 'PROSPECT' as CustomerStatus, country: '' })

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
  const custContacts = useMemo(() => contacts?.filter((c) => c.customerId === selId) ?? [], [contacts, selId])
  const custDeals = useMemo(() => deals?.filter((d) => d.customerId === selId) ?? [], [deals, selId])
  const custThreads = useMemo(() => threads?.filter((t) => t.customerId === selId) ?? [], [threads, selId])

  async function submit() {
    await onCreate({ name: form.name.trim(), kind: form.kind, status: form.status, country: form.country.trim() || null })
    setForm({ name: '', kind: 'CUSTOMER', status: 'PROSPECT', country: '' })
    setCreating(false)
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
              <div className="acme-md-item-sub">{c.kind === 'RESELLER' ? 'Reseller' : 'Kunde'}{c.country ? ` · ${c.country}` : ''}</div>
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
                <div className="acme-detail-sub">
                  {selected.kind === 'RESELLER' ? 'Reseller' : 'Kunde'}
                  {selected.country ? ` · ${selected.country}` : ''}
                  {selected.email ? ` · ${selected.email}` : ''}
                </div>
              </div>
              <span className="acme-status-chip" data-status={selected.status}>{selected.status}</span>
            </div>

            <div className="acme-detail-section">
              <div className="acme-section-label">Kontakte</div>
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
