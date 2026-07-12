# ADR-0010 — Versioned, tombstoned data (no hard deletes)

- Status: Accepted (2026-07-11)
- Scope: **all** persistent data across ACMEsuite — ACMEbase and every business module
- Related: ADR-0009 (search reacts to tombstones/versions), ADR-0007

## Context

We want data we can trust and reconstruct: never silently lose a prior state, and never
physically erase a record. This is the foundation for auditability, temporal / as-of queries,
undo-restore, and the central search index (which must react to a delete as a removal). A
principle stated once, up front, keeps every module consistent instead of each inventing its own
delete/history behaviour.

## Decision

Two rules for every entity, in every module:

1. **Deletion is a tombstone, never a hard delete.** Removing an entity sets a tombstone
   (`deleted_at`, `deleted_by`); the row stays. No `DELETE FROM` in business code.
2. **Every change is versioned.** Entities carry a monotonic `version` and audit stamps
   (`created_at`, `updated_at`, `created_by`, `updated_by`), and the full history of prior states
   is retained (append-only), so any past state is reconstructable / auditable.

Rules that follow:

- **Reads exclude tombstones by default** (a `deleted_at IS NULL` restriction); only explicit
  history/admin views see them.
- **Uniqueness is scoped to live rows** — unique constraints become partial
  (`... WHERE deleted_at IS NULL`), so an identifier can be reused once its holder is tombstoned.
- **Restore** clears the tombstone (a new version), it does not re-insert.
- **Search feed** (ADR-0009): a tombstone publishes `SearchDocumentRemoved`; any other change
  publishes `SearchDocumentChanged` for the current version.
- **Erasure / GDPR**: a legal erasure request conflicts with "no hard delete". Satisfy it by
  crypto-shredding (destroy the key for encrypted PII) or a narrowly-scoped, audited hard-erase
  procedure — the documented exception, never the default path.

## Mechanism (recommended, to finalize at implementation)

- A shared base mapping (`@MappedSuperclass`) contributing the audit + tombstone columns, with
  Hibernate `@SQLRestriction("deleted_at is null")` for default read filtering, and partial unique
  indexes created via Flyway.
- History via **Hibernate Envers** (`@Audited` → `*_AUD` + `REVINFO`; the DEL revision type is the
  tombstone in history), schema owned by Flyway. Alternatives considered: bi-temporal columns, or
  reconstructing history from the event outbox — decided per module when implemented.

## Consequences

- Storage grows (history retained) — acceptable; retention/archival is a later concern.
- **Retrofit**: existing hard deletes must become tombstones — e.g. `ProviderConfigService.delete`
  (today `repo.delete`) and the CRM/Supply "reset" delete-all endpoints. Existing base tables
  (`base_user`, `auth_provider_config`) adopt the audit/tombstone columns.
- Repositories and read models must be tombstone-aware; the search integration maps
  tombstone → removed, version change → changed.
- New modules build this in from the start (pulled into the in-flight module tracks).
