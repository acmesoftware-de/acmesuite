export type ThemeMode = 'dark' | 'light'

/**
 * Runtime metadata for a theme. A theme's *look* lives entirely in CSS under
 * `themes/<id>/` (tokens + component class rules, scoped by `[data-theme="<id>"]`).
 * This object only describes it to the app (e.g. a future theme picker) — it carries
 * no style values itself.
 */
export interface ThemeMeta {
  /** Matches the `data-theme` attribute and the `themes/<id>/` folder. */
  id: string
  /** Human-readable name for a picker. */
  label: string
  description?: string
  /** Which modes the theme's CSS provides. Every theme must supply at least 'dark'. */
  modes: ThemeMode[]
  /** Font families the theme uses (for docs / picker only; the CSS owns the truth). */
  fonts: { display: string; ui: string; mono: string }
}
