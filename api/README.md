# ACMEsuite — API Contracts

> **Home of the contracts.** These contracts live here in the **ACMEsuite** repo
> (`github.com/acmesoftware-de/acmesuite`). They are built by CI into a static
> API portal and published at **<https://api.acmesoftware.de>** — see
> [`.github/workflows/api-docs.yml`](../.github/workflows/api-docs.yml).

Contract-first OpenAPI contracts of the four **ACMEsuite** systems. The ACMEsuite is a standalone
application (on **ACMEbase**: DB + auth + roles); **clients consume it exclusively
through this API** (ADR-0006). Each system is its own bounded context with its own schema and
REST API. These contracts are the **source of truth** — the hand-written controllers are
validated against them.

| File | System | Content | Status |
|------|--------|--------|--------|
| [`acme-hr.yaml`](acme-hr.yaml) | **ACMEhr** | Employees (title/role/hierarchy), vacation/absence planning, deputy/assistant, sick days, powers of attorney, approval limits | **Contract v1.0.0** |
| [`acme-crm.yaml`](acme-crm.yaml) | **ACMEcrm** | Customers/resellers, products, price lists (+tiers), quotes, orders (→ e-approval) | **Contract v1.0.0** |
| [`acme-build.yaml`](acme-build.yaml) | **ACMEbuild** (formerly ACMEprod) | Products + bills of materials (BOM), production capacity/planning, material demand projection, **feasibility API** (can a contract be fulfilled → scarce resource) | **Contract v1.0.0** (template) |
| [`acme-supply.yaml`](acme-supply.yaml) | **ACMEsupply** | Suppliers, raw materials/energy, supply contracts (prices/tiers/lead times), procurements (→ e-approval) | **Contract v1.0.0** |

## Shared building blocks — [`acme-base.yaml`](acme-base.yaml)

Value types (`Money`, `DateRange`), the RFC-7807 `Problem`, the generic responses
(`Unauthorized`/`NotFound`/`Unprocessable`) and the e-approval types (`Approval`/`ApprovalDecision`)
live **once** in `acme-base.yaml` (foundation *ACMEbase*); the four module specs reference them
via cross-file `$ref` (`acme-base.yaml#/components/...`). The `bearerAuth` scheme additionally stays
local in each spec (OpenAPI 3.0 does not resolve `security` requirements cross-file).

Produce a self-contained spec: `npx @redocly/cli bundle api/acme-crm.yaml -o crm.bundled.yaml`.
CI bundles all four module specs and publishes them under `https://api.acmesoftware.de/specs/`.

## Conventions

- **OpenAPI 3.0.3**, natural string keys as IDs, money as `Money`, periods as `DateRange`
  — shared from [`acme-base.yaml`](acme-base.yaml).
- Errors per **RFC 7807** (`application/problem+json`).
- **Auth (ACMEbase):** `bearerAuth` (JWT); the role required per operation as `x-required-role`
  (`WATCH` reads · `WORK` writes operationally · `ADMIN` maintains master data; hierarchy ADMIN > WORK > WATCH).
  Secured operations respond `401` (`components/responses/Unauthorized`).
- ACMEhr **evolves** the existing `org` domain model (Person · OrgUnit · PowerOfAttorney · Absence · Role)
  and adds write/planning operations; the read-only `/api/org` browsing remains in place.
- This data feeds the **e-approval** logic (who may sign off on which amount, who deputizes during an absence)
  and clients (who is on vacation/sick on day N).

## Lint & portal (local)

```sh
# Lint all specs (config: ../redocly.yaml)
npx @redocly/cli lint

# Build the portal locally (one Redoc HTML per module spec)
npx @redocly/cli build-docs api/acme-hr.yaml -o hr.html
```

In CI this is handled by [`.github/workflows/api-docs.yml`](../.github/workflows/api-docs.yml):
lint → one Redoc page per module spec → landing page → deploy to GitHub Pages
(`api.acmesoftware.de`, HTTPS enforced).
