import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { THEMES, DEFAULT_THEME_ID } from '../../themes'
import type { ThemeMode } from './types'

interface ThemeContextValue {
  /** Active theme id (→ `data-theme` on the app root). */
  themeId: string
  setThemeId: (id: string) => void
  /** Active color mode (→ `data-mode`). */
  mode: ThemeMode
  setMode: (m: ThemeMode) => void
  toggleMode: () => void
  /** All registered themes (for a picker). */
  themes: typeof THEMES
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

const VALID_IDS = new Set(THEMES.map((t) => t.id))

/** Allow `?theme=<id>` to preselect a theme (handy for review/testing). */
function initialThemeId(): string {
  if (typeof window !== 'undefined') {
    const q = new URLSearchParams(window.location.search).get('theme')
    if (q && VALID_IDS.has(q)) return q
  }
  return DEFAULT_THEME_ID
}

function initialMode(): ThemeMode {
  if (typeof window !== 'undefined') {
    const q = new URLSearchParams(window.location.search).get('mode')
    if (q === 'light' || q === 'dark') return q
  }
  return 'dark'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [themeId, setThemeId] = useState<string>(initialThemeId)
  const [mode, setMode] = useState<ThemeMode>(initialMode)

  const toggleMode = useCallback(() => setMode((m) => (m === 'dark' ? 'light' : 'dark')), [])

  const value = useMemo<ThemeContextValue>(
    () => ({ themeId, setThemeId, mode, setMode, toggleMode, themes: THEMES }),
    [themeId, mode, toggleMode],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within <ThemeProvider>')
  return ctx
}
