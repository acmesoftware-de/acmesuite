import { LogoGlyph } from './LogoGlyph'
import { MODULES, type ModuleId } from '../modules/registry'
import type { ThemeMode } from '../theme/types'

interface TopBarProps {
  activeMod: ModuleId
  onSelectModule: (id: ModuleId) => void
  mode: ThemeMode
  onToggleTheme: () => void
}

export function TopBar({ activeMod, onSelectModule, mode, onToggleTheme }: TopBarProps) {
  return (
    <div className="acme-topbar">
      <LogoGlyph />
      <span className="acme-brand">
        ACME<span className="acme-brand-dim">SUITE</span>
      </span>

      <div className="acme-divider" />

      <div className="acme-modtabs">
        {MODULES.map((m) => (
          <div
            key={m.id}
            className={`acme-modtab${m.id === activeMod ? ' is-active' : ''}`}
            onClick={() => onSelectModule(m.id)}
          >
            <span className="acme-modtab-label">{m.name}</span>
            <span className="acme-modtab-underline" />
          </div>
        ))}
      </div>

      <div className="acme-spacer" />

      <div className="acme-search">
        <span>Suchen…</span>
        <span className="acme-kbd">⌘K</span>
      </div>

      <button className="acme-theme-toggle" onClick={onToggleTheme} title="Hell / Dunkel">
        {mode === 'dark' ? '◐' : '◑'}
      </button>

      <div className="acme-avatar">JS</div>
    </div>
  )
}
