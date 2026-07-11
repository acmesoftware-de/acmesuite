import type { Kpi } from '../modules/registry'

interface KpiBarProps {
  kpis: Kpi[]
}

export function KpiBar({ kpis }: KpiBarProps) {
  return (
    <div className="acme-kpis">
      {kpis.map((k) => (
        <div className="acme-kpi" key={k.label}>
          <div className="acme-kpi-label">{k.label}</div>
          <div className="acme-kpi-row">
            <span className="acme-kpi-value">{k.value}</span>
            <span className={`acme-kpi-delta ${k.up ? 'is-up' : 'is-down'}`}>{k.delta}</span>
          </div>
        </div>
      ))}
    </div>
  )
}
