import type { AbsentRow, TeamRow, TeamStatus } from './hrModel'

const STATUS_LABEL: Record<TeamStatus, string> = {
  aktiv: 'Aktiv',
  remote: 'Remote',
  urlaub: 'Urlaub',
  krank: 'Krank',
}

interface TeamViewProps {
  rows: TeamRow[]
  absent: AbsentRow[]
}

/** Team roster (avatar+name · role · team · status) with an "Abwesend heute" rail. */
export function TeamView({ rows, absent }: TeamViewProps) {
  return (
    <div className="acme-hr-team">
      <div className="acme-hr-table">
        <div className="acme-hr-thead">
          <span className="acme-th">MITARBEITER</span>
          <span className="acme-th">ROLLE</span>
          <span className="acme-th">TEAM</span>
          <span className="acme-th">STATUS</span>
        </div>
        {rows.map((r, i) => (
          <div className={`acme-hr-row${i % 2 === 1 ? ' is-zebra' : ''}`} key={r.id}>
            <div className="acme-hr-emp">
              <span className={`acme-hr-av acme-hr-av--${r.avIndex}`}>{r.ini}</span>
              <span className="acme-hr-name">{r.name}</span>
            </div>
            <span className="acme-hr-role">{r.role}</span>
            <span className="acme-hr-team-cell">{r.team}</span>
            <div className="acme-hr-status">
              <span className={`acme-hr-dot acme-hr-dot--${r.status}`} />
              <span className="acme-hr-status-label">{STATUS_LABEL[r.status]}</span>
            </div>
          </div>
        ))}
        {rows.length === 0 && <div className="acme-empty">Keine Mitarbeitenden.</div>}
      </div>

      <aside className="acme-hr-rail">
        <div className="acme-hr-rail-head">
          <span className="acme-hr-rail-dot" />
          <span className="acme-hr-rail-title">ABWESEND HEUTE</span>
        </div>
        <div className="acme-hr-rail-list">
          {absent.map((a) => (
            <div className="acme-hr-absent" key={a.id}>
              <span className={`acme-hr-av acme-hr-av--${a.status === 'urlaub' ? 3 : 0}`}>{a.ini}</span>
              <div className="acme-hr-absent-meta">
                <div className="acme-hr-absent-name">{a.name}</div>
                <div className="acme-hr-absent-role">{a.role}</div>
              </div>
              <span className={`acme-hr-badge acme-hr-badge--${a.status}`}>{STATUS_LABEL[a.status]}</span>
            </div>
          ))}
          {absent.length === 0 && <div className="acme-hr-rail-empty">Heute niemand abwesend.</div>}
        </div>
      </aside>
    </div>
  )
}
