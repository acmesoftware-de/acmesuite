import type { DealActions } from './CrmModule'
import type { Deal } from './crmApi'
import { STAGES, amountOf, fmtFull, ownerInitials, probabilityFor } from './pipelineModel'

/** Ordered pipeline table with inline edit (company + value contentEditable, phase select). */
export function TableView({ deals, actions }: { deals: Deal[]; actions: DealActions }) {
  const { canWrite } = actions
  return (
    <div className="acme-tbl">
      <div className="acme-tbl-head">
        <span />
        <span className="acme-th">FIRMA</span>
        <span className="acme-th">PHASE</span>
        <span className="acme-th">WERT</span>
        <span className="acme-th">WAHRSCHEINL.</span>
        <span className="acme-th">LETZTE AKTIVITÄT</span>
        <span className="acme-th acme-th--right">TEAM</span>
      </div>

      {deals.map((d) => {
        const prob = probabilityFor(d.stage)
        return (
          <div className="acme-tbl-row" key={d.id}>
            <span className="acme-checkbox" />

            <div className="acme-tbl-company">
              <div
                className="acme-editable acme-company-name"
                contentEditable={canWrite}
                suppressContentEditableWarning
                onBlur={(e) => {
                  const next = e.currentTarget.textContent?.trim()
                  if (next && next !== d.company) actions.setCompany(d.id, next)
                }}
              >
                {d.company}
              </div>
              <div className="acme-company-contact">{d.contact}</div>
            </div>

            <div className="acme-phase">
              <select
                className="acme-phase-select"
                value={d.stage}
                disabled={!canWrite}
                onChange={(e) => actions.setStage(d.id, e.target.value as Deal['stage'])}
              >
                {STAGES.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="acme-value">
              <span className="acme-value-cur">€</span>
              <span
                className="acme-editable acme-value-num"
                contentEditable={canWrite}
                suppressContentEditableWarning
                onBlur={(e) => {
                  const n = parseInt(String(e.currentTarget.textContent).replace(/[^0-9]/g, ''), 10)
                  if (!isNaN(n) && n !== amountOf(d)) actions.setValue(d.id, n)
                }}
              >
                {fmtFull(amountOf(d))}
              </span>
            </div>

            <div className="acme-prob">
              <div className="acme-prob-track">
                <div className="acme-prob-fill" style={{ width: prob + '%' }} />
              </div>
              <span className="acme-prob-pct">{prob}%</span>
            </div>

            <div className="acme-activity">
              <span className="acme-stage-dot" data-stage={d.stage} />
              <span className="acme-activity-text">{d.lastActivity ?? '—'}</span>
            </div>

            <div className="acme-team">
              <span className="acme-avatar acme-avatar--deal" data-owner={ownerInitials(d)}>
                {ownerInitials(d)}
              </span>
            </div>
          </div>
        )
      })}
    </div>
  )
}
