# ADR-0009 — Central full-text search (Lucene), fed by domain events

- Status: Accepted (2026-07-11)
- Scope: ACMEbase (a new `search` module), all business modules, the ⌘K search / ACMEassist
- Related: ADR-0007 (auth / roles), ADR-0008 (ACMEassist — a consumer of search)

## Context

Search must span all four modules (CRM, HR/People, Supply, Build) but be **one central,
clever** capability — a single ranked query, faceting, typo-tolerance, prefix (⌘K) — not four
per-module searches. A central index implies a central indexer whose truth is the module data.
The open question was **how to feed it**.

Constraints:
- Each module owns its schema (schema ownership) — the indexer must not read foreign tables.
- Search must respect authorization: a result is only returned if the caller may see it.
- The suite is a Spring Modulith monolith that already does cross-module work via **domain
  events** (the approval choreography: `CrmApprovalListener`, `HrApprovalListener`, …).
- No search hiccup may break a business write.

## Decision

**Central Lucene index in a new `search` module, fed asynchronously by domain events made
durable with the Spring Modulith event publication registry (transactional outbox).** The API
write only *publishes* an event; it never indexes inline.

1. **Feed = events, not inline-on-write.** Modules publish `SearchDocumentChanged(doc)` /
   `SearchDocumentRemoved(type,id)` (in `shared`) after a successful write. The `search` module's
   `@ApplicationModuleListener` consumes them after commit, off the request thread. Adding
   `spring-modulith-starter-jpa` turns these into a durable outbox: publications are persisted
   (`event_publication`, Flyway V23) and retried until acknowledged — no lost updates across a
   restart or a momentary index outage. Eventual consistency (ms–seconds) is fine for search.
2. **Contract in `shared`, index in `search`.** `SearchDocument` (owned/built by the module,
   which knows the data + its meaning) carries title/subtitle/body/keywords, a deep link, and
   **audiences** (who may see it). `search` owns the single Lucene `IndexWriter`, the query, and
   ranking — modules never depend on `search`.
3. **Truth via reindex.** A `SearchableProvider` per module streams all its documents (through its
   own read layer, not foreign schemas). The `search` module rebuilds the index from these on
   demand (`POST /api/search/reindex`, ADMIN) — first start, analyzer change, corruption. Same
   `SearchDocument` shape as the live feed.
4. **Permission-filtered queries.** `GET /api/search` filters to the caller's audiences (derived
   from the session token's role), so a result is never returned to someone who may not see it.

## Alternatives considered

- **Index synchronously in the API write path** (the intuitive option) — couples every write to a
  Lucene write: added latency, single-writer contention, and a search failure would cascade into
  the business write. Reindex would be a separate special case. Rejected.
- **CDC / read module schemas** — violates schema ownership. Rejected.
- **Per-module search** — no cross-module ranking/faceting; duplicated effort; not "central".
  Rejected.
- **A search engine service (Elasticsearch/OpenSearch/Solr)** — powerful, but an extra operational
  stack for a self-contained monolith. Lucene embedded is enough now; the `SearchIndex` seam keeps
  a future swap possible. Deferred.

## Consequences

Positive:
- One clever, central index; modules stay decoupled (they only publish events + provide a reindex
  stream) and IdP-/search-agnostic.
- Durable feed (outbox) — resilient to restarts and index downtime; the index is a rebuildable
  projection, never the system of record.
- Consistent with the existing event-driven choreography; permission-aware by construction.

Negative / follow-ups:
- Eventual consistency: a just-written entity is searchable a moment later.
- Every business module must publish `SearchDocumentChanged`/`Removed` and implement
  `SearchableProvider` — pulled into the four in-flight module tracks.
- Index storage: filesystem `IndexWriter` (single per JVM); the in-memory mode (`:memory:`) is for
  tests / ephemeral rebuild-from-source. Analyzers are language-agnostic (StandardAnalyzer) for
  now; per-language analyzers + synonyms are a later refinement.

## Where this lives (code)

- Contract: `shared/SearchDocument`, `SearchDocumentChanged`, `SearchDocumentRemoved`,
  `SearchableProvider`
- Index + query: `search/SearchIndex` (Lucene), `search/SearchService`, `search/web/SearchController`
- Feed: `search/SearchIndexListener` (`@ApplicationModuleListener`)
- Registry table: `V23__event_publication.sql`; config `acme.search.*`
