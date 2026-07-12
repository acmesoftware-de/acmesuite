import type { ReactNode } from 'react'
import type { ModuleDef } from '../modules/registry'

interface ModuleHeaderProps {
  module: ModuleDef
  activeSubKey: string
  onSelectSubView: (key: string) => void
  newLabel: string
  /** Show the "+ NEU" button (gated by write permission and module ownership of actions). */
  showNew: boolean
  /** Invoked when the "+ NEU" button is pressed (module-specific create action). */
  onNew?: () => void
  /** Optional module-provided control shown in the header, left of "+ NEU"
   *  (e.g. CRM's Tabelle/Kanban/Funnel switch on the Pipeline sub-view). */
  headerExtra?: ReactNode
}

export function ModuleHeader({
  module,
  activeSubKey,
  onSelectSubView,
  newLabel,
  showNew,
  onNew,
  headerExtra,
}: ModuleHeaderProps) {
  // The active sub-view may override the large page title (e.g. CRM: Pipeline/Kunden/Kontakte).
  const activeSub = module.subViews.find((sv) => sv.key === activeSubKey)
  const title = activeSub?.title ?? module.title

  return (
    <div className="acme-modhead">
      <div className="acme-modhead-lead">
        <div className="acme-eyebrow">{module.eyebrow}</div>
        <h1 className="acme-title">{title}</h1>
      </div>

      {headerExtra}

      <div className="acme-spacer" />

      {module.subViews.length > 0 && (
        <div className="acme-subtabs">
          {module.subViews.map((sv) => (
            <button
              key={sv.key}
              className={`acme-subtab${sv.key === activeSubKey ? ' is-active' : ''}`}
              onClick={() => onSelectSubView(sv.key)}
            >
              {sv.label}
            </button>
          ))}
        </div>
      )}

      {showNew && (
        <button className="acme-btn-new" onClick={onNew}>
          {newLabel}
        </button>
      )}
    </div>
  )
}
