import { useEffect, useMemo, useRef, useState } from 'react'
import type { Contact, ContactWrite, Customer, Deal, MailThread } from './crmApi'
import { DealMiniList, ThreadPanel, nameInitials } from './DetailPanels'

/** Kontakte (people) as master-detail: list left, selected contact + its deals / mail right. */
export function ContactsView({
  contacts,
  customers,
  deals,
  threads,
  canWrite,
  createTick,
  onCreate,
}: {
  contacts: Contact[] | null
  customers: Customer[] | null
  deals: Deal[] | null
  threads: MailThread[] | null
  canWrite: boolean
  createTick: number
  onCreate: (body: ContactWrite) => Promise<void>
}) {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ name: '', role: '', email: '', customerId: '' })

  const seen = useRef(createTick)
  useEffect(() => {
    if (createTick !== seen.current) {
      seen.current = createTick
      if (canWrite) setCreating(true)
    }
  }, [createTick, canWrite])

  const companyName = (id?: string | null) => customers?.find((c) => c.id === id)?.name ?? '—'

  const selected = useMemo(
    () => contacts?.find((c) => c.id === selectedId) ?? contacts?.[0] ?? null,
    [contacts, selectedId],
  )
  const selId = selected?.id
  const contactDeals = useMemo(() => deals?.filter((d) => d.contactId === selId) ?? [], [deals, selId])
  const contactThreads = useMemo(() => threads?.filter((t) => t.contactId === selId) ?? [], [threads, selId])

  async function submit() {
    await onCreate({
      name: form.name.trim(),
      role: form.role.trim() || null,
      email: form.email.trim() || null,
      customerId: form.customerId || null,
    })
    setForm({ name: '', role: '', email: '', customerId: '' })
    setCreating(false)
  }

  if (!contacts) return <div className="acme-empty">Kontakte werden geladen…</div>

  return (
    <div className="acme-md">
      <div className="acme-md-list">
        {creating && (
          <div className="acme-form-card">
            <div className="acme-form-grid">
              <label className="acme-field">
                <span className="acme-label">Name</span>
                <input className="acme-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </label>
              <label className="acme-field">
                <span className="acme-label">Rolle</span>
                <input className="acme-input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} />
              </label>
              <label className="acme-field">
                <span className="acme-label">E-Mail</span>
                <input className="acme-input" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </label>
              <label className="acme-field">
                <span className="acme-label">Firma</span>
                <select className="acme-select" value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })}>
                  <option value="">—</option>
                  {customers?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </label>
            </div>
            <div className="acme-form-actions">
              <button className="acme-btn acme-btn--ghost" onClick={() => setCreating(false)}>Abbrechen</button>
              <button className="acme-btn" disabled={!form.name.trim()} onClick={submit}>Anlegen</button>
            </div>
          </div>
        )}

        {contacts.map((c) => (
          <button
            key={c.id}
            className={`acme-md-item${c.id === selected?.id ? ' is-active' : ''}`}
            onClick={() => setSelectedId(c.id)}
          >
            <span className="acme-avatar acme-avatar--sm">{nameInitials(c.name)}</span>
            <div className="acme-md-item-main">
              <div className="acme-md-item-title">{c.name}</div>
              <div className="acme-md-item-sub">{[c.role, companyName(c.customerId)].filter(Boolean).join(' · ')}</div>
            </div>
          </button>
        ))}
        {contacts.length === 0 && <div className="acme-empty">Keine Kontakte.</div>}
      </div>

      <div className="acme-md-detail">
        {!selected ? (
          <div className="acme-empty">Kontakt auswählen.</div>
        ) : (
          <>
            <div className="acme-detail-head">
              <div>
                <div className="acme-detail-title">{selected.name}</div>
                <div className="acme-detail-sub">
                  {[selected.role, companyName(selected.customerId)].filter(Boolean).join(' · ')}
                  {selected.email ? ` · ${selected.email}` : ''}
                  {selected.phone ? ` · ${selected.phone}` : ''}
                </div>
              </div>
            </div>

            <div className="acme-detail-section">
              <div className="acme-section-label">Deals · {contactDeals.length}</div>
              <DealMiniList deals={contactDeals} />
            </div>

            <div className="acme-detail-section">
              <div className="acme-section-label">Mail-Threads · {contactThreads.length}</div>
              <ThreadPanel threads={contactThreads} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
