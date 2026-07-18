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

## CRM — Pipeline · Kunden · Kontakte (data mapping)

The CRM module has three header sub-views:

- **Pipeline** — the sales board with a secondary Tabelle · Kanban · Funnel switch.
- **Kunden** — companies (`/customers`), master-detail: a company's contacts, deals and
  mail threads on the right.
- **Kontakte** — people (`/contacts`), master-detail: a contact's deals and mail threads.

The hi-fi design shows a pipeline of *deals* moving through five stages
(`NEU · QUALIFIZIERT · ANGEBOT · VERHANDLUNG · GEWONNEN`) with a flat `company` string.
ACMEcrm has no "deal/stage" entity and no contacts/mail — it models sales as
`/customers`, `/products`, `/price-lists`, `/quotes`, `/orders`. We therefore extended the
contract ([`../api/acme-crm.yaml`](../api/acme-crm.yaml), now **v0.3.0**):

- **Pipeline overlay** (`Pipeline` tag) — `GET /pipeline` projects leads/quotes/orders onto
  the five stages; `PATCH /pipeline/{id}` sets the stage (board drag-drop / inline edit) plus
  the sales attributes the transactional models lack (`owner`, `contact`, `stage`). The
  status→stage derivation is documented on `GET /pipeline`.
- **Contacts** (`/contacts`) — people at a customer; a `Deal` now carries `customerId` **and**
  `contactId`, so company/contact detail views list their opportunities via
  `/pipeline?customerId=…`/`?contactId=…`.
- **Mail** (`/threads`) — correspondence attached to a customer/contact
  (`/threads?customerId=…`/`?contactId=…`).

Probability is derived from the stage (15/35/60/80/100). Stage / owner-avatar / status colors
are **theme-owned**: the views emit `data-stage` / `data-owner` / `data-status` attributes and
`themes/base/components.css` maps them to signal tokens — no colors in `src/`.

Everything lives in `src/modules/crm/` and talks only to `crmApi` (`/crm/pipeline`,
`/crm/customers`, `/crm/contacts`, `/crm/threads`). Writes (`+ DEAL` / `+ KUNDE` / `+ KONTAKT`,
inline edit, phase select, Kanban drag-drop) are gated on `useAuth().canWrite` — **WATCH is
read-only**.

Web forms / newsletter signups are configured in Admin (`/forms`, fields + trigger actions)
and captured via the public `POST /forms/{id}/submit` tool — see the `Forms` tag in the
contract. The Admin form-builder UI and the public form renderer are separate follow-ups.

> **Backend note:** `/pipeline`, `/contacts` and `/threads` are now implemented in the Java
> backend (`crm/PipelineService`, `crm/MailService`, `crm` contacts + Flyway `V28`), so the CRM
> module runs against the real API. Only `/forms` is still contract-first (pending backend,
> pairs with the Admin form-builder). `/crm/customers|quotes|orders` are unchanged.

## Status

App shell + Admin + **CRM** (Pipeline · Kunden · Kontakte, with deals/mail per company &
contact) complete and matched to the hi-fi design system: module switching with accent
recolor, per-module sub-view tabs, KPI bar, light/dark toggle. Theming is built in from the
start — the look is fully decoupled into `themes/` (two themes ship). Remaining module views
are built next against the contracts.
