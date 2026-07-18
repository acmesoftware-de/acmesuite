import { cellLabel, fmtCap, SHIFT_DAYS, shiftCapacity } from './buildModel'
import type { ShiftPlan } from './buildApi'

/**
 * Schichten — the editable weekly shift matrix (3 shifts × 6 days Mon–Sat). Clicking a cell
 * cycles Frei → Voll → Teil; the weekly capacity (Σ full=1 / partial=0.5) updates live.
 * Editing is gated by `canWrite`.
 */
export function SchichtenView({
  plan,
  canWrite,
  onCycle,
}: {
  plan: ShiftPlan
  canWrite: boolean
  onCycle: (row: number, col: number) => void
}) {
  const capacity = shiftCapacity(plan.rows)

  return (
    <div className="acme-bld-shifts">
      <div className="acme-bld-shifts-head">
        <span className="acme-bld-shifts-hint">
          {plan.week ?? 'KW 29'} · Klick auf Zelle: Frei → Voll → Teil
        </span>
        <div className="acme-bld-cap">
          <span className="acme-bld-cap-label">KAPAZITÄT</span>
          <span className="acme-bld-cap-value">{fmtCap(capacity)}</span>
          <span className="acme-bld-cap-unit">Schichten / Woche</span>
        </div>
      </div>

      <div className="acme-bld-matrix">
        <div className="acme-bld-matrix-days">
          <div className="acme-bld-matrix-rowlabel" />
          {SHIFT_DAYS.map((d) => (
            <div className="acme-bld-matrix-day" key={d}>
              {d}
            </div>
          ))}
        </div>

        {plan.rows.map((row, r) => (
          <div className="acme-bld-matrix-row" key={row.shift}>
            <div className="acme-bld-shift-meta">
              <div className="acme-bld-shift-name">{row.label}</div>
              <div className="acme-bld-shift-time">{row.time} Uhr</div>
            </div>
            {row.cells.map((cell, c) => (
              <button
                key={c}
                className="acme-bld-cell"
                data-cell={cell}
                onClick={() => onCycle(r, c)}
                disabled={!canWrite}
                title={canWrite ? 'Klick: Frei → Voll → Teil' : undefined}
              >
                {cellLabel(cell)}
              </button>
            ))}
          </div>
        ))}
      </div>

      <div className="acme-bld-legend">
        <div className="acme-bld-legend-item">
          <span className="acme-bld-legend-swatch" data-cell="FULL" />
          <span className="acme-bld-legend-text">Voll besetzt</span>
        </div>
        <div className="acme-bld-legend-item">
          <span className="acme-bld-legend-swatch" data-cell="PARTIAL" />
          <span className="acme-bld-legend-text">Teilbesetzt</span>
        </div>
        <div className="acme-bld-legend-item">
          <span className="acme-bld-legend-swatch" data-cell="FREE" />
          <span className="acme-bld-legend-text">Frei</span>
        </div>
      </div>
    </div>
  )
}
