import { useEffect, useRef, useState } from 'react'
import type { DealActions } from './CrmModule'
import type { Deal, DealCreate } from './crmApi'
import { TableView } from './TableView'
import { KanbanView } from './KanbanView'
import { FunnelView } from './FunnelView'

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
  actions,
  createTick,
  onCreate,
  mode,
}: {
  deals: Deal[]
  actions: DealActions
  createTick: number
  onCreate: (body: DealCreate) => Promise<void>
  mode: PipelineMode
}) {
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ company: '', contact: '', value: '', owner: 'JS' })

  const seen = useRef(createTick)
  useEffect(() => {
    if (createTick !== seen.current) {
      seen.current = createTick
      if (actions.canWrite) setCreating(true)
    }
  }, [createTick, actions.canWrite])

  async function submit() {
    await onCreate({
      company: form.company.trim(),
      contact: form.contact.trim() || null,
      ownerInitials: form.owner,
      value: form.value ? { amount: Number(form.value.replace(/[^0-9]/g, '')), currency: 'EUR' } : undefined,
    })
    setForm({ company: '', contact: '', value: '', owner: 'JS' })
    setCreating(false)
  }

  return (
    <div className="acme-pipeline">
      {creating && (
        <div className="acme-form-card">
          <div className="acme-form-grid">
            <label className="acme-field">
              <span className="acme-label">Firma</span>
              <input className="acme-input" value={form.company} onChange={(e) => setForm({ ...form, company: e.target.value })} />
            </label>
            <label className="acme-field">
              <span className="acme-label">Kontakt</span>
              <input className="acme-input" value={form.contact} onChange={(e) => setForm({ ...form, contact: e.target.value })} />
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
            <button className="acme-btn" disabled={!form.company.trim()} onClick={submit}>Deal anlegen</button>
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
