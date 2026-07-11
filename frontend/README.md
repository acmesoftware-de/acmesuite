# ACMEsuite Frontend

Web frontend for ACMEsuite — CRM, Supply, Build, HR and Admin on the shared ACMEbase
platform. Built against the contract-first REST API under [`../api/`](../api/); the app
integrates exclusively through the API, never against internal backend code.

## Stack

- **React 18 + TypeScript**, **Vite**
- **Theming**: the entire look (colors, fonts, geometry, component CSS) lives in
  [`themes/`](themes/), fully separated from `src/`. The app sets `data-theme`,
  `data-mode` and `data-module` on the root; the active theme's tokens do the rest.
  Ships two themes (`acme` default, `paper` demo). See [`themes/README.md`](themes/README.md).
- Self-hosted fonts (**no CDN**): bundled via `@fontsource`.

## Develop

```sh
npm install
npm run dev        # http://localhost:5273
```

In dev, `/api` is proxied to the local TLS edge (default `https://localhost:8543`).
Override with `ACMESUITE_API_ORIGIN`. See the repo root README for the backend stack.

```sh
npm run typecheck
npm run build
```

## Structure

- `themes/` — **the whole look**, separate from code: `base/components.css` (class
  contract) + one folder per theme (tokens, fonts, meta). See its README.
- `src/base.css` — structural reset only (no look)
- `src/theme/` — theme runtime: `ThemeProvider` / `useTheme`, types
- `src/modules/registry.ts` — single source of truth for module order, titles,
  sub-views and KPI tiles (content, not look — accents are theme-owned)
- `src/shell/` — app shell: `TopBar`, `ModuleHeader`, `KpiBar`, `LogoGlyph`,
  `useShellState` (emit class names only)
- `src/modules/` — module content (currently `ModulePlaceholder`; real views land here)
- `src/api/client.ts` — thin fetch wrapper (bearer auth, RFC-7807 errors)

## Status

App shell complete and matched to the hi-fi design: module switching with accent
recolor, per-module sub-view tabs, KPI bar (hidden on pure form views like
Supply → Import-Regeln), light/dark toggle. Theming is built in from the start — the
look is fully decoupled into `themes/` (two themes ship). Module views are built next,
one per module, against the API contracts.
