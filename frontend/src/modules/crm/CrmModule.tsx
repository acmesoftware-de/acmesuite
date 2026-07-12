import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { crmApi, type Deal, type DealCreate, type PipelineStage } from './crmApi'
import { probabilityFor } from './pipelineModel'
import { TableView } from './TableView'
import { KanbanView } from './KanbanView'
import { FunnelView } from './FunnelView'

/** Handlers the three sub-views use to mutate the shared pipeline (no-ops for WATCH). */
export interface DealActions {
  canWrite: boolean
  setStage: (id: string, stage: PipelineStage) => void
  setCompany: (id: string, company: string) => void
  setValue: (id: string, amount: number) => void
}

/**
 * CRM module content — the sales Pipeline over /crm/pipeline (see api/acme-crm.yaml).
 * Owns the deal list and optimistic writes; the shell drives which sub-view shows.
 * `newDealTick` increments when the shell's "+ DEAL" button is pressed.
 */
export function CrmModule({ subView, newDealTick }: { subView: string; newDealTick: number }) {
  const { canWrite } = useAuth()
  const [deals, setDeals] = useState<Deal[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<{ company: string; contact: string; value: string; owner: string }>({
    company: '',
    contact: '',
    value: '',
    owner: 'JS',
  })

  useEffect(() => {
    crmApi
      .listPipeline()
      .then(setDeals)
      .catch(() => setError('Konnte Pipeline nicht laden.'))
  }, [])

  // Shell "+ DEAL" opens the create form (write-gated at the shell already).
  useEffect(() => {
    if (newDealTick > 0) setCreating(true)
  }, [newDealTick])

  /** Optimistically apply `local`, then persist `body`; roll back + notify on failure. */
  const persist = useCallback(
    async (id: string, body: Parameters<typeof crmApi.updateDeal>[1], local: Partial<Deal>) => {
      setNotice(null)
      let snapshot: Deal[] | null = null
      setDeals((prev) => {
        snapshot = prev
        return prev?.map((d) => (d.id === id ? { ...d, ...local } : d)) ?? prev
      })
      try {
        const updated = await crmApi.updateDeal(id, body)
        setDeals((prev) => prev?.map((d) => (d.id === id ? updated : d)) ?? prev)
      } catch {
        setDeals(snapshot)
        setNotice('Änderung nicht möglich – zurückgesetzt.')
      }
    },
    [],
  )

  const actions: DealActions = {
    canWrite,
    setStage: (id, stage) => persist(id, { stage }, { stage, probability: probabilityFor(stage) }),
    setCompany: (id, company) => persist(id, { company }, { company }),
    setValue: (id, amount) =>
      persist(id, { value: { amount, currency: 'EUR' } }, { value: { amount, currency: 'EUR' } }),
  }

  async function submitCreate() {
    const body: DealCreate = {
      company: form.company.trim(),
      contact: form.contact.trim() || null,
      ownerInitials: form.owner,
      value: form.value ? { amount: Number(form.value.replace(/[^0-9]/g, '')), currency: 'EUR' } : undefined,
    }
    try {
      const created = await crmApi.createDeal(body)
      setDeals((prev) => [created, ...(prev ?? [])])
      setForm({ company: '', contact: '', value: '', owner: 'JS' })
      setCreating(false)
    } catch {
      setNotice('Deal konnte nicht angelegt werden.')
    }
  }

  return (
    <div className="acme-content">
      {creating && canWrite && (
        <div className="acme-form-card">
          <div className="acme-form-grid">
            <label className="acme-field">
              <span className="acme-label">Firma</span>
              <input
                className="acme-input"
                value={form.company}
                onChange={(e) => setForm({ ...form, company: e.target.value })}
              />
            </label>
            <label className="acme-field">
              <span className="acme-label">Kontakt</span>
              <input
                className="acme-input"
                value={form.contact}
                onChange={(e) => setForm({ ...form, contact: e.target.value })}
              />
            </label>
            <label className="acme-field">
              <span className="acme-label">Wert (€)</span>
              <input
                className="acme-input"
                inputMode="numeric"
                value={form.value}
                onChange={(e) => setForm({ ...form, value: e.target.value })}
              />
            </label>
            <label className="acme-field">
              <span className="acme-label">Owner</span>
              <select
                className="acme-select"
                value={form.owner}
                onChange={(e) => setForm({ ...form, owner: e.target.value })}
              >
                {['JS', 'AL', 'MW'].map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="acme-form-actions">
            <button className="acme-btn acme-btn--ghost" onClick={() => setCreating(false)}>
              Abbrechen
            </button>
            <button className="acme-btn" disabled={!form.company.trim()} onClick={submitCreate}>
              Deal anlegen
            </button>
          </div>
        </div>
      )}

      {notice && <div className="acme-notice">{notice}</div>}
      {error && <div className="acme-error">{error}</div>}

      {!deals && !error && <div className="acme-empty">Pipeline wird geladen…</div>}

      {deals &&
        (subView === 'kanban' ? (
          <KanbanView deals={deals} actions={actions} />
        ) : subView === 'funnel' ? (
          <FunnelView deals={deals} />
        ) : (
          <TableView deals={deals} actions={actions} />
        ))}
    </div>
  )
}
