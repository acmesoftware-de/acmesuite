import { useState } from 'react'
import type { DealActions } from './CrmModule'
import type { Deal } from './crmApi'
import { byStage, fmtK, ownerInitials } from './pipelineModel'

/** 5-stage board. Cards drag between columns to set their stage (write-gated). */
export function KanbanView({ deals, actions }: { deals: Deal[]; actions: DealActions }) {
  const { canWrite } = actions
  const [dragId, setDragId] = useState<string | null>(null)
  const [overStage, setOverStage] = useState<string | null>(null)

  const columns = byStage(deals)

  return (
    <div className="acme-board">
      {columns.map((col) => (
        <div className="acme-kan-col" key={col.stage.id}>
          <div className="acme-kan-head">
            <span className="acme-kan-dot" data-stage={col.stage.id} />
            <span className="acme-kan-label">{col.stage.label}</span>
            <span className="acme-kan-count">{col.count}</span>
            <span className="acme-kan-sum">{fmtK(col.sum)}</span>
          </div>

          <div
            className={`acme-kan-body${overStage === col.stage.id ? ' is-over' : ''}`}
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
                      const moved = deals.find((d) => d.id === dragId)
                      if (moved && moved.stage !== col.stage.id) actions.setStage(dragId, col.stage.id)
                    }
                    setDragId(null)
                    setOverStage(null)
                  }
                : undefined
            }
          >
            {col.deals.map((d) => (
              <div
                className={`acme-kan-card${dragId === d.id ? ' is-dragging' : ''}`}
                key={d.id}
                data-owner={ownerInitials(d)}
                draggable={canWrite}
                onDragStart={() => setDragId(d.id)}
                onDragEnd={() => {
                  setDragId(null)
                  setOverStage(null)
                }}
              >
                <div className="acme-kan-company">{d.company}</div>
                <div className="acme-kan-contact">{d.contact}</div>
                <div className="acme-kan-meta">
                  <span className="acme-avatar acme-avatar--card" data-owner={ownerInitials(d)}>
                    {ownerInitials(d)}
                  </span>
                  <span className="acme-kan-value">{fmtK(d.value?.amount ?? 0)}</span>
                  {d.ageDays != null && <span className="acme-kan-age">{d.ageDays}T</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
