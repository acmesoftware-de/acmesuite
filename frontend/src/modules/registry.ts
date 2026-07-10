// Single source of truth for the app shell: module order, accent colors, titles,
// sub-views and KPI tiles — transcribed verbatim from the hi-fi design prototype.
// Module content components plug into this later; the shell renders entirely from here.

export type ModuleId = 'CRM' | 'SUP' | 'BLD' | 'HR' | 'ADM'

export interface SubView {
  /** Stable key used in state and (later) routing. */
  key: string
  /** Uppercase tab label (Space Mono). */
  label: string
  /** Hide the KPI bar on pure form/board views (e.g. Supply → Import-Regeln). */
  hideKpis?: boolean
  /** Override the module's "+ NEU" button label on this view (e.g. HR → + BEWERBER). */
  newLabel?: string
}

export interface Kpi {
  label: string
  value: string
  delta: string
  /** true → positive (green delta), false → negative (red delta). */
  up: boolean
}

export interface ModuleDef {
  id: ModuleId
  /** Top-bar tab label. */
  name: string
  /** Small mono eyebrow above the title. */
  eyebrow: string
  /** Large display page title. */
  title: string
  /** "+ NEU" button label (may be overridden per sub-view). */
  newLabel: string
  /** Sub-view tabs shown at the right of the module header. Empty → no tabs. */
  subViews: SubView[]
  kpis: Kpi[]
}

// Note: the per-module accent color is NOT here — it is theme-owned. The shell sets
// `data-module` on the root and the active theme maps it to --accent (see
// themes/base/components.css and each theme's --accent-* tokens).

const D = { pos: true, neg: false }

export const MODULES: ModuleDef[] = [
  {
    id: 'CRM',
    name: 'CRM',
    eyebrow: 'MODUL 01 · VERTRIEB',
    title: 'Pipeline',
    newLabel: '+ DEAL',
    subViews: [
      { key: 'tabelle', label: 'TABELLE' },
      { key: 'kanban', label: 'KANBAN' },
      { key: 'funnel', label: 'FUNNEL' },
    ],
    kpis: [
      { label: 'PIPELINE', value: '€945k', delta: '+8,2%', up: D.pos },
      { label: 'OFFENE DEALS', value: '11', delta: '+3', up: D.pos },
      { label: 'WIN-RATE', value: '38%', delta: '−2%', up: D.neg },
      { label: 'Ø ZYKLUS', value: '21 T', delta: '−4 T', up: D.pos },
    ],
  },
  {
    id: 'SUP',
    name: 'Supply',
    eyebrow: 'MODUL 04 · SUPPLY CHAIN',
    title: 'Bestände',
    newLabel: '+ BESTELLUNG',
    subViews: [
      { key: 'bestand', label: 'BESTÄNDE' },
      { key: 'lieferanten', label: 'LIEFERANTEN' },
      { key: 'restriktion', label: 'IMPORT-REGELN', hideKpis: true },
    ],
    kpis: [
      { label: 'LAGERWERT', value: '€2,4 Mio', delta: '+1,1%', up: D.pos },
      { label: 'KRITISCHE SKUS', value: '4', delta: '+2', up: D.neg },
      { label: 'OFFENE BESTELLUNGEN', value: '12', delta: '−1', up: D.pos },
      { label: 'LIEFERTREUE', value: '96%', delta: '+0,4%', up: D.pos },
    ],
  },
  {
    id: 'BLD',
    name: 'Build',
    eyebrow: 'MODUL 03 · PRODUKTION',
    title: 'Fertigung',
    newLabel: '+ AUFTRAG',
    subViews: [
      { key: 'machbarkeit', label: 'MACHBARKEIT' },
      { key: 'schichten', label: 'SCHICHTEN' },
      { key: 'produkte', label: 'PRODUKTE' },
      { key: 'auftraege', label: 'AUFTRÄGE' },
      { key: 'maschinen', label: 'MASCHINEN' },
    ],
    kpis: [
      { label: 'AUSLASTUNG', value: '87%', delta: '+5%', up: D.pos },
      { label: 'AUFTRÄGE HEUTE', value: '10', delta: '+2', up: D.pos },
      { label: 'AUSSCHUSS', value: '1,8%', delta: '−0,3%', up: D.pos },
      { label: 'DURCHLAUFZEIT', value: '3,2 T', delta: '−0,4 T', up: D.pos },
    ],
  },
  {
    id: 'HR',
    name: 'HR',
    eyebrow: 'MODUL 02 · PERSONAL',
    title: 'Team',
    newLabel: '+ MITARBEITER',
    subViews: [
      { key: 'team', label: 'TEAM' },
      { key: 'orgchart', label: 'ORG-CHART' },
      { key: 'bewerber', label: 'BEWERBER', newLabel: '+ BEWERBER' },
    ],
    kpis: [
      { label: 'MITARBEITENDE', value: '148', delta: '+6', up: D.pos },
      { label: 'OFFENE STELLEN', value: '7', delta: '+2', up: D.neg },
      { label: 'ABWESEND HEUTE', value: '9', delta: '—', up: D.pos },
      { label: 'FLUKTUATION', value: '4,2%', delta: '−0,5%', up: D.pos },
    ],
  },
  {
    id: 'ADM',
    name: 'Admin',
    eyebrow: 'MODUL 05 · VERWALTUNG',
    title: 'System',
    newLabel: '+ NUTZER',
    subViews: [],
    kpis: [
      { label: 'NUTZER', value: '152', delta: '+4', up: D.pos },
      { label: 'ROLLEN', value: '8', delta: '0', up: D.pos },
      { label: 'AKTIVE SESSIONS', value: '37', delta: '+9', up: D.pos },
      { label: 'LIZENZEN', value: '160', delta: '8 frei', up: D.pos },
    ],
  },
]

export const MODULE_BY_ID: Record<ModuleId, ModuleDef> = Object.fromEntries(
  MODULES.map((m) => [m.id, m]),
) as Record<ModuleId, ModuleDef>
