# ACMEsuite

**ACMEsuite** is a lightweight, enterprise-ready business suite: **ACMEcrm** (sales), **ACMEhr**
(people), **ACMEsupply** (procurement) and **ACMEbuild** (production) on a shared platform
foundation — **ACMEbase** (database, authentication, role-based access control).

It is a self-contained system with a clean, contract-first REST API. Any consumer — another
service, an automation, a plugin, or an external system — integrates **exclusively through the
API**, never against internal code.

## Stack

- **Java 25**, **Spring Boot 4.1**, **Spring Modulith 2.1** (modular monolith)
- **PostgreSQL** + **Flyway** (each module owns its schema)
- **Testcontainers** for integration tests

## Modules

| Module   | Responsibility |
|----------|----------------|
| `base`   | **ACMEbase**: authentication (Spring Security, config-toggled) + roles `WATCH`/`WORK`/`ADMIN` |
| `shared` | Shared value types (`Money`, `DateRange`, `Rng`) |
| `org`/`hr` | People, org hierarchy, powers of attorney, absences, approval limits |
| `crm`    | Customers, products, price lists, quotes, orders (production contracts) |
| `supply` | Suppliers, materials, supply contracts, procurement, stock |
| `build`  | Products, bills of materials (BOM), production planning, feasibility |
| `process`| Approval routing and contract choreography (who signs, limits, powers of attorney) |

API contracts (OpenAPI) live under [`api/`](api/); the rendered portal is published to
`https://api.acmesoftware.de`.

## Requirements

- JDK 25 (`brew install openjdk@25`), then `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`
- A container runtime (Docker Desktop or colima)

## Build & test

```sh
mvn test          # Modulith verification + integration tests against a real Postgres (Testcontainers)
```

## Run locally (HTTPS)

Everything is served over TLS. The local stack (Postgres + app + an nginx TLS edge with mkcert
certificates) mirrors production, where TLS is terminated at the edge (Let's Encrypt).

```sh
# one-time: trust the local CA and create certificates
mkcert -install
mkcert -cert-file deploy/certs/cert.pem -key-file deploy/certs/key.pem localhost 127.0.0.1 ::1

docker-compose -f deploy/docker-compose.local.yml up -d --build   # → https://localhost:8543
```

See [`deploy/README.md`](deploy/README.md) for details.

## Roles (ACMEbase)

`WATCH` reads · `WORK` performs operational writes · `ADMIN` maintains master data. Authentication
is disabled by default (`acme.base.auth.enabled=false`, open for development); set it to `true` to
enforce the role rules over HTTP Basic.
