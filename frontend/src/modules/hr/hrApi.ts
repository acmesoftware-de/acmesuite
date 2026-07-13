import { api } from '../../api/client'

// ACMEhr — the "Team" module talks to the HR system of record exclusively through the
// REST contract (api/acme-hr.yaml, v0.1.0), via the shared api client. Base path is
// /api, so every path here is rooted at /hr/…. Roles (WATCH/WORK/ADMIN) are enforced
// server-side; WATCH may read everything below, WORK may write.

export type CompensationType = 'HOURLY' | 'SALARIED'
export type AbsenceType = 'VACATION' | 'SICK' | 'CURE' | 'TRAINING' | 'OTHER'
export type AbsenceStatus = 'PLANNED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type RoleKind = 'BUSINESS' | 'GOVERNANCE' | 'SYSTEM'

/** Mirrors acme-base.yaml#/components/schemas/DateRange. */
export interface DateRange {
  from: string | null
  until: string | null
}

export interface Employee {
  id: string
  firstName?: string | null
  lastName?: string | null
  fullName: string
  email?: string | null
  jobTitle?: string | null
  active: boolean
  applicant?: boolean
  primaryOrgUnitId?: string | null
  primaryOrgUnitName?: string | null
  managerId?: string | null
  deputyIds?: string[]
  assistantIds?: string[]
  secondaryUnitIds?: string[]
  compType?: CompensationType
  hourlyRate?: number | null
}

/** Applicant before being hired (applicant=true). Pipeline stage / match score are NOT
 *  part of the contract yet — see hrModel for how the Bewerber board synthesises them. */
export interface Applicant {
  id: string
  firstName?: string | null
  lastName?: string | null
  fullName: string
  email?: string | null
  jobTitle?: string | null
  targetOrgUnitId?: string | null
  appliedOn?: string | null
}

export interface Absence {
  id: string
  personId: string
  personName?: string
  type: AbsenceType
  status: AbsenceStatus
  period: DateRange
  substituteId?: string | null
  substituteName?: string | null
  reasonKey?: string | null
  note?: string | null
  workingDays?: number | null
}

export interface Role {
  id: string
  title: string
  kind: RoleKind
  description?: string | null
}

export interface PayrollSummary {
  salariedCount: number
  hourlyCount: number
  weeklySalariedEur: number
  weeklyHourlyEur: number
  weeklyTotalEur: number
}

export interface EmployeeQuery {
  active?: boolean
  unitId?: string
  q?: string
}

function qs(params?: Record<string, string | boolean | undefined>): string {
  if (!params) return ''
  const p = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined) p.set(k, String(v))
  }
  const s = p.toString()
  return s ? `?${s}` : ''
}

type Query = Record<string, string | boolean | undefined>

export const hrApi = {
  listEmployees: (query?: EmployeeQuery) =>
    api.get<Employee[]>(`/hr/employees${qs(query as Query)}`),
  getEmployee: (id: string) => api.get<Employee>(`/hr/employees/${id}`),
  listEmployeeAbsences: (id: string) => api.get<Absence[]>(`/hr/employees/${id}/absences`),

  listApplicants: (query?: { unitId?: string; q?: string }) =>
    api.get<Applicant[]>(`/hr/applicants${qs(query as Query)}`),
  hireApplicant: (id: string, day = 0) =>
    api.post<Employee>(`/hr/applicants/${id}/hire?day=${day}`),

  listAbsences: (query?: { personId?: string; type?: AbsenceType; status?: AbsenceStatus; from?: string; until?: string }) =>
    api.get<Absence[]>(`/hr/absences${qs(query as Query)}`),

  listRoles: () => api.get<Role[]>('/hr/roles'),
  getPayroll: () => api.get<PayrollSummary>('/hr/payroll'),
}
