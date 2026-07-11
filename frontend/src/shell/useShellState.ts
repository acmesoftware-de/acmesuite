import { useCallback, useMemo, useState } from 'react'
import { MODULE_BY_ID, MODULES, type ModuleId, type SubView } from '../modules/registry'

/** Each module remembers its own last sub-view (per the handoff interaction spec). */
type SubViewByModule = Record<ModuleId, string>

const initialSubViews: SubViewByModule = Object.fromEntries(
  MODULES.map((m) => [m.id, m.subViews[0]?.key ?? '']),
) as SubViewByModule

/** Module navigation + per-module sub-view state. Theme/mode live in ThemeProvider. */
export function useShellState() {
  const [activeMod, setActiveMod] = useState<ModuleId>('CRM')
  const [subViews, setSubViews] = useState<SubViewByModule>(initialSubViews)

  const module = MODULE_BY_ID[activeMod]
  const activeSubKey = subViews[activeMod]
  const activeSub: SubView | undefined = useMemo(
    () => module.subViews.find((s) => s.key === activeSubKey),
    [module, activeSubKey],
  )

  const setSubView = useCallback(
    (key: string) => setSubViews((prev) => ({ ...prev, [activeMod]: key })),
    [activeMod],
  )

  const showKpis = module.kpis.length > 0 && !activeSub?.hideKpis
  const newLabel = activeSub?.newLabel ?? module.newLabel

  return {
    activeMod,
    setActiveMod,
    module,
    activeSub,
    activeSubKey,
    setSubView,
    showKpis,
    newLabel,
  }
}

export type ShellState = ReturnType<typeof useShellState>
