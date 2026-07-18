import { useState } from 'react'
import { computeFeasibility, FEAS_ORDER, fmtInt, LEVER_DEFS, NO_LEVERS, type Levers } from './buildModel'

/**
 * Machbarkeit — the feasibility calculator. Seeds required/available labor hours from the
 * feasibility + capacity endpoints (`baseReqHours` / `baseDailyCap`), then recomputes LIVE as
 * the planner flips the three levers (client-side what-if). `canWrite` gates the levers.
 */
export function MachbarkeitView({
  baseReqHours,
  baseDailyCap,
  canWrite,
}: {
  baseReqHours: number
  baseDailyCap: number
  canWrite: boolean
}) {
  const [levers, setLevers] = useState<Levers>(NO_LEVERS)
  const feas = computeFeasibility(levers, baseReqHours, baseDailyCap)

  const toggle = (key: keyof Levers) => setLevers((l) => ({ ...l, [key]: !l[key] }))

  return (
    <div className="acme-bld-feas">
      <div className="acme-bld-feas-main">
        <div className="acme-bld-banner" data-feasible={feas.feasible}>
          <span className="acme-bld-banner-icon">{feas.feasible ? '✓' : '✕'}</span>
          <div className="acme-bld-banner-lead">
            <div className="acme-bld-banner-order">
              AUFTRAG {FEAS_ORDER.orderNo} · {FEAS_ORDER.productName} · {fmtInt(FEAS_ORDER.quantity)} Stk
            </div>
            <div className="acme-bld-banner-title">{feas.banner}</div>
            <div className="acme-bld-banner-sub">
              {feas.sub} · Wunschtermin {FEAS_ORDER.wunschDate}
            </div>
          </div>
          <div className="acme-bld-banner-metrics">
            <div className="acme-bld-metric">
              <div className="acme-bld-metric-label">BENÖTIGT</div>
              <div className="acme-bld-metric-value">{feas.reqHours} h</div>
            </div>
            <div className="acme-bld-metric">
              <div className="acme-bld-metric-label">VERFÜGBAR</div>
              <div className="acme-bld-metric-value">{feas.availHours} h</div>
            </div>
            <div className="acme-bld-metric">
              <div className="acme-bld-metric-label">AUSLASTUNG</div>
              <div className="acme-bld-metric-value is-sig">{feas.util} %</div>
            </div>
          </div>
        </div>

        <div className="acme-bld-engpass-label">ENGPASS-ANALYSE · AUSLASTUNG JE RESSOURCE</div>
        {feas.resources.map((r) => (
          <div className="acme-bld-res" key={r.name} data-level={r.level}>
            <span className="acme-bld-res-name">{r.name}</span>
            <div className="acme-bld-res-track">
              <div className="acme-bld-res-fill" style={{ width: `${Math.min(100, r.load)}%` }} />
            </div>
            <span className="acme-bld-res-load">{r.load} %</span>
            <span className="acme-bld-res-cap">
              {r.reqHours} h / {r.capHours} h
            </span>
          </div>
        ))}
      </div>

      <div className="acme-bld-rail">
        <div className="acme-bld-rail-head">
          <span className="acme-bld-rail-mark">✦</span>
          <span className="acme-bld-rail-title">STELLHEBEL</span>
        </div>
        {LEVER_DEFS.map((def) => (
          <button
            key={def.key}
            className={`acme-bld-lever${levers[def.key] ? ' is-on' : ''}`}
            onClick={() => toggle(def.key)}
            disabled={!canWrite}
            aria-pressed={levers[def.key]}
          >
            <span className="acme-bld-lever-main">
              <span className="acme-bld-lever-label">{def.label}</span>
              <span className="acme-bld-lever-desc">{def.desc}</span>
            </span>
            <span className="acme-bld-toggle" />
          </button>
        ))}
        <div className="acme-bld-rail-hint">
          <span className="acme-bld-rail-hint-mark">✦</span>
          <span className="acme-bld-rail-hint-text">
            Schalter kombinieren, bis der Auftrag zum Wunschtermin machbar ist.
          </span>
        </div>
      </div>
    </div>
  )
}
