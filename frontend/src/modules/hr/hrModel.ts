// Derivations for the HR "Team" module. The REST contract (acme-hr.yaml) is the system
// of record for people, reporting lines and absences; the design's presentational shapes
// (a 4-state team status, an org chart, a recruiting pipeline) are derived here from that
// raw data. Where the contract genuinely lacks a field the design needs, it is synthesised
// deterministically and flagged MOCK — never silently invented.

import type { Absence, Applicant, Employee } from './hrApi'

// ── shared helpers ──────────────────────────────────────────────────────────
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  const s = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0].slice(0, 2)
  return s.toUpperCase()
}

/** Stable non-negative hash of a string — for deterministic MOCK fields (no Math.random). */
function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function overlapsToday(a: Absence): boolean {
  const t = today()
  const from = a.period?.from
  const until = a.period?.until
  if (!from) return false
  return from <= t && (!until || t <= until)
}

// ── Team view ───────────────────────────────────────────────────────────────
export type TeamStatus = 'aktiv' | 'remote' | 'urlaub' | 'krank'

export interface TeamRow {
  id: string
  name: string
  role: string
  team: string
  status: TeamStatus
  ini: string
  avIndex: number
}

const CURRENT_ABSENCE: Partial<Record<string, TeamStatus>> = { SICK: 'krank', VACATION: 'urlaub', CURE: 'urlaub' }

/**
 * Team status per the design (aktiv/remote/urlaub/krank). Vacation/sick are REAL — derived
 * from an absence overlapping today. "remote" has no field in the contract, so a small,
 * deterministic subset is flagged remote as a MOCK placeholder (id-hash, ~1 in 6) purely so
 * the four-state pill is demonstrable; replace once a work-location field lands in the API.
 */
function deriveStatus(emp: Employee, absencesByPerson: Map<string, Absence[]>): TeamStatus {
  const abs = absencesByPerson.get(emp.id) ?? []
  for (const a of abs) {
    if (a.status === 'REJECTED' || a.status === 'CANCELLED') continue
    if (!overlapsToday(a)) continue
    const mapped = CURRENT_ABSENCE[a.type]
    if (mapped) return mapped
  }
  if (hash(emp.id) % 6 === 0) return 'remote' // MOCK: no work-location field in the contract
  return 'aktiv'
}

/** Hired, active staff as team rows (applicants excluded — they live in the Bewerber board). */
export function buildTeam(employees: Employee[], absences: Absence[]): TeamRow[] {
  const byPerson = groupAbsences(absences)
  return employees
    .filter((e) => e.active && !e.applicant)
    .sort((a, b) => a.fullName.localeCompare(b.fullName, 'de'))
    .map((e, i) => ({
      id: e.id,
      name: e.fullName,
      role: e.jobTitle ?? '—',
      team: e.primaryOrgUnitName ?? '—',
      status: deriveStatus(e, byPerson),
      ini: initials(e.fullName),
      avIndex: i % 4,
    }))
}

export interface AbsentRow {
  id: string
  name: string
  role: string
  status: Extract<TeamStatus, 'urlaub' | 'krank'>
  ini: string
}

/** "Abwesend heute" rail — people on vacation or sick today (real absences). */
export function absentToday(employees: Employee[], absences: Absence[]): AbsentRow[] {
  const byId = new Map(employees.map((e) => [e.id, e]))
  const seen = new Set<string>()
  const rows: AbsentRow[] = []
  for (const a of absences) {
    if (a.status === 'REJECTED' || a.status === 'CANCELLED') continue
    if (!overlapsToday(a)) continue
    const status = CURRENT_ABSENCE[a.type]
    if (status !== 'urlaub' && status !== 'krank') continue
    if (seen.has(a.personId)) continue
    seen.add(a.personId)
    const emp = byId.get(a.personId)
    const name = a.personName ?? emp?.fullName ?? a.personId
    rows.push({ id: a.personId, name, role: emp?.jobTitle ?? '—', status, ini: initials(name) })
  }
  return rows
}

function groupAbsences(absences: Absence[]): Map<string, Absence[]> {
  const m = new Map<string, Absence[]>()
  for (const a of absences) {
    const arr = m.get(a.personId)
    if (arr) arr.push(a)
    else m.set(a.personId, [a])
  }
  return m
}

// ── Org chart ─────────────────────────────────────────────────────────────
export interface OrgArea {
  id: string
  name: string
  role: string
  ini: string
  teamLabel: string
  colorIndex: number // 0..3 → area color (red/blue/green/yellow)
  reports: string[]
}

export interface OrgChart {
  ceo: { id: string; name: string; role: string; ini: string } | null
  areas: OrgArea[]
}

/**
 * Org hierarchy derived from the reporting line (managerId). The top is the root with the
 * largest team; its managing direct reports become the area cards, each showing team size
 * (whole subtree) and a couple of named reports. Colors rotate through the four brand area
 * colors. Robust to multiple roots / flat orgs.
 */
export function buildOrg(employees: Employee[]): OrgChart {
  const people = employees.filter((e) => e.active && !e.applicant)
  const byId = new Map(people.map((e) => [e.id, e]))
  const childrenOf = new Map<string, Employee[]>()
  for (const e of people) {
    const mid = e.managerId && byId.has(e.managerId) ? e.managerId : null
    if (!mid) continue
    const arr = childrenOf.get(mid)
    if (arr) arr.push(e)
    else childrenOf.set(mid, [e])
  }

  const subtreeSize = (id: string): number => {
    // Iterative DFS with a visited guard (defensive against cycles in imported data).
    let count = 0
    const stack = [...(childrenOf.get(id) ?? [])]
    const seen = new Set<string>()
    while (stack.length) {
      const n = stack.pop()!
      if (seen.has(n.id)) continue
      seen.add(n.id)
      count++
      stack.push(...(childrenOf.get(n.id) ?? []))
    }
    return count
  }

  const roots = people.filter((e) => !e.managerId || !byId.has(e.managerId))
  if (roots.length === 0) return { ceo: null, areas: [] }

  const ceo = roots
    .slice()
    .sort((a, b) => subtreeSize(b.id) - subtreeSize(a.id))[0]

  const directReports = childrenOf.get(ceo.id) ?? []
  // Area leads = the CEO's reports who themselves lead people; fall back to all reports
  // if the org under the CEO is flat.
  const leads = directReports.filter((r) => (childrenOf.get(r.id)?.length ?? 0) > 0)
  const areaLeads = leads.length > 0 ? leads : directReports

  const areas: OrgArea[] = areaLeads.map((lead, i) => {
    const size = subtreeSize(lead.id)
    const reps = (childrenOf.get(lead.id) ?? []).map((r) => r.fullName)
    const shown = reps.slice(0, 2)
    const rest = size - shown.length
    const reports = rest > 0 ? [...shown, `+ ${rest}`] : shown
    return {
      id: lead.id,
      name: lead.fullName,
      role: lead.jobTitle ?? '—',
      ini: initials(lead.fullName),
      teamLabel: `${lead.primaryOrgUnitName ?? 'Team'} · ${size}`,
      colorIndex: i % 4,
      reports,
    }
  })

  return { ceo: { id: ceo.id, name: ceo.fullName, role: ceo.jobTitle ?? 'CEO', ini: initials(ceo.fullName) }, areas }
}

// ── Bewerber (recruiting) board ───────────────────────────────────────────
export type Stage = 'neu' | 'screening' | 'interview' | 'angebot' | 'abgelehnt'

export interface ApplicantCard {
  id: string
  name: string
  team: string
  role: string
  stage: Stage
  score: number // 0 = not yet scored
  ageDays: number
  ini: string
  avIndex: number
}

export interface StageMeta {
  id: Stage
  label: string
}

export const STAGES: StageMeta[] = [
  { id: 'neu', label: 'NEU' },
  { id: 'screening', label: 'SCREENING' },
  { id: 'interview', label: 'INTERVIEW' },
  { id: 'angebot', label: 'ANGEBOT' },
  { id: 'abgelehnt', label: 'ABGELEHNT' },
]

const STAGE_IDS: Stage[] = STAGES.map((s) => s.id)

export type MatchTier = 'good' | 'mid' | 'bad' | 'none'
export function matchTier(score: number): MatchTier {
  if (score <= 0) return 'none'
  if (score >= 85) return 'good'
  if (score >= 70) return 'mid'
  return 'bad'
}

function ageInDays(appliedOn?: string | null, fallbackSeed = 0): number {
  if (appliedOn) {
    const applied = Date.parse(appliedOn)
    if (!Number.isNaN(applied)) {
      const days = Math.floor((Date.now() - applied) / 86_400_000)
      return Math.max(0, days)
    }
  }
  return fallbackSeed % 12 // MOCK fallback when appliedOn is absent
}

/**
 * Turn contract applicants into pipeline cards. Pipeline `stage` and `matchScore` are NOT in
 * the contract, so both are synthesised deterministically from the applicant id (MOCK, stable
 * per candidate) — clearly separated from the real name/role/unit/appliedOn fields. Wire these
 * to real fields once the contract carries a recruiting stage + score.
 */
export function toCard(a: Applicant): ApplicantCard {
  const h = hash(a.id)
  const stage = STAGE_IDS[h % STAGE_IDS.length] // MOCK
  const score = stage === 'neu' ? 0 : 55 + (h % 43) // MOCK: 55–97, unscored while NEU
  return {
    id: a.id,
    name: a.fullName,
    team: a.targetOrgUnitId ? unitShort(a.targetOrgUnitId) : '—',
    role: a.jobTitle ?? '—',
    stage,
    score,
    ageDays: ageInDays(a.appliedOn, h),
    ini: initials(a.fullName),
    avIndex: h % 4,
  }
}

function unitShort(unitId: string): string {
  // "ou-vertrieb" → "Vertrieb"; best-effort label from the unit id.
  const tail = unitId.replace(/^ou-/, '').replace(/[-_]+/g, ' ').trim()
  return tail ? tail.charAt(0).toUpperCase() + tail.slice(1) : unitId
}

export interface ApplicantColumn {
  id: Stage
  label: string
  items: ApplicantCard[]
}

export function toColumns(cards: ApplicantCard[]): ApplicantColumn[] {
  return STAGES.map((s) => ({ id: s.id, label: s.label, items: cards.filter((c) => c.stage === s.id) }))
}

/**
 * Representative applicants for the Bewerber board when the live `/applicants` endpoint is
 * unavailable (older backends predate it) — the design's sample set, clearly a demo fallback.
 */
type ApplicantSeed = Pick<ApplicantCard, 'name' | 'role' | 'team' | 'stage' | 'score' | 'ageDays'>

const FALLBACK_SEED: ApplicantSeed[] = [
  { name: 'Sophie Berger', role: 'Account Executive', team: 'Vertrieb', stage: 'interview', score: 88, ageDays: 2 },
  { name: 'Jonas Weber', role: 'CNC-Techniker', team: 'Produktion', stage: 'screening', score: 74, ageDays: 1 },
  { name: 'Aylin Demir', role: 'Disponentin', team: 'Supply', stage: 'angebot', score: 92, ageDays: 5 },
  { name: 'Marco Rossi', role: 'QS-Ingenieur', team: 'Produktion', stage: 'interview', score: 81, ageDays: 3 },
  { name: 'Hanna Groß', role: 'People Partner', team: 'People', stage: 'neu', score: 0, ageDays: 0 },
  { name: 'Ben Fischer', role: 'Sales Ops', team: 'Vertrieb', stage: 'screening', score: 69, ageDays: 4 },
  { name: 'Clara Vogt', role: 'IT-Admin', team: 'IT', stage: 'abgelehnt', score: 55, ageDays: 8 },
  { name: 'Ida Nowak', role: 'Werkstudentin QS', team: 'Produktion', stage: 'angebot', score: 85, ageDays: 6 },
]

export const FALLBACK_APPLICANTS: ApplicantCard[] = FALLBACK_SEED.map((a, i) => ({
  id: `mock-${i}`,
  ini: initials(a.name),
  avIndex: i % 4,
  ...a,
}))
