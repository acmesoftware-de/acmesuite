import { useState } from 'react'
import { matchTier, type ApplicantColumn, type Stage } from './hrModel'

interface BewerberViewProps {
  columns: ApplicantColumn[]
  /** WORK/ADMIN may move candidates between stages (drag & drop) and hire. Read-only for WATCH. */
  canWrite: boolean
  onMove: (id: string, stage: Stage) => void
  /** Start hiring an OFFER-stage candidate (opens the approval folder). */
  onHire: (id: string) => void
  /** True when the board shows demo candidates (live /applicants unavailable). */
  fallback?: boolean
}

/** Recruiting kanban (NEU · SCREENING · INTERVIEW · ANGEBOT · ABGELEHNT). */
export function BewerberView({ columns, canWrite, onMove, onHire, fallback }: BewerberViewProps) {
  const [dragId, setDragId] = useState<string | null>(null)

  function handleDrop(stage: Stage) {
    if (dragId) onMove(dragId, stage)
    setDragId(null)
  }

  return (
    <>
      {fallback && (
        <div className="acme-hr-demo-note">
          Demo-Bewerber — <code className="acme-code">/applicants</code> ist auf diesem Backend
          noch nicht verfügbar.
        </div>
      )}
      <div className="acme-hr-board">
        {columns.map((col) => (
          <div
            className="acme-hr-col"
            key={col.id}
            onDragOver={(e) => {
              if (canWrite) e.preventDefault()
            }}
            onDrop={() => canWrite && handleDrop(col.id)}
          >
            <div className="acme-hr-col-head">
              <span className={`acme-hr-col-dot acme-hr-col-dot--${col.id}`} />
              <span className="acme-hr-col-label">{col.label}</span>
              <span className="acme-hr-col-count">{col.items.length}</span>
            </div>
            <div className="acme-hr-col-body">
              {col.items.map((a) => (
                <div
                  className={`acme-hr-card${dragId === a.id ? ' is-dragging' : ''}`}
                  key={a.id}
                  draggable={canWrite}
                  onDragStart={() => canWrite && setDragId(a.id)}
                  onDragEnd={() => setDragId(null)}
                >
                  <div className="acme-hr-card-head">
                    <span className={`acme-hr-av acme-hr-av--sm acme-hr-av--${a.avIndex}`}>{a.ini}</span>
                    <div className="acme-hr-card-id">
                      <div className="acme-hr-card-name">{a.name}</div>
                      <div className="acme-hr-card-team">{a.team}</div>
                    </div>
                  </div>
                  <div className="acme-hr-card-role">{a.role}</div>
                  <div className="acme-hr-card-foot">
                    <span className={`acme-hr-match acme-hr-match--${matchTier(a.score)}`}>
                      MATCH {a.score > 0 ? `${a.score}%` : '—'}
                    </span>
                    <span className="acme-hr-age">{a.ageDays > 0 ? `${a.ageDays}T` : 'neu'}</span>
                  </div>
                  {canWrite && col.id === 'angebot' && (
                    <button
                      className="acme-hr-hire"
                      onClick={() => onHire(a.id)}
                      onMouseDown={(e) => e.stopPropagation()}
                    >
                      Einstellen
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
