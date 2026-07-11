import type { ModuleDef, SubView } from './registry'

interface ModulePlaceholderProps {
  module: ModuleDef
  activeSub?: SubView
}

// Content-area stand-in until the real module views are built. Keeps the shell fully
// navigable (module switch, sub-view tabs, theme, mode) and shows what plugs in here.
export function ModulePlaceholder({ module, activeSub }: ModulePlaceholderProps) {
  return (
    <div className="acme-content">
      <div className="acme-placeholder">
        <div className="acme-eyebrow">{module.eyebrow}</div>
        <div className="acme-placeholder-title">{activeSub ? activeSub.label : module.title}</div>
        <div className="acme-placeholder-desc">
          Ansicht in Arbeit. Die App-Shell steht — dieses Modul wird als Nächstes gegen die
          ACMEsuite-API gebaut.
        </div>
      </div>
    </div>
  )
}
