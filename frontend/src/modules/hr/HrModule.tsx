import { useEffect, useMemo, useState } from 'react'
import { hrApi, type Absence, type Employee } from './hrApi'
import {
  FALLBACK_APPLICANTS,
  absentToday,
  buildOrg,
  buildTeam,
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
  /** WORK/ADMIN may perform write actions (drag & drop). Gated by the role context. */
  canWrite: boolean
}

/**
 * HR "Team" module (design module 02 = Personal). Loads people, absences and applicants from
 * the ACMEhr contract and renders the sub-view chosen in the shell. Employees, reporting lines
 * and absences are real; the recruiting pipeline's stage/score are synthesised (see hrModel).
 */
export function HrModule({ subView, canWrite }: HrModuleProps) {
  const [employees, setEmployees] = useState<Employee[] | null>(null)
  const [absences, setAbsences] = useState<Absence[]>([])
  const [peopleError, setPeopleError] = useState(false)

  const [cards, setCards] = useState<ApplicantCard[] | null>(null)
  const [applicantsFallback, setApplicantsFallback] = useState(false)

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

  const team = useMemo(() => (employees ? buildTeam(employees, absences) : []), [employees, absences])
  const absent = useMemo(() => (employees ? absentToday(employees, absences) : []), [employees, absences])
  const org = useMemo(() => (employees ? buildOrg(employees) : { ceo: null, areas: [] }), [employees])

  function moveApplicant(id: string, stage: Stage) {
    setCards((prev) => prev?.map((c) => (c.id === id ? { ...c, stage } : c)) ?? null)
  }

  let content: JSX.Element
  if (subView === 'bewerber') {
    content = cards ? (
      <BewerberView
        columns={toColumns(cards)}
        canWrite={canWrite}
        onMove={moveApplicant}
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

  return <div className="acme-hr-module">{content}</div>
}

function Loading() {
  return <div className="acme-hr-loading">Lädt…</div>
}
