import { useEffect, useMemo, useRef, useState } from 'react'
import type { DealActions } from './CrmModule'
import type { Contact, Customer, Deal, DealCreate } from './crmApi'
import { TableView } from './TableView'
import { KanbanView } from './KanbanView'
import { FunnelView } from './FunnelView'

/** Display label for a contact in a select ("Sara Mena · CTO"). */
function contactLabel(c: Contact): string {
  return [c.name, c.role].filter(Boolean).join(' · ')
}

export type PipelineMode = 'tabelle' | 'kanban' | 'funnel'

/** The Tabelle/Kanban/Funnel switch options — rendered in the module header (see App). */
export const PIPELINE_MODES: { key: PipelineMode; label: string }[] = [
  { key: 'tabelle', label: 'TABELLE' },
  { key: 'kanban', label: 'KANBAN' },
  { key: 'funnel', label: 'FUNNEL' },
]

/** Pipeline sub-view over the shared deal list. The view mode is controlled by the shell
 *  header (the Tabelle/Kanban/Funnel switch lives next to the Pipeline tab). */
export function PipelineView({
  deals,
  customers,
  contacts,
  actions,
  createTick,
  onCreate,
  mode,
}: {
  deals: Deal[]
  customers: Customer[]
  contacts: Contact[]
  actions: DealActions
  createTick: number
  onCreate: (body: DealCreate) => Promise<void>
  mode: PipelineMode
}) {
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ customerId: '', contactId: '', value: '', owner: 'JS' })

  const seen = useRef(createTick)
  useEffect(() => {
    if (createTick !== seen.current) {
      seen.current = createTick
      if (actions.canWrite) setCreating(true)
    }
  }, [createTick, actions.canWrite])

  // Contacts of the chosen company drive the dependent Kontakt select.
  const companyContacts = useMemo(
    () => (form.customerId ? contacts.filter((c) => c.customerId === form.customerId) : []),
    [contacts, form.customerId],
  )

  async function submit() {
    const customer = customers.find((c) => c.id === form.customerId)
    const contact = contacts.find((c) => c.id === form.contactId)
    if (!customer) return
    await onCreate({
      company: customer.name,
      customerId: customer.id,
      contact: contact ? contactLabel(contact) : null,
      contactId: contact?.id ?? null,
      ownerInitials: form.owner,
      value: form.value ? { amount: Number(form.value.replace(/[^0-9]/g, '')), currency: 'EUR' } : undefined,
    })
    setForm({ customerId: '', contactId: '', value: '', owner: 'JS' })
    setCreating(false)
  }

  return (
    <div className="acme-pipeline">
      {creating && (
        <div className="acme-form-card">
          <div className="acme-form-grid">
            <label className="acme-field">
              <span className="acme-label">Kunde</span>
              <select
                className="acme-select"
                value={form.customerId}
                onChange={(e) => setForm({ ...form, customerId: e.target.value, contactId: '' })}
              >
                <option value="">— Kunde wählen —</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </label>
            <label className="acme-field">
              <span className="acme-label">Kontakt</span>
              <select
                className="acme-select"
                value={form.contactId}
                disabled={!form.customerId}
                onChange={(e) => setForm({ ...form, contactId: e.target.value })}
              >
                <option value="">{form.customerId ? '— optional —' : 'zuerst Kunde wählen'}</option>
                {companyContacts.map((c) => (
                  <option key={c.id} value={c.id}>{contactLabel(c)}</option>
                ))}
              </select>
            </label>
            <label className="acme-field">
              <span className="acme-label">Wert (€)</span>
              <input className="acme-input" inputMode="numeric" value={form.value} onChange={(e) => setForm({ ...form, value: e.target.value })} />
            </label>
            <label className="acme-field">
              <span className="acme-label">Owner</span>
              <select className="acme-select" value={form.owner} onChange={(e) => setForm({ ...form, owner: e.target.value })}>
                {['JS', 'AL', 'MW'].map((o) => (
                  <option key={o} value={o}>{o}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="acme-form-actions">
            <button className="acme-btn acme-btn--ghost" onClick={() => setCreating(false)}>Abbrechen</button>
            <button className="acme-btn" disabled={!form.customerId} onClick={submit}>Deal anlegen</button>
          </div>
        </div>
      )}

      <div className={`acme-pipeline-body${mode === 'kanban' ? ' acme-pipeline-body--board' : ''}`}>
        {mode === 'kanban' ? (
          <KanbanView deals={deals} actions={actions} />
        ) : mode === 'funnel' ? (
          <FunnelView deals={deals} />
        ) : (
          <TableView deals={deals} actions={actions} />
        )}
      </div>
    </div>
  )
}
