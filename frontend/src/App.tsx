import { useState } from 'react'
import { TopBar } from './shell/TopBar'
import { ModuleHeader } from './shell/ModuleHeader'
import { KpiBar } from './shell/KpiBar'
import { ModulePlaceholder } from './modules/ModulePlaceholder'
import { AdminModule } from './modules/admin/AdminModule'
import { CrmModule } from './modules/crm/CrmModule'
import { PIPELINE_MODES, type PipelineMode } from './modules/crm/PipelineView'
import { HrModule } from './modules/hr/HrModule'
import { useShellState } from './shell/useShellState'
import { Assist } from './assist/Assist'
import { useTheme } from './theme/ThemeProvider'
import { useAuth } from './auth/AuthContext'
import { LoginScreen } from './auth/LoginScreen'
import { ClaimAdminScreen } from './auth/ClaimAdminScreen'
import { SetPasswordScreen } from './auth/SetPasswordScreen'
import { versionLabel } from './version'

export function App() {
  const { themeId, mode, toggleMode } = useTheme()
  const auth = useAuth()
  const shell = useShellState()
  const { module } = shell
  // Bumped when the shell's "+ DEAL" button is pressed; the CRM module opens its create form.
  const [newDealTick, setNewDealTick] = useState(0)
  // CRM pipeline view mode (Tabelle/Kanban/Funnel) — its switch lives in the module header,
  // next to the Pipeline tab, so it is only shown on that sub-view.
  const [pipelineMode, setPipelineMode] = useState<PipelineMode>('tabelle')
  const showPipelineSwitch = module.id === 'CRM' && shell.activeSubKey === 'pipeline'

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

      {auth.phase === 'needsBootstrap' && <ClaimAdminScreen />}

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
            onNew={module.id === 'CRM' ? () => setNewDealTick((t) => t + 1) : undefined}
            headerExtra={
              showPipelineSwitch ? (
                <div className="acme-subtabs">
                  {PIPELINE_MODES.map((m) => (
                    <button
                      key={m.key}
                      className={`acme-subtab${pipelineMode === m.key ? ' is-active' : ''}`}
                      onClick={() => setPipelineMode(m.key)}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              ) : undefined
            }
          />

          {shell.showKpis && <KpiBar kpis={module.kpis} />}

          {module.id === 'ADM' ? (
            <AdminModule subView={shell.activeSubKey} />
          ) : module.id === 'CRM' ? (
            <CrmModule
              subView={shell.activeSubKey}
              newDealTick={newDealTick}
              pipelineMode={pipelineMode}
            />
          ) : module.id === 'HR' ? (
            <HrModule subView={shell.activeSubKey} canWrite={auth.canWrite} />
          ) : (
            <ModulePlaceholder module={module} activeSub={shell.activeSub} />
          )}

          <Assist module={shell.activeMod} subView={shell.activeSubKey} />
        </>
      )}
    </div>
  )
}
