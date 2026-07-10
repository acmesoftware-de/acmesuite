# Theming

The **entire look** of the ACMEsuite frontend — colors, fonts, geometry, and the CSS
rules for every component — lives here in `themes/`, fully separated from the app code
in `src/`. The code emits only stable class names and three attributes; a theme turns
those into a complete visual design.

Switching or adding a theme requires **no changes to `src/`**.

## How it works

The app root (`<div class="acme-app">`, rendered by [`src/App.tsx`](../src/App.tsx))
carries three attributes — this is the whole theming interface:

| Attribute      | Set from                    | Meaning                                  |
| -------------- | --------------------------- | ---------------------------------------- |
| `data-theme`   | `ThemeProvider` (theme id)  | which theme's tokens/look apply          |
| `data-mode`    | `ThemeProvider` (`dark`/`light`) | color mode within the theme         |
| `data-module`  | active module (`CRM`…`ADM`) | drives the active `--accent`             |

Everything visual is expressed as **CSS custom properties (design tokens)**:

```
themes/
  brand.css          # immutable ACMEsoftware brand constants (the Bar-Mark colors)
  base/
    components.css   # the class contract: every component rule, using ONLY var(--token)
  acme/              # default theme
    tokens.css       # the --token values (dark + light), fonts, accents, geometry
    fonts.css        # @fontsource imports (bundled, no CDN)
    meta.ts          # ThemeMeta (id, label, modes, fonts) for the registry/picker
  paper/             # proof-of-concept alternate theme (same files)
  index.ts           # imports base + every theme; exports THEMES + DEFAULT_THEME_ID
```

- **`base/components.css`** implements the look of each component using only tokens
  (`var(--panel)`, `var(--accent)`, `var(--font-display)`, `var(--radius)`, …). It has
  no hard-coded colors, fonts, or radii. It lives in the theme layer, not `src/`, so
  the look stays out of the code.
- Each **theme** supplies the token *values* under `[data-theme="<id>"]` (and a
  `[data-theme="<id>"][data-mode="light"]` block for light mode). That's usually all a
  theme needs — colors, fonts, and geometry all flow from these variables.
- The **module → accent** mapping is shared in `base` (`[data-module="CRM"] {
  --accent: var(--accent-crm) }` …); each theme only defines the `--accent-*` values.

Because themes are scoped by `[data-theme="<id>"]`, they all bundle together and
coexist; the active one is chosen by flipping the root attribute at runtime.

### Brand mark (not themeable)

The logo is the **Bar-Mark** — four ascending bars in a fixed color sequence
(Red · Yellow · Blue · Green), per [brand.acmesoftware.de](https://brand.acmesoftware.de).
Brand policy requires the sequence to stay unchanged, so its colors live in
`themes/brand.css` as `--mark-*` constants shared by **all** themes — they are not theme
tokens. The default `acme` theme derives its module accents from these marks; other
themes may use a different accent palette but keep the same brand logo.

## The token contract

A theme must define these variables under `[data-theme="<id>"]`:

**Surfaces / text** (flip in the `[data-mode="light"]` block):
`--bg --panel --panel2 --line --line2 --ink --dim --faint --chip --shadow`

**Semantic signal colors** (mode-independent):
`--success --warning --danger --info --neutral`

**Module accents** (brand):
`--accent-crm --accent-sup --accent-bld --accent-hr --accent-adm`

**Typography** (families only; sizes are structural and live in `base`):
`--font-display --font-ui --font-mono`

**Geometry**: `--radius` (corner rounding — `0` = squared), `--edge` (accent edge width)

`--accent` and `--on-accent` are resolved automatically from `data-module` by `base`.
A theme may override `--on-accent` for a specific module if its accent needs a
different foreground (see Paper's HR amber → white).

## The class contract

`src/` emits these class names; `base/components.css` styles them. Treat the set as the
interface between code and themes:

`acme-app` · `acme-topbar` · `acme-logo` / `acme-logo-bar--{crm,hr,bld,sup}` ·
`acme-brand` / `acme-brand-dim` · `acme-divider` · `acme-modtabs` / `acme-modtab`
(`is-active`) / `acme-modtab-label` / `acme-modtab-underline` · `acme-spacer` ·
`acme-search` / `acme-kbd` · `acme-theme-toggle` · `acme-avatar` · `acme-modhead` /
`acme-eyebrow` / `acme-title` · `acme-subtabs` / `acme-subtab` (`is-active`) ·
`acme-btn-new` · `acme-kpis` / `acme-kpi` / `acme-kpi-label` / `acme-kpi-row` /
`acme-kpi-value` / `acme-kpi-delta` (`is-up` / `is-down`) · `acme-content` /
`acme-placeholder` / `acme-placeholder-title` / `acme-placeholder-desc`

New components add a class here and a rule in `base/components.css`.

## Fonts — NO CDN

Fonts are **self-hosted and bundled** via [`@fontsource`](https://fontsource.org)
npm packages. Nothing is loaded from a CDN at runtime — this is a hard policy.

A theme references families through `--font-*`; the actual `@font-face` data comes from
that theme's `fonts.css`. To use a family not already bundled:

```sh
npm i @fontsource/<family>
```

then `@import '@fontsource/<family>/<weight>.css'` in your theme's `fonts.css` and point
`--font-*` at it. Duplicate imports across themes are de-duplicated at build time.

## Add a theme

1. `mkdir themes/<id>` and add `tokens.css`, `fonts.css`, `meta.ts` (copy Paper's).
2. Fill in the token values under `[data-theme="<id>"]` (+ the light block).
3. Register it in `themes/index.ts`: `import './<id>/tokens.css'` and add its meta to
   `THEMES`.
4. Done — no `src/` changes. Preview it at `?theme=<id>` (and `?mode=light`).

To make it the default, set `DEFAULT_THEME_ID` in `themes/index.ts`.

## Switching at runtime

`ThemeProvider` ([`src/theme/ThemeProvider.tsx`](../src/theme/ThemeProvider.tsx)) holds
the active `themeId` and `mode` and exposes `useTheme()` (`{ themeId, setThemeId, mode,
setMode, toggleMode, themes }`). Wire `setThemeId` to a picker built from `themes` to
let users switch. `?theme=` / `?mode=` URL params preselect for review and testing.
