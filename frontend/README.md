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
- `src/modules/` — module content: `admin/` (ACMEbase), `crm/` (Pipeline), and
  `ModulePlaceholder` for the modules still to come
- `src/api/client.ts` — thin fetch wrapper (bearer auth, RFC-7807 errors)

## CRM — Pipeline (data mapping)

The hi-fi design shows a sales pipeline of *deals* moving through five stages
(`NEU · QUALIFIZIERT · ANGEBOT · VERHANDLUNG · GEWONNEN`). ACMEcrm has no "deal/stage"
entity — it models sales as `/customers`, `/products`, `/price-lists`, `/quotes` and
`/orders`. We therefore added a **Pipeline overlay** to the contract
([`../api/acme-crm.yaml`](../api/acme-crm.yaml), `Pipeline` tag, bumped to **v0.2.0**):
`GET /pipeline` projects leads/quotes/orders onto the five stages, and
`PATCH /pipeline/{id}` sets the stage (for board drag-drop / inline edit) plus the
sales-overlay attributes the transactional models lack (`owner`, `contact`, `stage`).
The status→stage derivation is documented on `GET /pipeline` in the spec.

Probability is derived from the stage (15/35/60/80/100). Stage and owner-avatar colors
are **theme-owned**: the views emit `data-stage` / `data-owner` attributes and
`themes/base/components.css` maps them to signal tokens — no colors in `src/`.

The three sub-views (Tabelle · Kanban · Funnel) live in `src/modules/crm/` and talk only
to `crmApi` → `/crm/pipeline`. Writes (`+ DEAL`, inline company/value edit, phase select,
Kanban drag-drop) are gated on `useAuth().canWrite` — **WATCH is read-only**.

> **Backend note:** the `/pipeline` projection is contract-first; the Java backend
> implementation is still pending, so the endpoint is currently mocked for review (a
> contract-shaped dev mock stands in for it). The existing `/crm/quotes|orders` endpoints
> are unchanged. This is the only mocked surface.

## Status

App shell + Admin + **CRM Pipeline** (Tabelle · Kanban · Funnel) complete and matched to
the hi-fi design: module switching with accent recolor, per-module sub-view tabs, KPI bar,
light/dark toggle. Theming is built in from the start — the look is fully decoupled into
`themes/` (two themes ship). Remaining module views are built next against the contracts.
