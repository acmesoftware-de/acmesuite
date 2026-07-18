import { api } from '../../api/client'

// ACMEhr — the "Team" module talks to the HR system of record exclusively through the
// REST contract (api/acme-hr.yaml, v0.3.0), via the shared api client. Base path is
// /api, so every path here is rooted at /hr/…. Roles (WATCH/WORK/ADMIN) are enforced
// server-side; WATCH may read everything below, WORK may write.

export type CompensationType = 'HOURLY' | 'SALARIED'
export type AbsenceType = 'VACATION' | 'SICK' | 'CURE' | 'TRAINING' | 'OTHER'
export type AbsenceStatus = 'PLANNED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type RoleKind = 'BUSINESS' | 'GOVERNANCE' | 'SYSTEM'
/** Recruiting pipeline stage (contract enum). */
export type ApplicantStage = 'NEW' | 'SCREENING' | 'INTERVIEW' | 'OFFER' | 'REJECTED'
/** Primary work location. */
export type WorkLocation = 'ONSITE' | 'REMOTE' | 'HYBRID'

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
  workLocation?: WorkLocation | null
}

/** Applicant before being hired (applicant=true), incl. recruiting stage + fit score. */
export interface Applicant {
  id: string
  firstName?: string | null
  lastName?: string | null
  fullName: string
  email?: string | null
  jobTitle?: string | null
  targetOrgUnitId?: string | null
  targetOrgUnitName?: string | null
  appliedOn?: string | null
  stage?: ApplicantStage | null
  matchScore?: number | null
}

export interface ApplicantWrite {
  firstName: string
  lastName: string
  email?: string
  jobTitle?: string
  targetOrgUnitId?: string
  stage?: ApplicantStage
  matchScore?: number
}

export interface EmployeeCreate {
  firstName: string
  lastName: string
  email?: string
  jobTitle?: string
  primaryOrgUnitId?: string
  managerId?: string
  workLocation?: WorkLocation
}

export interface EmployeeUpdate {
  jobTitle?: string
  managerId?: string | null
  active?: boolean
  workLocation?: WorkLocation
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
  createEmployee: (body: EmployeeCreate) => api.post<Employee>('/hr/employees', body),
  updateEmployee: (id: string, body: EmployeeUpdate) => api.patch<Employee>(`/hr/employees/${id}`, body),

  listApplicants: (query?: { unitId?: string; q?: string }) =>
    api.get<Applicant[]>(`/hr/applicants${qs(query as Query)}`),
  createApplicant: (body: ApplicantWrite) => api.post<Applicant>('/hr/applicants', body),
  setApplicantStage: (id: string, stage: ApplicantStage) =>
    api.patch<Applicant>(`/hr/applicants/${id}`, { stage }),
  hireApplicant: (id: string, day = 0) =>
    api.post<Employee>(`/hr/applicants/${id}/hire?day=${day}`),

  listAbsences: (query?: { personId?: string; type?: AbsenceType; status?: AbsenceStatus; from?: string; until?: string }) =>
    api.get<Absence[]>(`/hr/absences${qs(query as Query)}`),

  listRoles: () => api.get<Role[]>('/hr/roles'),
  getPayroll: () => api.get<PayrollSummary>('/hr/payroll'),
}
