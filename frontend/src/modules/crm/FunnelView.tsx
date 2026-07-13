import type { Deal } from './crmApi'
import { amountOf, funnelRows, fmtK, fmtTotal } from './pipelineModel'

/** Funnel: a bar per stage (width ∝ deal count), share % + conversion, and right-column totals. */
export function FunnelView({ deals }: { deals: Deal[] }) {
  const rows = funnelRows(deals)
  const totalValue = deals.reduce((acc, d) => acc + amountOf(d), 0)
  const won = deals.filter((d) => d.stage === 'GEWONNEN').length
  const closeRate = deals.length ? Math.round((won / deals.length) * 100) + '%' : '—'

  return (
    <div className="acme-fun">
      <div className="acme-fun-rows">
        {rows.map((row) => (
          <div className="acme-fun-row" key={row.stage.id}>
            <div className="acme-fun-side">
              <div className="acme-fun-label">{row.stage.label}</div>
              <div className="acme-fun-sum">{fmtK(row.sum)}</div>
            </div>
            <div className="acme-fun-track">
              <div className="acme-fun-bar" data-stage={row.stage.id} style={{ width: row.widthPct + '%' }}>
                <span className="acme-fun-count">{row.count}</span>
                <span className="acme-fun-unit">DEALS</span>
              </div>
            </div>
            <div className="acme-fun-conv">
              <div className="acme-fun-pct">{row.sharePct}</div>
              <div className="acme-fun-conv-sub">Konv. {row.conversion}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="acme-fun-totals">
        <div>
          <div className="acme-th">GESAMT-PIPELINE</div>
          <div className="acme-fun-total-value">{fmtTotal(totalValue)}</div>
        </div>
        <div>
          <div className="acme-th">LEAD → ABSCHLUSS</div>
          <div className="acme-fun-total-value">{closeRate}</div>
        </div>
      </div>
    </div>
  )
}
