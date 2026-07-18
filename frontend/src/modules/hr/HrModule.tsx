import { useEffect, useMemo, useState } from 'react'
import { hrApi, type Absence, type Employee, type WorkLocation } from './hrApi'
import {
  FALLBACK_APPLICANTS,
  absentToday,
  buildOrg,
  buildTeam,
  toApiStage,
  toCard,
  toColumns,
  type ApplicantCard,
  type Stage,
} from './hrModel'
import { TeamView } from './TeamView'
import { OrgChartView } from './OrgChartView'
import { BewerberView } from './BewerberView'

interface HrModuleProps {
  /** Active sub-view key from the shell (team | orgchart | bewerber). */
  subView: string
  /** WORK/ADMIN may perform write actions (create, drag & drop, hire). Gated by the role context. */
  canWrite: boolean
  /** Bumped when the shell's "+ NEU" button is pressed → open the create form. */
  newTick?: number
}

interface OrgUnitOpt {
  id: string
  name: string
}

/**
 * HR "Team" module (design module 02 = Personal). Loads people, absences and applicants from
 * the ACMEhr contract and renders the sub-view chosen in the shell. All data is real now:
 * reporting lines, absences, work location, and the recruiting pipeline (stage + match score).
 */
export function HrModule({ subView, canWrite, newTick = 0 }: HrModuleProps) {
  const [employees, setEmployees] = useState<Employee[] | null>(null)
  const [absences, setAbsences] = useState<Absence[]>([])
  const [peopleError, setPeopleError] = useState(false)

  const [cards, setCards] = useState<ApplicantCard[] | null>(null)
  const [applicantsFallback, setApplicantsFallback] = useState(false)

  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([hrApi.listEmployees(), hrApi.listAbsences()])
      .then(([emps, abs]) => {
        setEmployees(emps)
        setAbsences(abs)
      })
      .catch(() => setPeopleError(true))

    // The recruiting board tolerates a missing /applicants endpoint (older backends): fall
    // back to representative demo candidates so the pipeline stays demonstrable.
    hrApi
      .listApplicants()
      .then((list) => {
        if (list.length > 0) setCards(list.map(toCard))
        else {
          setCards(FALLBACK_APPLICANTS)
          setApplicantsFallback(true)
        }
      })
      .catch(() => {
        setCards(FALLBACK_APPLICANTS)
        setApplicantsFallback(true)
      })
  }, [])

  // The shell's "+ NEU" button (bumps newTick) opens the create form for the active view.
  useEffect(() => {
    if (newTick > 0 && canWrite) setCreating(true)
  }, [newTick, canWrite])
  // Reset any open form / notice when switching sub-views.
  useEffect(() => {
    setCreating(false)
    setNotice(null)
  }, [subView])

  const team = useMemo(() => (employees ? buildTeam(employees, absences) : []), [employees, absences])
  const absent = useMemo(() => (employees ? absentToday(employees, absences) : []), [employees, absences])
  const org = useMemo(() => (employees ? buildOrg(employees) : { ceo: null, areas: [] }), [employees])

  const units = useMemo<OrgUnitOpt[]>(() => {
    const seen = new Map<string, string>()
    for (const e of employees ?? []) {
      if (e.primaryOrgUnitId && !seen.has(e.primaryOrgUnitId)) {
        seen.set(e.primaryOrgUnitId, e.primaryOrgUnitName ?? e.primaryOrgUnitId)
      }
    }
    return [...seen].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name, 'de'))
  }, [employees])

  async function moveApplicant(id: string, stage: Stage) {
    const previous = cards
    setCards((cs) => cs?.map((c) => (c.id === id ? { ...c, stage } : c)) ?? null)
    if (applicantsFallback) return // demo candidates aren't persisted
    try {
      await hrApi.setApplicantStage(id, toApiStage(stage))
    } catch {
      setCards(previous) // roll back the optimistic move
      setNotice('Stufe konnte nicht gespeichert werden.')
    }
  }

  async function hire(id: string) {
    try {
      await hrApi.hireApplicant(id)
      setNotice('Einstellung angestoßen — Freigabe läuft.')
    } catch {
      setNotice('Einstellung fehlgeschlagen.')
    }
  }

  async function createApplicant(form: CreateForm) {
    const created = await hrApi.createApplicant({
      firstName: form.firstName,
      lastName: form.lastName,
      jobTitle: form.jobTitle || undefined,
      targetOrgUnitId: form.unitId || undefined,
    })
    setCards((cs) => [toCard(created), ...(cs ?? [])])
    setApplicantsFallback(false)
    setCreating(false)
  }

  async function createEmployee(form: CreateForm) {
    const created = await hrApi.createEmployee({
      firstName: form.firstName,
      lastName: form.lastName,
      jobTitle: form.jobTitle || undefined,
      primaryOrgUnitId: form.unitId || undefined,
      workLocation: form.workLocation,
    })
    setEmployees((es) => [...(es ?? []), created])
    setCreating(false)
  }

  const bewerber = subView === 'bewerber'

  let content: JSX.Element
  if (bewerber) {
    content = cards ? (
      <BewerberView
        columns={toColumns(cards)}
        canWrite={canWrite}
        onMove={moveApplicant}
        onHire={hire}
        fallback={applicantsFallback}
      />
    ) : (
      <Loading />
    )
  } else if (peopleError) {
    content = <div className="acme-error">Konnte HR-Daten nicht laden.</div>
  } else if (!employees) {
    content = <Loading />
  } else if (subView === 'orgchart') {
    content = <OrgChartView chart={org} />
  } else {
    content = <TeamView rows={team} absent={absent} />
  }

  return (
    <div className="acme-hr-module">
      {notice && (
        <div className="acme-notice">
          {notice}
          <button className="acme-notice-close" onClick={() => setNotice(null)}>
            ✕
          </button>
        </div>
      )}
      {creating && (subView === 'team' || bewerber) && (
        <PersonForm
          kind={bewerber ? 'applicant' : 'employee'}
          units={units}
          onCancel={() => setCreating(false)}
          onSubmit={(f) => (bewerber ? createApplicant(f) : createEmployee(f)).catch(() =>
            setNotice(bewerber ? 'Bewerber anlegen fehlgeschlagen.' : 'Mitarbeiter anlegen fehlgeschlagen.'),
          )}
        />
      )}
      {content}
    </div>
  )
}

interface CreateForm {
  firstName: string
  lastName: string
  jobTitle: string
  unitId: string
  workLocation: WorkLocation
}

function PersonForm({
  kind,
  units,
  onSubmit,
  onCancel,
}: {
  kind: 'applicant' | 'employee'
  units: OrgUnitOpt[]
  onSubmit: (form: CreateForm) => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<CreateForm>({
    firstName: '',
    lastName: '',
    jobTitle: '',
    unitId: '',
    workLocation: 'ONSITE',
  })
  const set = (patch: Partial<CreateForm>) => setForm((f) => ({ ...f, ...patch }))
  const valid = form.firstName.trim() !== '' && form.lastName.trim() !== ''

  return (
    <div className="acme-form-card">
      <div className="acme-form-grid">
        <label className="acme-field">
          <span className="acme-label">Vorname</span>
          <input className="acme-input" value={form.firstName} onChange={(e) => set({ firstName: e.target.value })} />
        </label>
        <label className="acme-field">
          <span className="acme-label">Nachname</span>
          <input className="acme-input" value={form.lastName} onChange={(e) => set({ lastName: e.target.value })} />
        </label>
        <label className="acme-field">
          <span className="acme-label">{kind === 'applicant' ? 'Zielrolle' : 'Rolle'}</span>
          <input className="acme-input" value={form.jobTitle} onChange={(e) => set({ jobTitle: e.target.value })} />
        </label>
        <label className="acme-field">
          <span className="acme-label">Team</span>
          <select className="acme-select" value={form.unitId} onChange={(e) => set({ unitId: e.target.value })}>
            <option value="">—</option>
            {units.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}
              </option>
            ))}
          </select>
        </label>
        {kind === 'employee' && (
          <label className="acme-field">
            <span className="acme-label">Arbeitsort</span>
            <select
              className="acme-select"
              value={form.workLocation}
              onChange={(e) => set({ workLocation: e.target.value as WorkLocation })}
            >
              <option value="ONSITE">Vor Ort</option>
              <option value="REMOTE">Remote</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </label>
        )}
      </div>
      <div className="acme-form-actions">
        <button className="acme-btn acme-btn--ghost" onClick={onCancel}>
          Abbrechen
        </button>
        <button className="acme-btn" disabled={!valid} onClick={() => onSubmit(form)}>
          {kind === 'applicant' ? 'Bewerber anlegen' : 'Mitarbeiter anlegen'}
        </button>
      </div>
    </div>
  )
}

function Loading() {
  return <div className="acme-hr-loading">Lädt…</div>
}
