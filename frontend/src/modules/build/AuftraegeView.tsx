import { useEffect, useRef, useState } from 'react'
import type { OrderStage, ProductionOrder, ProductionOrderCreate } from './buildApi'
import { fmtInt, ownerAvIndex, PRODUCTS, toOrderColumns } from './buildModel'

/**
 * Aufträge — the production planning board (GEPLANT · RÜSTEN · IN ARBEIT · PRÜFUNG · FERTIG).
 * Cards drag between columns to set their stage (write-gated). The "+ AUFTRAG" button (shell
 * header) bumps `createTick`, which opens the inline create form here.
 */
export function AuftraegeView({
  orders,
  canWrite,
  onMove,
  onCreate,
  createTick,
}: {
  orders: ProductionOrder[]
  canWrite: boolean
  onMove: (id: string, stage: OrderStage) => void
  onCreate: (body: ProductionOrderCreate & { productName: string }) => void
  createTick: number
}) {
  const [dragId, setDragId] = useState<string | null>(null)
  const [overStage, setOverStage] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)

  // The shell's "+ AUFTRAG" button opens the create form. Only react to an *increment* of the
  // tick (not the mount value), so revisiting this sub-view doesn't reopen the form.
  const seenTick = useRef(createTick)
  useEffect(() => {
    if (createTick !== seenTick.current) {
      seenTick.current = createTick
      if (canWrite) setCreating(true)
    }
  }, [createTick, canWrite])

  const columns = toOrderColumns(orders)

  return (
    <div className="acme-bld-orders">
      {creating && (
        <CreateOrderForm
          onClose={() => setCreating(false)}
          onSubmit={(body) => {
            onCreate(body)
            setCreating(false)
          }}
        />
      )}

      <div className="acme-bld-board">
        {columns.map((col) => (
          <div className="acme-bld-col" key={col.stage.id}>
            <div className="acme-bld-col-head">
              <span className="acme-bld-col-dot" data-bstage={col.stage.id} />
              <span className="acme-bld-col-label">{col.stage.label}</span>
              <span className="acme-bld-col-count">{col.count}</span>
            </div>

            <div
              className={`acme-bld-col-body${overStage === col.stage.id ? ' is-over' : ''}`}
              onDragOver={
                canWrite
                  ? (e) => {
                      e.preventDefault()
                      setOverStage(col.stage.id)
                    }
                  : undefined
              }
              onDragLeave={() => setOverStage((s) => (s === col.stage.id ? null : s))}
              onDrop={
                canWrite
                  ? () => {
                      if (dragId) {
                        const moved = orders.find((o) => o.id === dragId)
                        if (moved && moved.stage !== col.stage.id) onMove(dragId, col.stage.id)
                      }
                      setDragId(null)
                      setOverStage(null)
                    }
                  : undefined
              }
            >
              {col.orders.map((o) => (
                <div
                  className={`acme-bld-card${dragId === o.id ? ' is-dragging' : ''}`}
                  key={o.id}
                  draggable={canWrite}
                  onDragStart={() => setDragId(o.id)}
                  onDragEnd={() => {
                    setDragId(null)
                    setOverStage(null)
                  }}
                >
                  <div className="acme-bld-card-no">{o.orderNo}</div>
                  <div className="acme-bld-card-product">{o.productName ?? o.productId ?? '—'}</div>
                  <div className="acme-bld-card-qty">{fmtInt(o.quantity)} Stk</div>
                  <div className="acme-bld-card-foot">
                    {o.machine && <span className="acme-bld-card-machine">{o.machine}</span>}
                    {o.ownerInitials && (
                      <span className={`acme-bld-card-owner acme-bld-av--${ownerAvIndex(o.ownerInitials)}`}>
                        {o.ownerInitials}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function CreateOrderForm({
  onSubmit,
  onClose,
}: {
  onSubmit: (body: ProductionOrderCreate & { productName: string }) => void
  onClose: () => void
}) {
  const [productId, setProductId] = useState(PRODUCTS[0].id)
  const [quantity, setQuantity] = useState('100')
  const [machine, setMachine] = useState('')
  const [owner, setOwner] = useState('')

  const qty = Math.max(1, Math.round(Number(quantity) || 0))
  const product = PRODUCTS.find((p) => p.id === productId) ?? PRODUCTS[0]

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSubmit({
      productId,
      productName: product.name,
      quantity: qty,
      machine: machine.trim() || null,
      ownerInitials: owner.trim().toUpperCase() || null,
      stage: 'GEPLANT',
    })
  }

  return (
    <form className="acme-form-card acme-bld-create" onSubmit={submit}>
      <div className="acme-form-grid">
        <div className="acme-field">
          <label className="acme-label">Produkt</label>
          <select className="acme-select" value={productId} onChange={(e) => setProductId(e.target.value)}>
            {PRODUCTS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} · {p.art}
              </option>
            ))}
          </select>
        </div>
        <div className="acme-field">
          <label className="acme-label">Menge (Stk)</label>
          <input
            className="acme-input"
            type="number"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
          />
        </div>
        <div className="acme-field">
          <label className="acme-label">Maschine (optional)</label>
          <input
            className="acme-input"
            value={machine}
            placeholder="z. B. CNC-01"
            onChange={(e) => setMachine(e.target.value)}
          />
        </div>
        <div className="acme-field">
          <label className="acme-label">Planer-Kürzel (optional)</label>
          <input
            className="acme-input"
            value={owner}
            placeholder="z. B. MW"
            maxLength={3}
            onChange={(e) => setOwner(e.target.value)}
          />
        </div>
      </div>
      <div className="acme-form-actions">
        <button type="button" className="acme-btn acme-btn--ghost" onClick={onClose}>
          Abbrechen
        </button>
        <button type="submit" className="acme-btn">
          Auftrag anlegen
        </button>
      </div>
    </form>
  )
}
