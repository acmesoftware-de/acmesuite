import type { ModuleDef } from '../modules/registry'

interface ModuleHeaderProps {
  module: ModuleDef
  activeSubKey: string
  onSelectSubView: (key: string) => void
  newLabel: string
  /** Show the "+ NEU" button (gated by write permission and module ownership of actions). */
  showNew: boolean
}

export function ModuleHeader({
  module,
  activeSubKey,
  onSelectSubView,
  newLabel,
  showNew,
}: ModuleHeaderProps) {
  return (
    <div className="acme-modhead">
      <div>
        <div className="acme-eyebrow">{module.eyebrow}</div>
        <h1 className="acme-title">{module.title}</h1>
      </div>

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

      {showNew && <button className="acme-btn-new">{newLabel}</button>}
    </div>
  )
}
