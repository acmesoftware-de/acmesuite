# ADR-0008 · Phase-1 implementation plan

- Status: **Proposed** (2026-07-12) — companion to [ADR-0008](ADR-0008-acmeassist-copilot.md)
- Scope: the **"ask the current module"** slice — read-only, streamed, grounded Q&A, with the
  **Customer-360 briefer** ([Appendix A](ADR-0008-acmeassist-agents.md)) as the reference agent.
- Reflects the review decisions (bilingual DE/EN · two-tier CPU routing, no GPU · persist
  conversations · loopback dispatch · generated+curated tools · governance G1/G2/G4).

> **What phase 1 proves:** the whole spine — Spring AI + langgraph4j engine, SSE streaming,
> tool-use over the REST contracts, and **execute-as-the-user** authorization — with **no writes**
> (no confirmation flow, no `assist_draft` yet). Everything mutating is deferred to phase 3.

## 0. Prerequisite spikes (before committing the module)

1. **Spring AI × Spring Boot 4.1 compatibility** — confirm a Spring AI release that runs on the
   Boot 4.1 / Java 25 line; pick the BOM version. Blocker if incompatible.
2. **langgraph4j** — validate the `StateGraph` + Spring AI `ChatModel` integration on a throwaway
   ReAct loop with one Ollama tool call. Confirms tool-calling round-trips.
3. **Ollama tool-calling** — pull `qwen2.5:7b`, verify a real function call returns structured
   args; measure warm/cold latency on the target CPU (sizes the hardware floor).

> **Spike outcome (2026-07-12).** Boot parent is **4.1.0** (Spring Framework 7). The Spring AI BOM
> resolves from Maven Central, but Spring AI 1.x targets Framework 6 / Boot 3 — running its
> autoconfiguration on Boot 4.1 is unverified and, if incompatible, would fail the whole app
> context (every `@SpringBootTest`). langgraph4j Boot-4 support is likewise unconfirmed. **Decision:
> M3 ships an interim `provider=ollama` engine that talks to Ollama directly (Spring `RestClient`
> → `/api/chat`) with a hand-rolled ReAct loop over `AuthenticatedApiDispatcher`** — no heavy new
> deps, so the shared build is safe. The `AssistantEngine` port is unchanged, so swapping to
> Spring AI + langgraph4j once they support Boot 4.1 is a drop-in.
>
> **Spike #3 — part A done (local `qwen2.5:7b` on Apple M4 Pro, 2026-07-12).** Wire format,
> tool-calling and grounding **validated end to end**: the model emits `tool_calls` in the shape
> `RestClientOllamaChat` parses, we feed the tool result back, and it produces a correct grounded
> German answer. Latency (Metal, not the CPU target): **~36 s cold, ~1–4 s warm** → keep-warm is
> essential. **Reliability finding:** in ~25–30 % of warm turns the model leaked the tool call as
> *text* instead of a structured `tool_calls` object, so our engine mis-read it as the final answer
> (garbage output). **Hardening item (M3.1) — implemented & verified (2026-07-12):** `ToolCallRecovery`
> re-routes JSON-tool-call-shaped content, plus a prompt nudge; after the fix, 6/6 warm re-runs
> produced grounded answers with no garbage.
>
> **Spike #3 — part B done (Hetzner CCX33, AMD EPYC-Milan, 8 dedicated vCPU, no AVX-512/AMX,
> CPU-only, 2026-07-12).** Warm single-tool turn (2 LLM calls): `qwen2.5:7b` **~7.3–7.5 s**,
> `granite4:3b` **~6.4–7.4 s**; cold **~18–20 s** (≈3× warm → **keep-warm essential**). Grounding +
> tool-calling reliable, M3.1 held (no garbage). **Finding:** the **two-tier speedup is modest** on
> this CPU class — the 3B was barely faster for short turns (prefill-bound; Granite also generated
> longer answers), so treat two-tier as *optional*, not assumed. **Conservative floor:** Milan has
> no AVX-512/AMX; Zen4/Genoa (AVX-512) or Intel Sapphire Rapids (AMX) would be materially faster.
> **Hardware floor:** ≥8 modern vCPU + 16–32 GB with keep-warm; prefer AVX-512/AMX (or a small GPU)
> for snappy interactive multi-step turns. Extrapolated: a 3–5-step turn ≈ 15–40 s warm on this box
> → simple turns usable, heavy multi-step is async territory (matches Decision 4).

## 1. Backend module layout

New Spring Modulith module `de.acmesoftware.acmesuite.assist`:

```
assist/
  AssistProperties.java           # @ConfigurationProperties("acme.assist")
  AssistantEngine.java            # port: startTurn(...) -> stream of AssistEvent
  StubAssistantEngine.java        # deterministic; default (acme.assist.provider=stub)
  spring/
    SpringAiAssistantEngine.java  # provider=ollama|claude|…  (ChatModel + langgraph4j graph)
    AssistGraph.java              # StateGraph: model <-> tools, guard maxIterations
  tools/
    AssistTool.java               # tool descriptor (name, schema, GET op, required role)
    OpenApiToolFactory.java       # generate tool schemas from api/acme-*.yaml
    AuthenticatedApiDispatcher.java  # loopback HTTP as the user (forwards the Base JWT)
  agent/
    AgentDefinition.java          # persona: prompt, toolset, role ceiling, trigger
    AgentRegistry.java            # code-defined agents + DB enable/disable (AI_ADMIN)
    Customer360Agent.java         # the phase-1 reference agent
  audit/
    AssistAudit.java, AssistAuditService.java   # G2: model+prompt version, tools, user
  domain/
    Conversation.java, ConversationRepository.java, Turn.java
  web/
    AssistController.java         # POST /messages (SSE), GET /capabilities
    AssistDtos.java               # request/response/event records
```

**Dependencies (pom):** Spring AI BOM + `spring-ai-ollama` (and later `spring-ai-anthropic`),
`langgraph4j` — all self-hosted deps, no CDN. Versions pinned per spike #1.

## 2. Configuration

```yaml
# application.yml (defaults; auth stays config-toggled per ADR-0007)
acme:
  assist:
    enabled: true                 # G4 global kill switch
    provider: stub                # stub | ollama | claude   (stub default → CI green)
    ollama:
      base-url: http://localhost:11434
      model-fast: granite4:3b     # two-tier: easy turns
      model-main: qwen2.5:7b      # hard turns / tool-heavy
      keep-alive: -1              # keep warm
      num-ctx: 4096
    budget:
      max-tool-iterations: 5
      per-user-daily-tokens: 0    # 0 = unlimited (only meaningful for hosted providers)
    locale-default: de            # bilingual DE/EN, locale-selected at runtime
```

`AssistProperties` mirrors this as a record (like `ai.AiProperties`). Per-agent `enabled` flags
live in the DB (AgentRegistry), not config.

## 3. Contract — `api/acme-assist.yaml` (v0.1.0)

```yaml
openapi: 3.0.3
info: { title: ACMEassist — in-app co-pilot, version: 0.1.0 }
paths:
  /assist/messages:
    post:
      summary: Send a message; streams the assistant's reply (SSE)
      x-required-role: WATCH        # conversation is read-shaped
      requestBody:                  # { conversationId?, message, context:{module,subView,entityId?} }
        content: { application/json: { schema: { $ref: '#/components/schemas/AssistRequest' } } }
      responses:
        '200':
          description: text/event-stream of AssistEvent (delta | tool | message | done)
          content: { text/event-stream: { schema: { type: string } } }
        '401': { $ref: 'acme-base.yaml#/components/responses/Unauthorized' }
  /assist/capabilities:
    get:
      summary: Agents & tools available to the caller (role- and module-scoped)
      x-required-role: WATCH
      responses: { '200': { description: capabilities } }
components:
  schemas:
    AssistRequest:
      type: object
      required: [message, context]
      properties:
        conversationId: { type: string, nullable: true }
        message: { type: string }
        context:
          type: object
          properties: { module: {type: string}, subView: {type: string}, entityId: {type: string, nullable: true} }
```

Served under `/api/base/assist`. SSE body documented in the description (OpenAPI 3.0 can't type an
event stream). Phase-3 adds `/assist/actions/{id}/confirm`.

## 4. Persistence

Flyway migration `V2x__assist.sql` — **reconcile the final number at integration** (the search
branch already took `V23`):

```sql
create table assist_conversation (
  id            varchar(36) primary key,
  user_id       varchar(36) not null,
  module        varchar(16),
  created_at    timestamptz not null default now()
);
create table assist_turn (
  id              varchar(36) primary key,
  conversation_id varchar(36) not null references assist_conversation(id),
  role            varchar(16) not null,      -- user | assistant
  content         text,
  created_at      timestamptz not null default now()
);
-- G2: audit is append-only; PII pseudonymized (store user_id, not names)
create table assist_audit (
  id              bigserial primary key,
  conversation_id varchar(36),
  user_id         varchar(36) not null,
  user_role       varchar(16) not null,
  agent           varchar(64),
  provider        varchar(32),
  model           varchar(64),
  model_version   varchar(64),
  prompt_version  varchar(32),
  tool            varchar(96),               -- endpoint invoked (null for pure text turn)
  outcome         varchar(24),               -- ok | denied_403 | error
  prev_hash       varchar(64),               -- tamper-evidence (hash chain)
  hash            varchar(64),
  created_at      timestamptz not null default now()
);
```

`assist_draft` is **not** created in phase 1 (no writes yet).

## 5. Security change (`base/BaseSecurityConfig`)

Add **before** the general write rule so `WATCH` users can converse:

```java
.requestMatchers(HttpMethod.POST, "/api/base/assist/messages").hasAnyRole(WATCH, WORK, ADMIN)
.requestMatchers(HttpMethod.GET,  "/api/base/assist/capabilities").hasAnyRole(WATCH, WORK, ADMIN)
.requestMatchers("/api/base/assist/admin/**").hasRole("AI_ADMIN")   // console (later)
```

Write **tools** stay gated because the tool call is re-dispatched through the same chain — a
`WATCH` conversation simply can't get a write tool to execute.

## 6. Core backend flow (read-only ReAct loop)

`AuthenticatedApiDispatcher.runAsUser(principal, toolCall)` issues a loopback request to the
module's own REST endpoint with the caller's bearer token → identical authz (403 if not entitled).
`SpringAiAssistantEngine` drives the langgraph4j graph (`model` ↔ `tools`, guard = 5 iterations,
no confirmation node) and emits `AssistEvent`s. `AssistController` bridges those to an `SseEmitter`.
`AssistAuditService` stamps each turn/tool call (G2). `StubAssistantEngine` returns a deterministic
brief so the transport + graph are CI-testable without a model.

## 7. Frontend

```
frontend/src/assist/
  Assist.tsx          # bottom bar + expandable panel (mounts last in .acme-app)
  assistApi.ts        # POST /assist/messages via fetch + ReadableStream (bearer, NOT EventSource)
  useAssist.ts        # state: open, turns, streaming; context from useShellState
```

- **Classes → `themes/base/components.css`** (tokens only, no inline styles, no CDN): reuse the
  prototype's `.acme-assist*` rules; elevation `0 -16px 48px var(--shadow)`; `✦` mark; tier badges.
- **Mount:** last child of `.acme-app` in `App.tsx`, inside `phase === 'authed'`.
- **Context:** pass `{ module: shell.activeMod, subView: shell.activeSubKey }` per turn (entityId
  deferred — no selected-entity concept yet).
- **⌘K:** bind the existing stub search chip to focus the assistant (one command surface).
- **SSE:** `fetch('/api/base/assist/messages', { headers:{Authorization:Bearer …} })` + read the
  stream; never put the token in a URL.
- **G1 disclosure:** persistent "KI-Assistent" label + source citations under answers.
- **i18n:** DE/EN strings for chips, labels, disclosure; locale from `useAuth`/browser.

## 8. Build order (milestones + acceptance)

| M | Milestone | Done when |
|---|-----------|-----------|
| M0 | Spikes (§0) pass | Spring AI on Boot 4.1 + a langgraph4j tool round-trip work; CPU latency measured |
| M1 | Module skeleton + `StubAssistantEngine` + SSE `/messages` | A canned brief streams to a test client over SSE; CI green with `provider=stub` |
| M2 | `AuthenticatedApiDispatcher` + Customer-360 read tools | A tool call hits `/api/crm/customers/{id}` as the user; 403 for the unentitled |
| M3 | `SpringAiAssistantEngine` + langgraph4j graph (`provider=ollama`) | Real grounded German/English brief from `qwen2.5:7b`, ≤5 tool iterations |
| M4 | `assist_audit` (G2) + `AssistProperties` two-tier + kill switch (G4) | Each turn logged with model+version; `enabled=false` disables instantly; fast/main routing |
| M5 | Frontend panel + `assistApi` SSE + ⌘K + G1 disclosure | User opens panel, asks, sees streamed answer + sources; theme-contract clean |
| M6 | `api/acme-assist.yaml` v0.1.0 + security matcher + persistence | Contract lints; WATCH can converse; conversation/turns persisted |

**Phase-1 exit = the Customer-360 acceptance criteria** (Appendix A): WATCH user gets a grounded,
streamed, bilingual brief citing real records; every tool call in `assist_audit` with model
version; no write tool reachable; stub engine covers CI; unentitled tool → 403 surfaced, never
fabricated.

## 9. Out of scope for phase 1 (deferred)

Writes / proposed actions / `assist_draft` / confirmation flow (phase 3) · proactive agents & the
service principal (later) · RAG + embeddings (phase 4) · the AI_ADMIN admin console UI (with the
write tiers) · hosted-provider budgets beyond the config stub.

## 10. Risks / dependencies

- Spikes (§0) are gating — especially Spring AI ⇄ Boot 4.1 and langgraph4j maturity.
- **Migration number** must be reconciled with the search branch (`V23` taken) at integration.
- CPU latency for heavy 7B multi-step turns stays tens-of-seconds (async-friendly) — set
  expectations; two-tier routing helps the common case.
- First SSE endpoint in the stack — bound the `SseEmitter` executor; add timeouts.
