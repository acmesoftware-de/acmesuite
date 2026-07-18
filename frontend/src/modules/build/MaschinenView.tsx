import type { Machine } from './buildApi'
import { machineDimmed, machineIsLive, machineStatusLabel, oeeLevel, oeeText } from './buildModel'

/**
 * Maschinen — the digital-twin monitor (bonus view). One tile per machine: status color on the
 * top edge, live status with a pulsing dot for running machines, the big OEE number (colored by
 * threshold), the running order, three mini-bars (availability/performance/quality) and a
 * progress bar. Idle/maintenance machines are dimmed.
 */
export function MaschinenView({ machines }: { machines: Machine[] }) {
  return (
    <div className="acme-bld-machines">
      <div className="acme-bld-machine-grid">
        {machines.map((m) => (
          <div
            key={m.id}
            className={`acme-bld-tile${machineDimmed(m) ? ' is-dimmed' : ''}`}
            data-mstatus={m.status}
          >
            <div className="acme-bld-tile-head">
              <span className="acme-bld-tile-name">{m.name}</span>
              <span className="acme-bld-tile-status">
                <span className={`acme-bld-tile-dot${machineIsLive(m.status) ? ' is-live' : ''}`} />
                <span className="acme-bld-tile-status-label">{machineStatusLabel(m.status)}</span>
              </span>
            </div>

            <div className="acme-bld-tile-oee" data-oee={oeeLevel(m.oee)}>
              <span className="acme-bld-tile-oee-value">{oeeText(m.oee)}</span>
              <span className="acme-bld-tile-oee-unit">OEE</span>
            </div>

            <div className="acme-bld-tile-order">{m.currentOrder ?? '—'}</div>

            <div className="acme-bld-tile-bars">
              <MiniBar label="VERF" value={m.availability} />
              <MiniBar label="LEIST" value={m.performance} />
              <MiniBar label="QUAL" value={m.quality} />
            </div>

            <div>
              <div className="acme-bld-tile-progress-head">
                <span className="acme-bld-tile-progress-label">FORTSCHRITT</span>
                <span className="acme-bld-tile-progress-pct">{m.progress}%</span>
              </div>
              <div className="acme-bld-tile-progress-track">
                <div className="acme-bld-tile-progress-fill" style={{ width: `${m.progress}%` }} />
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function MiniBar({ label, value }: { label: string; value: number }) {
  return (
    <div className="acme-bld-tile-bar">
      <div className="acme-bld-tile-bar-label">{label}</div>
      <div className="acme-bld-tile-bar-track">
        <div className="acme-bld-tile-bar-fill" style={{ width: `${value}%` }} />
      </div>
    </div>
  )
}
