# ADR-0007 — Federated authentication, local authorization

- Status: Accepted (2026-07-10)
- Scope: ACMEbase (auth), the suite APIs, the Admin surface, the frontend sign-in
- Supersedes: the initial config-toggled HTTP Basic auth (in-memory demo users)
- Related: ADR-0005 (HR to Entra provisioning), ADR-0006 (API-only integration)

## Context

ACMEbase gates the suite APIs with three access roles — `WATCH` (read), `WORK`
(operational writes), `ADMIN` (master data + administration), hierarchy
`ADMIN > WORK > WATCH`.

The first implementation was config-toggled **HTTP Basic** with three in-memory demo
users (`watch`/`work`/`admin`, no-op passwords). This had several problems:

- The OpenAPI contract already declared `bearerAuth` (JWT) — a contract-vs-implementation
  mismatch.
- No real login, no session, and no way to manage users or assign roles.
- No path to the customer's identity provider (Entra / generic OIDC).

Constraints that shaped the decision:

- Customers run their own IdPs. We want real sign-in, but authorization must **not** depend
  on IdP group claims — those are fragile and customer-configuration-dependent.
- We need a break-glass path that works even when no external IdP is configured or reachable.
- Provider secrets (client secrets) must be stored safely, but ACMEsuite has no secret store
  (vault) yet.
- Existing tests and local development assume open APIs.

## Decision

Adopt **federated authentication, local authorization**.

1. **Split authn from authz.**
   - *Authentication* (who you are): local password, or a federated provider. Providers
     implement an `AuthProvider` SPI (Spring beans) and declare a `ConfigField` schema so the
     Admin UI renders their configuration generically. Ships: local, Microsoft Entra ID,
     generic OIDC.
   - *Authorization* (what you may do): the access role is **always assigned locally in Base**
     by an admin — never taken from an IdP claim.

2. **Base issues its own session JWT** (HS256) after a successful login, whether local or
   federated. Downstream module APIs validate only this Base token (its `role` claim) and never
   see the external IdP. This also resolves the prior `bearerAuth`/JWT contract mismatch.

3. **Local break-glass admin** is bootstrapped at first startup when no `ADMIN` exists. If
   `acme.base.auth.bootstrap.admin-password` is set (recommended for real deployments), that is
   the admin's password — nothing secret is logged. Otherwise a one-time random password is logged
   once and must be changed on first login (fine for local dev, but useless where the logs aren't
   readable).

4. **Identity linking**: a federated login maps its external subject (`oid`/`sub`) to a local
   user record; an unknown federated user is `PENDING` (no access) until an admin assigns a role.

5. **Provider secrets** are protected with **AES-256-GCM envelope encryption** in Base
   (`SecretCipher`), master key from configuration/environment. Secrets are write-only over the
   API — never returned; the UI only learns which secrets are set. The cipher sits behind a small
   interface so the master key can later be backed by a real secret store (OpenBao/KMS).

6. **Auth stays config-toggled** (`acme.base.auth.enabled`): default off = open (local dev and
   the existing test suite stay green); on = role rules enforced over the Base JWT.

## Alternatives considered

- **Keep HTTP Basic** — no real login/session, no user management, and the contract would have
  to regress to Basic. Rejected.
- **Full OIDC/Keycloak with roles from the IdP** — heaviest option; couples authorization to
  each customer's IdP configuration and offers no break-glass. Rejected as the authorization
  source (federated *authentication* is embraced via the provider plugins).
- **Store secrets in a vault (OpenBao) now** — most robust, but adds an operational stack the
  suite does not yet run. Deferred; the `SecretCipher` interface keeps this swappable.
- **Env-variable secret references only** — not editable in the Admin UI. Rejected.

## Consequences

Positive:

- Real sign-in with pluggable providers; roles are owned by the product, independent of the
  customer's IdP configuration.
- Modules remain IdP-agnostic — they only ever validate the Base token.
- The break-glass admin guarantees first-run access and survives an IdP outage/misconfiguration.
- Contract and implementation are aligned (bearer JWT), and the Admin can manage users, roles
  and provider configuration.

Negative / follow-ups:

- The actual OIDC redirect/callback login flow for Entra and generic OIDC is **not yet wired** —
  provider registration, config schema and encrypted storage exist; the login mechanics are a
  follow-up.
- Envelope encryption is only as strong as master-key handling. Production must supply a real key
  (environment/KMS) and should migrate to a secret store.
- HS256 uses a shared symmetric secret. If issuing and validating ever split across services,
  move to an asymmetric algorithm (RS256).

## Where this lives (code)

- SPI and providers: `base/auth/AuthProvider`, `LocalAuthProvider`, `EntraAuthProvider`,
  `OidcAuthProvider`
- Token: `base/token/SessionTokenService`; validation in `base/BaseSecurityConfig`
- Users and roles: `base/domain/BaseUser`, `base/UserAdminService`,
  `base/BootstrapAdminInitializer`
- Crypto and provider config: `base/crypto/SecretCipher`, `base/auth/ProviderConfigService`
- Contract: `api/acme-base.yaml` (v1.1.0)
- Schema: `V21__base_auth.sql`, `V22__auth_provider_config.sql`
- Frontend: `frontend/src/auth/*`, `frontend/src/modules/admin/*`
