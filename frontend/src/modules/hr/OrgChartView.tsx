import type { OrgChart } from './hrModel'

interface OrgChartViewProps {
  chart: OrgChart
}

/** Org chart derived from the reporting line: CEO on top, area-lead cards with their reports. */
export function OrgChartView({ chart }: OrgChartViewProps) {
  if (!chart.ceo) {
    return <div className="acme-empty">Keine Organisationsdaten.</div>
  }
  return (
    <div className="acme-hr-org">
      <div className="acme-hr-org-tree">
        <div className="acme-hr-ceo">
          <span className="acme-hr-ceo-av">{chart.ceo.ini}</span>
          <div>
            <div className="acme-hr-ceo-name">{chart.ceo.name}</div>
            <div className="acme-hr-ceo-role">{chart.ceo.role}</div>
          </div>
        </div>

        <div className="acme-hr-connector" />

        <div className="acme-hr-areas">
          {chart.areas.map((a) => (
            <div className="acme-hr-area" key={a.id}>
              <div className={`acme-hr-area-card acme-hr-area--${a.colorIndex}`}>
                <div className="acme-hr-area-head">
                  <span className="acme-hr-area-av">{a.ini}</span>
                  <div>
                    <div className="acme-hr-area-name">{a.name}</div>
                    <div className="acme-hr-area-role">{a.role}</div>
                  </div>
                </div>
                <div className="acme-hr-area-team">{a.teamLabel}</div>
              </div>
              <div className="acme-hr-area-reports">
                {a.reports.map((d, i) => (
                  <div className="acme-hr-area-report" key={i}>
                    {d}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
