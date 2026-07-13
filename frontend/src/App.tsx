import { TopBar } from './shell/TopBar'
import { ModuleHeader } from './shell/ModuleHeader'
import { KpiBar } from './shell/KpiBar'
import { ModulePlaceholder } from './modules/ModulePlaceholder'
import { AdminModule } from './modules/admin/AdminModule'
import { useShellState } from './shell/useShellState'
import { Assist } from './assist/Assist'
import { useTheme } from './theme/ThemeProvider'
import { useAuth } from './auth/AuthContext'
import { LoginScreen } from './auth/LoginScreen'
import { SetPasswordScreen } from './auth/SetPasswordScreen'
import { versionLabel } from './version'

export function App() {
  const { themeId, mode, toggleMode } = useTheme()
  const auth = useAuth()
  const shell = useShellState()
  const { module } = shell

  // The root always carries the theming attributes; data-module also drives --accent on
  // the login/splash screens (fall back to CRM before a module is active).
  const dataModule = auth.phase === 'authed' ? module.id : 'CRM'

  return (
    <div
      className="acme-app"
      data-theme={themeId}
      data-mode={mode}
      data-module={dataModule}
      data-build={versionLabel}
    >
      {auth.phase === 'loading' && <div className="acme-splash">ACMEsuite…</div>}

      {auth.phase === 'anon' && <LoginScreen />}

      {auth.phase === 'mustSetPassword' && <SetPasswordScreen />}

      {auth.phase === 'authed' && (
        <>
          <TopBar
            activeMod={shell.activeMod}
            onSelectModule={shell.setActiveMod}
            mode={mode}
            onToggleTheme={toggleMode}
            userLabel={auth.user?.displayName || auth.user?.username || '—'}
            roleLabel={auth.user?.role ?? ''}
            isAdmin={auth.isAdmin}
            onLogout={auth.logout}
          />

          <ModuleHeader
            module={module}
            activeSubKey={shell.activeSubKey}
            onSelectSubView={shell.setSubView}
            newLabel={shell.newLabel}
            showNew={auth.canWrite && !module.ownsActions}
          />

          {shell.showKpis && <KpiBar kpis={module.kpis} />}

          {module.id === 'ADM' ? (
            <AdminModule subView={shell.activeSubKey} />
          ) : (
            <ModulePlaceholder module={module} activeSub={shell.activeSub} />
          )}

          <Assist module={shell.activeMod} subView={shell.activeSubKey} />
        </>
      )}
    </div>
  )
}
