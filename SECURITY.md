# Security Policy

Thank you for helping keep **ACMEsuite** secure.

## Supported versions

ACMEsuite is pre-1.0 (`0.x`). Security fixes land on the `main` branch; there are currently no
backports to older tags.

| Version | Supported |
|--------|:---------:|
| `main` (0.x) | ✅ |
| older tags   | ❌ |

## Reporting a vulnerability

**Please do not report security issues through public GitHub issues, pull requests, or discussions.**

Use one of the two confidential channels:

1. **Preferred — GitHub Private Vulnerability Reporting:** the *Security → Report a vulnerability* tab
   of the repo (<https://github.com/acmesoftware-de/acmesuite/security/advisories/new>). The exchange
   stays private until a fix is published.
2. **Email:** <security@acmesoftware.de> — PGP-encrypted welcome (key on request).

Please give us time to respond before disclosing details publicly (coordinated disclosure).

### What to include

- Affected component/endpoint and version/commit (`main` SHA)
- Reproduction steps or a proof of concept
- Impact assessment (e.g. auth bypass, data exposure, RCE)
- Optional: a suggested fix

### What to expect

- **Acknowledgement** within **3 business days**
- **Initial assessment** (confirmation/severity) within **10 business days**
- A disclosure timeline agreed together; credit in the advisory on request

## Scope

ACMEsuite is a **reference/demo application** for a fictional company. The seed catalog (people,
customers, email addresses) is **entirely fictional** and does not represent real data. The bundled
development defaults (`application.yml`: local DB credentials, `acme.base.auth.enabled=false`) are
**for local development only** and are meant to be hardened for production — that is not a reportable
finding.

## How we secure the CI

Every push to `main` and every pull request runs:

- **SBOM (CycloneDX)** from the Maven build → pushed to **Dependency-Track 5**
- **Trivy gates** on the dependencies (from the SBOM) and the container image
  (fails on `CRITICAL`/`HIGH`)

See [`.github/workflows/security.yml`](.github/workflows/security.yml).

> **For maintainers:** also recommended under *Settings → Code security*: enable *Private
> vulnerability reporting*, *Dependabot alerts/updates*, and *Secret scanning* (incl. push protection).
