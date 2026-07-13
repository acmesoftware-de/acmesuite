import { LogoGlyph } from './LogoGlyph'
import { MODULES, type ModuleId } from '../modules/registry'
import type { ThemeMode } from '../theme/types'

interface TopBarProps {
  activeMod: ModuleId
  onSelectModule: (id: ModuleId) => void
  mode: ThemeMode
  onToggleTheme: () => void
  userLabel: string
  roleLabel: string
  isAdmin: boolean
  onLogout: () => void
}

function initials(label: string): string {
  const parts = label.trim().split(/\s+/)
  return (parts.length > 1 ? parts[0][0] + parts[1][0] : label.slice(0, 2)).toUpperCase()
}

export function TopBar({
  activeMod,
  onSelectModule,
  mode,
  onToggleTheme,
  userLabel,
  roleLabel,
  isAdmin,
  onLogout,
}: TopBarProps) {
  const visibleModules = MODULES.filter((m) => !m.adminOnly || isAdmin)
  return (
    <div className="acme-topbar">
      <LogoGlyph />
      <span className="acme-brand">
        ACME<span className="acme-brand-dim">SUITE</span>
      </span>

      <div className="acme-divider" />

      <div className="acme-modtabs">
        {visibleModules.map((m) => (
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

      <a
        className="acme-api-link"
        href="/swagger.html"
        target="_blank"
        rel="noopener"
        title="API-Dokumentation (Swagger UI)"
      >
        API
      </a>

      <button className="acme-theme-toggle" onClick={onToggleTheme} title="Hell / Dunkel">
        {mode === 'dark' ? '◐' : '◑'}
      </button>

      <button
        className="acme-avatar acme-avatar--button"
        onClick={onLogout}
        title={`${userLabel} · ${roleLabel} — abmelden`}
      >
        {initials(userLabel)}
      </button>
    </div>
  )
}
