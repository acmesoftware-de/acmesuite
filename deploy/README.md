# ACMEsuite — local run stack (HTTPS)

Runs the ACMEsuite locally **over TLS** (project rule: everything via SSL/TLS) so that a client
in `mode=http` can test against it for real. The setup mirrors the surrounding platform stack:
DB + app (built from source) + **nginx TLS edge** (mkcert locally, Let's Encrypt in prod).

```
Client (mode=http) ──HTTPS──▶ nginx edge :8543 ──HTTP──▶ acmesuite-backend :8080 ──▶ acmesuite-db
```

## Once: certificates

```sh
mkcert -install                                  # mkcert root CA into the system trust store (once)
mkcert -cert-file deploy/certs/cert.pem -key-file deploy/certs/key.pem localhost 127.0.0.1 ::1
```

`deploy/certs/` is gitignored (no certificates in the repo).

## Start / stop

```sh
docker-compose -f deploy/docker-compose.local.yml up -d --build
#  -> ACMEsuite on https://localhost:8543

docker-compose -f deploy/docker-compose.local.yml down       # stop (DB volume is kept)
docker-compose -f deploy/docker-compose.local.yml down -v    # incl. DB volume
```

## Check

```sh
curl -fsS https://localhost:8543/api/org/persons | head    # persons (seeded)
curl -fsS https://localhost:8543/api/crm/products | head   # product catalog
curl -fsS https://localhost:8543/actuator/health           # {"status":"UP"}
curl -sI  http://localhost:8480/api/org/persons            # 301 -> https
```

## Ports

| Service | Host | Purpose |
|--------|------|-------|
| nginx edge | `8543` (https), `8480` (http→redirect) | TLS termination |
| Postgres | `5545` | own DB `acme_suite` (does not collide with `:5544`) |

A client points at this stack via `acme.twin.suite.base-url=https://localhost:8543` and
`acme.twin.suite.mode=http`.
