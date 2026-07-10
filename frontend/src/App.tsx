import { TopBar } from './shell/TopBar'
import { ModuleHeader } from './shell/ModuleHeader'
import { KpiBar } from './shell/KpiBar'
import { ModulePlaceholder } from './modules/ModulePlaceholder'
import { useShellState } from './shell/useShellState'
import { useTheme } from './theme/ThemeProvider'

export function App() {
  const shell = useShellState()
  const { themeId, mode, toggleMode } = useTheme()
  const { module } = shell

  // The three attributes below are the entire theming interface: the active theme's
  // CSS (themes/) turns them into the whole look. No style values live in this tree.
  return (
    <div className="acme-app" data-theme={themeId} data-mode={mode} data-module={module.id}>
      <TopBar
        activeMod={shell.activeMod}
        onSelectModule={shell.setActiveMod}
        mode={mode}
        onToggleTheme={toggleMode}
      />

      <ModuleHeader
        module={module}
        activeSubKey={shell.activeSubKey}
        onSelectSubView={shell.setSubView}
        newLabel={shell.newLabel}
      />

      {shell.showKpis && <KpiBar kpis={module.kpis} />}

      <ModulePlaceholder module={module} activeSub={shell.activeSub} />
    </div>
  )
}
