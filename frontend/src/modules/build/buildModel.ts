// Derivations for the ACMEbuild "Fertigung" module. The REST contract (acme-build.yaml) is
// the system of record for products/BOM, capacity and feasibility; the design's presentational
// shapes (the live feasibility calculator, the shift matrix, the planning board, the machine
// monitor) are derived here. Where the contract genuinely lacks a field the design needs, it
// is provided deterministically and flagged MOCK / demo — never silently invented.
//
// The five sub-views split cleanly by data source:
//   • Machbarkeit — seeds the required/available labor hours from /feasibility + /capacity,
//     then recomputes LIVE from the three levers (client-side what-if; the contract has no
//     lever parameters). Per-station shares are a planning-model constant (flagged MOCK).
//   • Schichten   — /shift-plan (falls back to the design's default week).
//   • Produkte    — multi-level BOM, routing and Eigen/Zukauf exceed the flat /boms contract,
//     so the master data here is representative demo data (flagged).
//   • Aufträge    — /production-orders (falls back to a representative board).
//   • Maschinen   — /machines (falls back to a representative monitor; bonus view).

import type {
  Capacity,
  FeasibilityResult,
  Machine,
  MachineStatus,
  OrderStage,
  ProductionOrder,
  ShiftCell,
  ShiftPlan,
  ShiftRow,
} from './buildApi'

// ── shared helpers ──────────────────────────────────────────────────────────
/** de-DE integer with thousands separators, e.g. 1000 → "1.000". */
export function fmtInt(n: number): string {
  return Math.round(n).toLocaleString('de-DE')
}

// ═════════════════════════════════════════════════════════════════════════════
// Machbarkeit — live feasibility calculator
// ═════════════════════════════════════════════════════════════════════════════

export interface Levers {
  /** Second shift → doubles daily capacity. */
  secondShift: boolean
  /** Saturday shift → +1 production day in the window. */
  saturday: boolean
  /** Part outsourcing → −35 % in-house effort. */
  outsource: boolean
}

export const NO_LEVERS: Levers = { secondShift: false, saturday: false, outsource: false }

export interface LeverDef {
  key: keyof Levers
  label: string
  desc: string
}

export const LEVER_DEFS: LeverDef[] = [
  { key: 'secondShift', label: 'Zweite Schicht', desc: '+56 h/Tag Kapazität' },
  { key: 'saturday', label: 'Samstagsschicht', desc: '+1 Produktionstag' },
  { key: 'outsource', label: 'Teil-Fremdvergabe', desc: '−35 % Eigenfertigung' },
]

/** The order whose feasibility the planner checks (design: FA-1050 · Rahmen XL · 800 Stk). */
export const FEAS_ORDER = {
  orderNo: 'FA-1050',
  productId: 'p1',
  productName: 'Rahmen XL',
  quantity: 800,
  wunschAT: 6,
  wunschDate: 'Fr, 17.07.',
} as const

/** Baselines used when the live endpoints are unavailable (design prototype constants). */
export const BASE_REQ_HOURS_FALLBACK = 384
export const BASE_DAILY_CAP_FALLBACK = 56
const BASE_WINDOW_DAYS = 6

/**
 * Per-station split of the order's effort and of available capacity. Feasibility only returns
 * WORKER/MATERIAL scarcity, not a station breakdown, so these shares are a planning-model
 * constant (MOCK) — the Engpass bars visualise where the load concentrates (design default:
 * CNC-Fräsen is the bottleneck at 159 %).
 */
const STATIONS: { name: string; share: number; capShare: number }[] = [
  { name: 'Zuschnitt · Laser-01', share: 0.18, capShare: 0.25 },
  { name: 'CNC-Fräsen · CNC-01/03', share: 0.42, capShare: 0.3 },
  { name: 'Schweißen · Schweiß-01', share: 0.25, capShare: 0.25 },
  { name: 'Montage & Lack', share: 0.15, capShare: 0.2 },
]

export type LoadLevel = 'ok' | 'warn' | 'over'

export interface ResourceLoad {
  name: string
  reqHours: number
  capHours: number
  load: number // percent
  level: LoadLevel
}

export interface Feasibility {
  feasible: boolean
  reqHours: number
  availHours: number
  shortfallHours: number
  util: number // percent
  neededDays: number
  resources: ResourceLoad[]
  banner: string
  sub: string
}

function loadLevel(load: number): LoadLevel {
  return load > 100 ? 'over' : load > 85 ? 'warn' : 'ok'
}

/**
 * Live feasibility recompute. `baseReqHours` is seeded from the feasibility API
 * (requiredLaborUnits) and `baseDailyCap` from /capacity (factoryLaborUnitsPerDay); both fall
 * back to the design constants when the endpoints are unavailable. The lever math is the
 * design's model: reqH = base × (outsource ? .65 : 1); dailyCap = base × (2nd shift ? 2 : 1);
 * window = 6 + (saturday ? 1 : 0); availH = dailyCap × window; feasible ⇔ availH ≥ reqH.
 */
export function computeFeasibility(
  levers: Levers,
  baseReqHours = BASE_REQ_HOURS_FALLBACK,
  baseDailyCap = BASE_DAILY_CAP_FALLBACK,
): Feasibility {
  const reqHours = Math.round(baseReqHours * (levers.outsource ? 0.65 : 1))
  const dailyCap = Math.round(baseDailyCap * (levers.secondShift ? 2 : 1))
  const windowDays = BASE_WINDOW_DAYS + (levers.saturday ? 1 : 0)
  const availHours = dailyCap * windowDays
  const feasible = availHours >= reqHours
  const shortfallHours = Math.max(0, reqHours - availHours)
  const util = availHours > 0 ? Math.round((reqHours / availHours) * 100) : 0
  const neededDays = dailyCap > 0 ? Math.ceil(reqHours / dailyCap) : 0

  const resources: ResourceLoad[] = STATIONS.map((s) => {
    const reqH = Math.round(s.share * reqHours)
    const capH = Math.round(s.capShare * availHours)
    const load = capH > 0 ? Math.round((reqH / capH) * 100) : 0
    return { name: s.name, reqHours: reqH, capHours: capH, load, level: loadLevel(load) }
  })

  return {
    feasible,
    reqHours,
    availHours,
    shortfallHours,
    util,
    neededDays,
    resources,
    banner: feasible ? 'MACHBAR ZUM WUNSCHTERMIN' : 'NICHT MACHBAR ZUM WUNSCHTERMIN',
    sub: feasible
      ? `Prognose ${neededDays} Arbeitstage · Wunsch ${FEAS_ORDER.wunschAT} AT`
      : `${shortfallHours} h Kapazität fehlen im Zeitfenster`,
  }
}

/** Seed the required-hours baseline from a feasibility result, if it carries labor units. */
export function reqHoursFrom(result: FeasibilityResult | null): number {
  const v = result?.requiredLaborUnits
  return typeof v === 'number' && v > 0 ? Math.round(v) : BASE_REQ_HOURS_FALLBACK
}

/** Seed the daily-capacity baseline from /capacity, if available. */
export function dailyCapFrom(capacity: Capacity | null): number {
  const v = capacity?.factoryLaborUnitsPerDay
  return typeof v === 'number' && v > 0 ? Math.round(v) : BASE_DAILY_CAP_FALLBACK
}

// ═════════════════════════════════════════════════════════════════════════════
// Schichten — weekly shift matrix
// ═════════════════════════════════════════════════════════════════════════════

export const SHIFT_DAYS = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa']

interface ShiftMeta {
  shift: ShiftRow['shift']
  label: string
  time: string
}
const SHIFT_META: ShiftMeta[] = [
  { shift: 'EARLY', label: 'Frühschicht', time: '06–14' },
  { shift: 'LATE', label: 'Spätschicht', time: '14–22' },
  { shift: 'NIGHT', label: 'Nachtschicht', time: '22–06' },
]

/** Design default week (used when /shift-plan is unavailable). */
const DEFAULT_CELLS: ShiftCell[][] = [
  ['FULL', 'FULL', 'FULL', 'FULL', 'FULL', 'FREE'],
  ['FULL', 'FULL', 'FULL', 'FULL', 'PARTIAL', 'FREE'],
  ['FREE', 'FREE', 'FULL', 'FULL', 'FREE', 'FREE'],
]

export const DEFAULT_SHIFT_PLAN: ShiftPlan = {
  week: 'KW 29',
  rows: SHIFT_META.map((m, r) => ({ shift: m.shift, label: m.label, time: m.time, cells: DEFAULT_CELLS[r] })),
}

/** Cell-click cycle: Frei → Voll → Teil → Frei. */
export function nextCell(c: ShiftCell): ShiftCell {
  return c === 'FREE' ? 'FULL' : c === 'FULL' ? 'PARTIAL' : 'FREE'
}

export function cellLabel(c: ShiftCell): string {
  return c === 'FULL' ? 'VOLL' : c === 'PARTIAL' ? 'TEIL' : ''
}

/** Weekly capacity = Σ (full = 1 / partial = 0.5). */
export function shiftCapacity(rows: ShiftRow[]): number {
  let cap = 0
  for (const row of rows) for (const c of row.cells) cap += c === 'FULL' ? 1 : c === 'PARTIAL' ? 0.5 : 0
  return cap
}

/** de-DE capacity number (may be fractional, e.g. "12,5"). */
export function fmtCap(n: number): string {
  return n.toLocaleString('de-DE', { maximumFractionDigits: 1 })
}

/** Normalise an API plan (ensure labels/time + 6 cells per row) or fall back to the default. */
export function normalizeShiftPlan(plan: ShiftPlan | null): ShiftPlan {
  if (!plan || !plan.rows || plan.rows.length === 0) return DEFAULT_SHIFT_PLAN
  const rows = SHIFT_META.map((m, r) => {
    const found = plan.rows.find((x) => x.shift === m.shift) ?? plan.rows[r]
    const cells = (found?.cells ?? DEFAULT_CELLS[r]).slice(0, 6)
    while (cells.length < 6) cells.push('FREE')
    return { shift: m.shift, label: m.label, time: m.time, cells }
  })
  return { week: plan.week ?? DEFAULT_SHIFT_PLAN.week, rows }
}

// ═════════════════════════════════════════════════════════════════════════════
// Produkte — master data (multi-level BOM + routing)
//
// The flat /boms contract carries only materialId + quantity per line (plus labor/energy),
// with no product master, no multi-level structure, no Eigen/Zukauf and no availability. The
// design's Produkte view needs all of these, so this is representative demo master data,
// flagged PRODUCTS_ARE_DEMO. Wire to real fields once the contract carries a structured BOM
// and a routing resource.
// ═════════════════════════════════════════════════════════════════════════════

export const PRODUCTS_ARE_DEMO = true

export type BomAvail = 'ok' | 'low' | 'critical'
export type BomKind = 'Eigenfertigung' | 'Zukauf'

export interface Product {
  id: string
  name: string
  art: string
  variant: string
  discontinued: boolean
}

export interface BomItem {
  pos: string
  name: string
  level: number
  quantity: string
  unit: string
  kind: BomKind
  avail: BomAvail
}

export interface RoutingStep {
  step: string
  operation: string
  machine: string | null // null → "manuell"
  minutes: number
}

export const PRODUCTS: Product[] = [
  { id: 'p1', name: 'Rahmen XL', art: 'ACM-RX', variant: 'Rev. 03', discontinued: false },
  { id: 'p2', name: 'Gehäuse A2', art: 'ACM-GA2', variant: 'Rev. 01', discontinued: false },
  { id: 'p3', name: 'Welle 12 mm', art: 'ACM-W12', variant: 'Serie', discontinued: false },
  { id: 'p4', name: 'Halter C3', art: 'ACM-HC3', variant: 'Rev. 02', discontinued: false },
  { id: 'p5', name: 'Klemme K2', art: 'ACM-K2', variant: 'Serie', discontinued: true },
]

export const BOM_BY_PRODUCT: Record<string, BomItem[]> = {
  p1: [
    { pos: '1', name: 'Rahmen XL — Baugruppe', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.1', name: 'Seitenprofil links', level: 1, quantity: '2', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.2', name: 'Seitenprofil rechts', level: 1, quantity: '2', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.3', name: 'Aluprofil 40×40', level: 2, quantity: '4', unit: 'm', kind: 'Zukauf', avail: 'ok' },
    { pos: '2', name: 'Querstrebe', level: 0, quantity: '3', unit: 'Stk', kind: 'Eigenfertigung', avail: 'low' },
    { pos: '2.1', name: 'Edelstahlblech 2 mm', level: 1, quantity: '1,5', unit: 'm²', kind: 'Zukauf', avail: 'low' },
    { pos: '3', name: 'Verbindungssatz', level: 0, quantity: '1', unit: 'Set', kind: 'Zukauf', avail: 'ok' },
    { pos: '3.1', name: 'Schraube M6×20', level: 1, quantity: '24', unit: 'Stk', kind: 'Zukauf', avail: 'ok' },
    { pos: '3.2', name: 'Kugellager 6204', level: 1, quantity: '4', unit: 'Stk', kind: 'Zukauf', avail: 'critical' },
    { pos: '4', name: 'Motoreinheit', level: 0, quantity: '1', unit: 'Stk', kind: 'Zukauf', avail: 'critical' },
    { pos: '4.1', name: 'Motor 0,75 kW', level: 1, quantity: '1', unit: 'Stk', kind: 'Zukauf', avail: 'critical' },
    { pos: '5', name: 'Pulverbeschichtung', level: 0, quantity: '1', unit: '—', kind: 'Eigenfertigung', avail: 'ok' },
  ],
  p2: [
    { pos: '1', name: 'Gehäuse A2 — Baugruppe', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.1', name: 'Blech-Unterteil', level: 1, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.2', name: 'Edelstahlblech 2 mm', level: 2, quantity: '0,8', unit: 'm²', kind: 'Zukauf', avail: 'low' },
    { pos: '2', name: 'Deckel', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '3', name: 'Dichtung Ø30', level: 0, quantity: '2', unit: 'Stk', kind: 'Zukauf', avail: 'ok' },
  ],
  p3: [
    { pos: '1', name: 'Welle 12 mm', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.1', name: 'Rundstahl Ø14', level: 1, quantity: '0,3', unit: 'm', kind: 'Zukauf', avail: 'ok' },
    { pos: '2', name: 'Kugellager 6204', level: 0, quantity: '2', unit: 'Stk', kind: 'Zukauf', avail: 'critical' },
  ],
  p4: [
    { pos: '1', name: 'Halter C3', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.1', name: 'Aluprofil 40×40', level: 1, quantity: '0,2', unit: 'm', kind: 'Zukauf', avail: 'ok' },
    { pos: '2', name: 'Schraube M6×20', level: 0, quantity: '4', unit: 'Stk', kind: 'Zukauf', avail: 'ok' },
  ],
  p5: [
    { pos: '1', name: 'Klemme K2', level: 0, quantity: '1', unit: 'Stk', kind: 'Eigenfertigung', avail: 'ok' },
    { pos: '1.1', name: 'Federstahlband', level: 1, quantity: '0,1', unit: 'm', kind: 'Zukauf', avail: 'low' },
  ],
}

export const ROUTING_BY_PRODUCT: Record<string, RoutingStep[]> = {
  p1: [
    { step: '10', operation: 'Profile zuschneiden', machine: 'Laser-01', minutes: 4 },
    { step: '20', operation: 'Aufnahmen fräsen', machine: 'CNC-01', minutes: 8 },
    { step: '30', operation: 'Rahmen schweißen', machine: 'Schweiß-01', minutes: 12 },
    { step: '40', operation: 'Entgraten', machine: null, minutes: 5 },
    { step: '50', operation: 'Verbindungssatz montieren', machine: null, minutes: 9 },
    { step: '60', operation: 'Pulverbeschichtung', machine: 'Lackieranlage', minutes: 6 },
    { step: '70', operation: 'Endprüfung', machine: 'Messplatz', minutes: 3 },
  ],
  p2: [
    { step: '10', operation: 'Blech zuschneiden', machine: 'Laser-01', minutes: 3 },
    { step: '20', operation: 'Kanten abkanten', machine: 'Press-02', minutes: 4 },
    { step: '30', operation: 'Schweißen', machine: 'Schweiß-01', minutes: 7 },
    { step: '40', operation: 'Endprüfung', machine: null, minutes: 2 },
  ],
  p3: [
    { step: '10', operation: 'Drehen', machine: 'Dreh-01', minutes: 6 },
    { step: '20', operation: 'Schleifen', machine: 'Schleif-01', minutes: 4 },
    { step: '30', operation: 'Prüfen', machine: 'Messplatz', minutes: 2 },
  ],
  p4: [
    { step: '10', operation: 'Fräsen', machine: 'CNC-01', minutes: 5 },
    { step: '20', operation: 'Bohren', machine: 'CNC-01', minutes: 3 },
    { step: '30', operation: 'Entgraten', machine: null, minutes: 2 },
  ],
  p5: [
    { step: '10', operation: 'Stanzen', machine: 'Press-02', minutes: 2 },
    { step: '20', operation: 'Biegen', machine: 'Press-02', minutes: 2 },
    { step: '30', operation: 'Prüfen', machine: null, minutes: 1 },
  ],
}

export interface ProductMeta {
  bomCount: number
  stepCount: number
  totalMinutes: number
}

export function productMeta(id: string): ProductMeta {
  const bom = BOM_BY_PRODUCT[id] ?? []
  const routing = ROUTING_BY_PRODUCT[id] ?? []
  return {
    bomCount: bom.length,
    stepCount: routing.length,
    totalMinutes: routing.reduce((a, s) => a + s.minutes, 0),
  }
}

/** BOM indentation per the design: 12 + level × 20 px. */
export function bomIndent(level: number): number {
  return 12 + level * 20
}

// ═════════════════════════════════════════════════════════════════════════════
// Aufträge — production board
// ═════════════════════════════════════════════════════════════════════════════

export interface StageMeta {
  id: OrderStage
  label: string
}

export const ORDER_STAGES: StageMeta[] = [
  { id: 'GEPLANT', label: 'GEPLANT' },
  { id: 'RUESTEN', label: 'RÜSTEN' },
  { id: 'IN_ARBEIT', label: 'IN ARBEIT' },
  { id: 'PRUEFUNG', label: 'PRÜFUNG' },
  { id: 'FERTIG', label: 'FERTIG' },
]

export interface OrderColumn {
  stage: StageMeta
  count: number
  orders: ProductionOrder[]
}

export function toOrderColumns(orders: ProductionOrder[]): OrderColumn[] {
  return ORDER_STAGES.map((stage) => {
    const inStage = orders.filter((o) => o.stage === stage.id)
    return { stage, count: inStage.length, orders: inStage }
  })
}

/** Representative board when /production-orders is unavailable (design sample set). */
export const FALLBACK_ORDERS: ProductionOrder[] = [
  { id: 'b1', orderNo: 'FA-1042', productId: 'p2', productName: 'Gehäuse A2', quantity: 250, machine: 'CNC-03', ownerInitials: 'MW', stage: 'IN_ARBEIT' },
  { id: 'b2', orderNo: 'FA-1043', productId: 'p3', productName: 'Welle 12 mm', quantity: 1000, machine: 'Dreh-01', ownerInitials: 'JS', stage: 'GEPLANT' },
  { id: 'b3', orderNo: 'FA-1044', productId: null, productName: 'Deckel B', quantity: 500, machine: 'Press-02', ownerInitials: 'AL', stage: 'RUESTEN' },
  { id: 'b4', orderNo: 'FA-1045', productId: 'p1', productName: 'Rahmen XL', quantity: 80, machine: 'Schweiß-01', ownerInitials: 'MW', stage: 'IN_ARBEIT' },
  { id: 'b5', orderNo: 'FA-1046', productId: 'p4', productName: 'Halter C3', quantity: 320, machine: 'CNC-01', ownerInitials: 'JS', stage: 'PRUEFUNG' },
  { id: 'b6', orderNo: 'FA-1047', productId: null, productName: 'Blende A', quantity: 640, machine: 'Laser-01', ownerInitials: 'AL', stage: 'GEPLANT' },
  { id: 'b7', orderNo: 'FA-1041', productId: null, productName: 'Gehäuse A1', quantity: 250, machine: 'CNC-03', ownerInitials: 'MW', stage: 'FERTIG' },
  { id: 'b8', orderNo: 'FA-1048', productId: null, productName: 'Adapter M8', quantity: 2000, machine: 'Dreh-02', ownerInitials: 'JS', stage: 'GEPLANT' },
  { id: 'b9', orderNo: 'FA-1040', productId: null, productName: 'Grundplatte', quantity: 150, machine: 'Fräs-01', ownerInitials: 'AL', stage: 'FERTIG' },
  { id: 'b10', orderNo: 'FA-1049', productId: 'p5', productName: 'Klemme K2', quantity: 900, machine: 'Press-02', ownerInitials: 'MW', stage: 'IN_ARBEIT' },
]

// ═════════════════════════════════════════════════════════════════════════════
// Maschinen — digital-twin monitor
// ═════════════════════════════════════════════════════════════════════════════

interface MachineStatusMeta {
  label: string
  live: boolean // pulsing dot + full opacity for a running machine
}
const MACHINE_STATUS: Record<MachineStatus, MachineStatusMeta> = {
  RUNNING: { label: 'Läuft', live: true },
  SETUP: { label: 'Rüsten', live: false },
  FAULT: { label: 'Störung', live: false },
  MAINTENANCE: { label: 'Wartung', live: false },
  IDLE: { label: 'Leerlauf', live: false },
}

export function machineStatusLabel(s: MachineStatus): string {
  return MACHINE_STATUS[s]?.label ?? s
}
export function machineIsLive(s: MachineStatus): boolean {
  return MACHINE_STATUS[s]?.live ?? false
}
/** Idle/maintenance machines are dimmed (design: opacity .6 when OEE is 0). */
export function machineDimmed(m: Machine): boolean {
  return m.oee <= 0
}

export type OeeLevel = 'good' | 'warn' | 'bad' | 'off'
export function oeeLevel(oee: number): OeeLevel {
  return oee >= 85 ? 'good' : oee >= 70 ? 'warn' : oee > 0 ? 'bad' : 'off'
}
export function oeeText(oee: number): string {
  return oee > 0 ? `${oee}%` : '—'
}

/** Representative monitor when /machines is unavailable (bonus view; design sample set). */
export const FALLBACK_MACHINES: Machine[] = [
  { id: 'm1', name: 'CNC-01', status: 'RUNNING', oee: 91, availability: 96, performance: 95, quality: 99, progress: 64, currentOrder: 'FA-1046 · Halter C3' },
  { id: 'm2', name: 'CNC-03', status: 'RUNNING', oee: 87, availability: 93, performance: 92, quality: 98, progress: 38, currentOrder: 'FA-1042 · Gehäuse A2' },
  { id: 'm3', name: 'Dreh-01', status: 'SETUP', oee: 78, availability: 82, performance: 96, quality: 99, progress: 0, currentOrder: 'FA-1043 · Welle 12 mm' },
  { id: 'm4', name: 'Dreh-02', status: 'IDLE', oee: 0, availability: 0, performance: 0, quality: 0, progress: 0, currentOrder: '— kein Auftrag' },
  { id: 'm5', name: 'Press-02', status: 'RUNNING', oee: 84, availability: 90, performance: 94, quality: 99, progress: 72, currentOrder: 'FA-1049 · Klemme K2' },
  { id: 'm6', name: 'Laser-01', status: 'RUNNING', oee: 88, availability: 94, performance: 93, quality: 100, progress: 21, currentOrder: 'FA-1047 · Blende A' },
  { id: 'm7', name: 'Schweiß-01', status: 'FAULT', oee: 62, availability: 68, performance: 92, quality: 98, progress: 45, currentOrder: 'FA-1045 · Rahmen XL' },
  { id: 'm8', name: 'Fräs-01', status: 'MAINTENANCE', oee: 0, availability: 0, performance: 0, quality: 0, progress: 0, currentOrder: 'Wartung bis 14:00' },
]

/** Stable initials-avatar palette index (rotates through the four brand area colors). */
export function ownerAvIndex(initials: string | null | undefined): number {
  if (!initials) return 0
  let h = 0
  for (let i = 0; i < initials.length; i++) h = (h * 31 + initials.charCodeAt(i)) | 0
  return Math.abs(h) % 4
}
