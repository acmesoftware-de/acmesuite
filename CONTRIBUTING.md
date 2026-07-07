# Contributing to ACMEsuite

Thanks for your interest in contributing! ACMEsuite is a self-contained business suite —
**ACMEcrm/HR/Supply/Build** on the **ACMEbase** foundation. This document summarizes how to build
locally, which conventions apply, and how a contribution lands.

## Requirements

- **JDK 25** (`brew install openjdk@25`, then `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`)
- A **container runtime** (Docker Desktop or colima) — the integration tests start a real PostgreSQL
  via **Testcontainers**.
- **Node 20+** only if you work on the API contracts / the portal (Redocly).

## Build & test

```sh
mvn verify          # compile, Modulith verification, integration tests (Testcontainers/Postgres)
```

For the API contracts under [`api/`](api/):

```sh
npx @redocly/cli lint                       # all four module specs (config: redocly.yaml)
npx @redocly/cli build-docs api/acme-hr.yaml -o hr.html
```

## Architecture guardrails

- **Modular monolith (Spring Modulith).** Module boundaries are enforced by `ModularityTests` — if
  they turn red, an illegal dependency between modules was introduced. No module reaches past another
  module's public API.
- **Self-contained.** ACMEsuite is standalone: consumers talk to the suite **exclusively through its
  REST API**. Outbound clients / glue code into third-party systems do **not** belong here — they go
  into the respective connector/plugin repository.
- **Contract-first.** API changes go into the OpenAPI spec first (`api/*.yaml`), then the controller —
  the spec is the source of truth. `redocly lint` must be green.
- **Schema ownership via Flyway.** Ship schema changes as a **new** migration
  (`src/main/resources/db/migration/VN__…​.sql`); do not edit already-published migrations. Hibernate
  only validates (`ddl-auto: validate`).

## Code style

- Write code that reads like its surroundings — same naming, comment density, and idioms.
- **English** Javadoc/comments; English technical identifiers.
- No internal product codenames in code or docs.

## Commits & branches

- **Conventional Commits:** `feat(...)`, `fix(...)`, `chore(...)`, `refactor(...)`, `docs(...)`.
- **Descriptive, topic-based branch names** (e.g. `feat/order-partial-fulfilment`,
  `fix/absence-overlap`) — no generated random names.
- Small, focused commits; one logical change per PR.

## Pull request flow

1. Fork/branch from `main`.
2. Make the change with tests; `mvn verify` green locally.
3. Open a PR against `main`. CI must be green:
   - **CI** ([`ci.yml`](.github/workflows/ci.yml)) — `mvn verify`
   - **Security** ([`security.yml`](.github/workflows/security.yml)) — SBOM/DT5 + Trivy gates
   - **API portal** ([`api-docs.yml`](.github/workflows/api-docs.yml)) — Redocly lint (on contract changes)
4. Describe the *why*, not just the *what*; link affected issues.

## License of contributions

By contributing, you agree that your contribution is licensed under the project's **Apache License 2.0**
(inbound = outbound, see [`LICENSE`](LICENSE)).

## Security issues

Please do **not** report them as public issues — see [`SECURITY.md`](SECURITY.md).
