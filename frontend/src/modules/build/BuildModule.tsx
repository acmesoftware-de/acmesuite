import { useCallback, useEffect, useRef, useState } from 'react'
import {
  buildApi,
  type Machine,
  type OrderStage,
  type ProductionOrder,
  type ProductionOrderCreate,
  type ShiftPlan,
} from './buildApi'
import {
  BASE_DAILY_CAP_FALLBACK,
  BASE_REQ_HOURS_FALLBACK,
  DEFAULT_SHIFT_PLAN,
  FALLBACK_MACHINES,
  FALLBACK_ORDERS,
  FEAS_ORDER,
  dailyCapFrom,
  nextCell,
  normalizeShiftPlan,
  reqHoursFrom,
} from './buildModel'
import { MachbarkeitView } from './MachbarkeitView'
import { SchichtenView } from './SchichtenView'
import { ProdukteView } from './ProdukteView'
import { AuftraegeView } from './AuftraegeView'
import { MaschinenView } from './MaschinenView'

interface BuildModuleProps {
  /** Active sub-view key from the shell. */
  subView: string
  /** WORK/ADMIN may perform writes (order moves, shift edits, levers, create). */
  canWrite: boolean
  /** Bumped when the shell's "+ AUFTRAG" button is pressed. */
  newOrderTick: number
}

/**
 * ACMEbuild "Fertigung" module (design module 03 = Produktion). Loads the shop-floor data from
 * the ACMEbuild contract and renders the sub-view the shell selected. Products/BOM/routing are
 * representative master data (the flat /boms contract lacks the structure); production orders,
 * shift plan and machines come from the v0.2.0 endpoints and fall back to demo data (with a
 * note) when an older backend 404s them — mirroring the HR module's discipline.
 */
export function BuildModule({ subView, canWrite, newOrderTick }: BuildModuleProps) {
  const [ready, setReady] = useState(false)
  const [baseReqHours, setBaseReqHours] = useState(BASE_REQ_HOURS_FALLBACK)
  const [baseDailyCap, setBaseDailyCap] = useState(BASE_DAILY_CAP_FALLBACK)

  const [plan, setPlan] = useState<ShiftPlan>(DEFAULT_SHIFT_PLAN)
  const [planPersistent, setPlanPersistent] = useState(false)

  const [orders, setOrders] = useState<ProductionOrder[]>([])
  const [ordersPersistent, setOrdersPersistent] = useState(false)
  const [ordersFallback, setOrdersFallback] = useState(false)

  const [machines, setMachines] = useState<Machine[]>([])
  const [machinesFallback, setMachinesFallback] = useState(false)

  const [notice, setNotice] = useState<string | null>(null)
  const localSeq = useRef(0)

  useEffect(() => {
    let alive = true
    Promise.allSettled([
      buildApi.getCapacity(),
      buildApi.checkFeasibility({ items: [{ productId: FEAS_ORDER.productId, quantity: FEAS_ORDER.quantity }] }),
      buildApi.getShiftPlan(),
      buildApi.listProductionOrders(),
      buildApi.listMachines(),
    ]).then(([cap, feas, sp, ord, mac]) => {
      if (!alive) return
      setBaseReqHours(reqHoursFrom(feas.status === 'fulfilled' ? feas.value : null))
      setBaseDailyCap(dailyCapFrom(cap.status === 'fulfilled' ? cap.value : null))

      setPlan(normalizeShiftPlan(sp.status === 'fulfilled' ? sp.value : null))
      setPlanPersistent(sp.status === 'fulfilled')

      const ov = ord.status === 'fulfilled' ? ord.value : []
      if (ov.length > 0) {
        setOrders(ov)
        setOrdersPersistent(true)
      } else {
        setOrders(FALLBACK_ORDERS)
        setOrdersFallback(true)
      }

      const mv = mac.status === 'fulfilled' ? mac.value : []
      if (mv.length > 0) setMachines(mv)
      else {
        setMachines(FALLBACK_MACHINES)
        setMachinesFallback(true)
      }

      setReady(true)
    })
    return () => {
      alive = false
    }
  }, [])

  /** Cell click cycles Frei → Voll → Teil; persists the whole plan when it is API-backed. */
  const cycleShift = useCallback(
    (r: number, c: number) => {
      setPlan((prev) => {
        const rows = prev.rows.map((row, i) =>
          i === r ? { ...row, cells: row.cells.map((cell, j) => (j === c ? nextCell(cell) : cell)) } : row,
        )
        const next: ShiftPlan = { ...prev, rows }
        if (planPersistent && canWrite) buildApi.putShiftPlan({ rows: next.rows }).catch(() => undefined)
        return next
      })
    },
    [planPersistent, canWrite],
  )

  /** Move an order between board stages. Optimistic; rolls back on a failed persist. */
  const moveOrder = useCallback(
    (id: string, stage: OrderStage) => {
      setNotice(null)
      let snapshot: ProductionOrder[] | null = null
      setOrders((prev) => {
        snapshot = prev
        return prev.map((o) => (o.id === id ? { ...o, stage } : o))
      })
      if (ordersPersistent) {
        buildApi.updateProductionOrder(id, { stage }).catch(() => {
          if (snapshot) setOrders(snapshot)
          setNotice('Verschieben nicht möglich – zurückgesetzt.')
        })
      }
    },
    [ordersPersistent],
  )

  /** Create a production order (from the "+ AUFTRAG" form). Persists when API-backed. */
  const createOrder = useCallback(
    (body: ProductionOrderCreate & { productName: string }) => {
      setNotice(null)
      if (ordersPersistent) {
        buildApi
          .createProductionOrder(body)
          .then((created) => setOrders((prev) => [created, ...prev]))
          .catch(() => setNotice('Anlegen nicht möglich.'))
        return
      }
      const seq = ++localSeq.current
      const draft: ProductionOrder = {
        id: `local-${seq}`,
        orderNo: `FA-${1050 + seq}`,
        productId: body.productId,
        productName: body.productName,
        quantity: body.quantity,
        machine: body.machine ?? null,
        ownerInitials: body.ownerInitials ?? null,
        stage: body.stage ?? 'GEPLANT',
      }
      setOrders((prev) => [draft, ...prev])
    },
    [ordersPersistent],
  )

  if (!ready) {
    return (
      <div className="acme-bld-module">
        <div className="acme-bld-loading">Lädt…</div>
      </div>
    )
  }

  let content: JSX.Element
  if (subView === 'schichten') {
    content = <SchichtenView plan={plan} canWrite={canWrite} onCycle={cycleShift} />
  } else if (subView === 'produkte') {
    content = (
      <>
        <div className="acme-bld-demo-note">
          <b>Demo-Stammdaten:</b> mehrstufige Stückliste, Arbeitspläne und Eigen/Zukauf gehen über
          den heutigen <code className="acme-code">/boms</code>-Kontrakt hinaus.
        </div>
        <ProdukteView />
      </>
    )
  } else if (subView === 'auftraege') {
    content = (
      <>
        {ordersFallback && (
          <div className="acme-bld-demo-note">
            <b>Demo-Auftragstafel:</b> <code className="acme-code">/production-orders</code> ist im
            aktuellen Backend noch nicht verfügbar (Kontrakt v0.2.0).
          </div>
        )}
        <AuftraegeView
          orders={orders}
          canWrite={canWrite}
          onMove={moveOrder}
          onCreate={createOrder}
          createTick={newOrderTick}
        />
      </>
    )
  } else if (subView === 'maschinen') {
    content = (
      <>
        {machinesFallback && (
          <div className="acme-bld-demo-note">
            <b>Demo-Maschinenmonitor:</b> <code className="acme-code">/machines</code> ist im
            aktuellen Backend noch nicht verfügbar (Kontrakt v0.2.0).
          </div>
        )}
        <MaschinenView machines={machines} />
      </>
    )
  } else {
    content = <MachbarkeitView baseReqHours={baseReqHours} baseDailyCap={baseDailyCap} canWrite={canWrite} />
  }

  return (
    <div className="acme-bld-module">
      {notice && <div className="acme-notice">{notice}</div>}
      {content}
    </div>
  )
}
