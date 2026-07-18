import { api } from '../../api/client'

// ACMEbuild — the "Fertigung" module talks to the production system of record exclusively
// through the REST contract (api/acme-build.yaml, v0.2.0), via the shared api client. Base
// path is /api, so every path here is rooted at /build/…. Roles (WATCH/WORK/ADMIN) are
// enforced server-side; WATCH may read everything below, WORK may write (order moves, shifts).
//
// v0.1.0 surface (products/BOM, capacity, supply projection, feasibility) is REAL master
// data. The shop-floor resources (production orders, shift plan, machines) were added to the
// contract in v0.2.0 for what this UI persists; older backends may 404 them, so the module
// falls back to representative demo data and says so (see buildModel + BuildModule).

// ── Planning surface (contract v0.1.0) ──────────────────────────────────────
export interface BomLine {
  materialId: string
  quantity: number
}

export interface ProductBom {
  productId: string
  laborUnits: number
  energyUnits: number
  lines: BomLine[]
}

export interface Capacity {
  factoryWorkers: number
  laborUnitsPerWorkerDay: number
  factoryLaborUnitsPerDay: number
  harborWorkers: number
  harborUnitsPerWorkerDay: number
  harborUnitsPerDay: number
}

export interface SupplyProjectionItem {
  materialId: string
  requiredQuantity: number
}

export interface SupplyProjection {
  horizonDays: number
  materials: SupplyProjectionItem[]
}

export interface FeasibilityItem {
  productId: string
  quantity: number
}

export interface FeasibilityRequest {
  items: FeasibilityItem[]
  dueDate?: string
}

export interface MaterialRequirement {
  materialId: string
  requiredQuantity: number
  availableQuantity: number
}

export type ScarceKind = 'WORKER' | 'MATERIAL'

export interface ScarceResource {
  resource: ScarceKind
  reference?: string
  shortfall: number
}

export interface FeasibilityResult {
  feasible: boolean
  earliestDate?: string
  requiredLaborUnits: number
  requiredMaterials: MaterialRequirement[]
  scarce: ScarceResource[]
}

// ── Shop floor (contract v0.2.0) ────────────────────────────────────────────
export type OrderStage = 'GEPLANT' | 'RUESTEN' | 'IN_ARBEIT' | 'PRUEFUNG' | 'FERTIG'

export interface ProductionOrder {
  id: string
  orderNo: string
  productId?: string | null
  productName?: string | null
  quantity: number
  machine?: string | null
  ownerInitials?: string | null
  stage: OrderStage
  dueDate?: string | null
}

export interface ProductionOrderCreate {
  productId: string
  quantity: number
  machine?: string | null
  ownerInitials?: string | null
  stage?: OrderStage
  dueDate?: string | null
}

export interface ProductionOrderUpdate {
  stage?: OrderStage
  machine?: string | null
  ownerInitials?: string | null
}

export type ShiftCell = 'FREE' | 'FULL' | 'PARTIAL'
export type ShiftId = 'EARLY' | 'LATE' | 'NIGHT'

export interface ShiftRow {
  shift: ShiftId
  label?: string
  time?: string
  cells: ShiftCell[]
}

export interface ShiftPlan {
  week?: string
  rows: ShiftRow[]
}

export interface ShiftPlanWrite {
  rows: ShiftRow[]
}

export type MachineStatus = 'RUNNING' | 'SETUP' | 'FAULT' | 'MAINTENANCE' | 'IDLE'

export interface Machine {
  id: string
  name: string
  status: MachineStatus
  oee: number
  availability: number
  performance: number
  quality: number
  progress: number
  currentOrder?: string | null
}

function query(params: Record<string, string | undefined>): string {
  const q = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v != null && v !== '') as [string, string][],
  ).toString()
  return q ? `?${q}` : ''
}

/** ACMEbuild endpoints (see api/acme-build.yaml). Frontend talks only through here. */
export const buildApi = {
  // Planning (v0.1.0 — real master data)
  listBoms: () => api.get<ProductBom[]>('/build/boms'),
  getBom: (productId: string) => api.get<ProductBom>(`/build/products/${productId}/bom`),
  getCapacity: () => api.get<Capacity>('/build/capacity'),
  getSupplyProjection: (horizonDays?: number) =>
    api.get<SupplyProjection>(`/build/supply-projection${query({ horizonDays: horizonDays?.toString() })}`),
  checkFeasibility: (body: FeasibilityRequest) => api.post<FeasibilityResult>('/build/feasibility', body),

  // Shop floor (v0.2.0)
  listProductionOrders: (stage?: OrderStage) =>
    api.get<ProductionOrder[]>(`/build/production-orders${query({ stage })}`),
  createProductionOrder: (body: ProductionOrderCreate) =>
    api.post<ProductionOrder>('/build/production-orders', body),
  updateProductionOrder: (id: string, body: ProductionOrderUpdate) =>
    api.patch<ProductionOrder>(`/build/production-orders/${id}`, body),

  getShiftPlan: () => api.get<ShiftPlan>('/build/shift-plan'),
  putShiftPlan: (body: ShiftPlanWrite) => api.put<ShiftPlan>('/build/shift-plan', body),

  listMachines: () => api.get<Machine[]>('/build/machines'),
}
