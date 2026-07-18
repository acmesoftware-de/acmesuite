import { useState } from 'react'
import {
  BOM_BY_PRODUCT,
  bomIndent,
  PRODUCTS,
  productMeta,
  ROUTING_BY_PRODUCT,
} from './buildModel'

/**
 * Produkte — master-detail. Left: the product list (selected item gets an accent left edge).
 * Right: header metrics plus two panels — the multi-level Stückliste (BOM, indented per level,
 * Eigen/Zukauf badge, availability dot) and the Arbeitsschritte routing (step · machine · time).
 *
 * Multi-level BOM, Eigen/Zukauf and routing exceed the flat /boms contract, so this is
 * representative demo master data (see buildModel · PRODUCTS_ARE_DEMO).
 */
export function ProdukteView() {
  const [activeId, setActiveId] = useState(PRODUCTS[0].id)
  const active = PRODUCTS.find((p) => p.id === activeId) ?? PRODUCTS[0]
  const bom = BOM_BY_PRODUCT[active.id] ?? []
  const routing = ROUTING_BY_PRODUCT[active.id] ?? []
  const meta = productMeta(active.id)

  return (
    <div className="acme-bld-prod">
      <div className="acme-bld-prod-list">
        <div className="acme-bld-prod-list-label">PRODUKTE · {PRODUCTS.length}</div>
        {PRODUCTS.map((p) => (
          <button
            key={p.id}
            className={`acme-bld-prod-item${p.id === activeId ? ' is-active' : ''}`}
            onClick={() => setActiveId(p.id)}
          >
            <span className="acme-bld-prod-item-head">
              <span className="acme-bld-prod-item-name">{p.name}</span>
              {p.discontinued && <span className="acme-bld-prod-item-badge">AUSLAUF</span>}
            </span>
            <span className="acme-bld-prod-item-sub">
              {p.art} · {p.variant}
            </span>
          </button>
        ))}
      </div>

      <div className="acme-bld-prod-detail">
        <div className="acme-bld-prod-detail-head">
          <span className="acme-bld-prod-name">{active.name}</span>
          <span className="acme-bld-prod-art">
            {active.art} · {active.variant}
          </span>
          <div className="acme-bld-prod-metrics">
            <div className="acme-bld-prod-metric">
              <div className="acme-bld-prod-metric-label">POSITIONEN</div>
              <div className="acme-bld-prod-metric-value">{meta.bomCount}</div>
            </div>
            <div className="acme-bld-prod-metric">
              <div className="acme-bld-prod-metric-label">SCHRITTE</div>
              <div className="acme-bld-prod-metric-value">{meta.stepCount}</div>
            </div>
            <div className="acme-bld-prod-metric">
              <div className="acme-bld-prod-metric-label">DURCHLAUF</div>
              <div className="acme-bld-prod-metric-value is-accent">{meta.totalMinutes} min/Stk</div>
            </div>
          </div>
        </div>

        <div className="acme-bld-prod-panels">
          <div className="acme-bld-bom">
            <div className="acme-bld-panel-label">STÜCKLISTE</div>
            <div className="acme-bld-bom-head">
              <span className="acme-bld-th acme-bld-th--pos">POS</span>
              <span className="acme-bld-th">KOMPONENTE</span>
              <span className="acme-bld-th">MENGE</span>
              <span className="acme-bld-th">TYP</span>
              <span />
            </div>
            {bom.map((b) => (
              <div className="acme-bld-bom-row" key={b.pos}>
                <span className="acme-bld-bom-pos">{b.pos}</span>
                <span
                  className={`acme-bld-bom-name${b.level === 0 ? ' is-top' : ''}`}
                  style={{ paddingLeft: bomIndent(b.level) }}
                >
                  {b.name}
                </span>
                <span className="acme-bld-bom-qty">
                  {b.quantity} {b.unit}
                </span>
                <span>
                  <span className={`acme-bld-kind${b.kind === 'Eigenfertigung' ? ' acme-bld-kind--eigen' : ''}`}>
                    {b.kind === 'Eigenfertigung' ? 'Eigen' : 'Zukauf'}
                  </span>
                </span>
                <span className="acme-bld-bom-avail" data-avail={b.avail} title={availLabel(b.avail)}>
                  <span className="acme-bld-avail-dot" />
                </span>
              </div>
            ))}
          </div>

          <div className="acme-bld-routing">
            <div className="acme-bld-panel-label">ARBEITSSCHRITTE</div>
            <div className="acme-bld-routing-head">
              <span className="acme-bld-th">NR</span>
              <span className="acme-bld-th">ARBEITSGANG</span>
              <span className="acme-bld-th">MASCHINE</span>
              <span className="acme-bld-th acme-bld-th--right">ZEIT</span>
            </div>
            {routing.map((s) => (
              <div className="acme-bld-routing-row" key={s.step}>
                <span className="acme-bld-routing-step">{s.step}</span>
                <span className="acme-bld-routing-op">{s.operation}</span>
                <span>
                  <span className={`acme-bld-machine-chip${s.machine ? '' : ' is-manual'}`}>
                    {s.machine ?? 'manuell'}
                  </span>
                </span>
                <span className="acme-bld-routing-time">{s.minutes} min</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function availLabel(a: 'ok' | 'low' | 'critical'): string {
  return a === 'ok' ? 'Verfügbar' : a === 'low' ? 'Knapp' : 'Fehlt'
}
