# ADR-0008 — ACMEassist: an in-app co-pilot for ACMEsuite

- Status: **Proposed** (2026-07-12) — exploration / design spike, not yet accepted
- Scope: a new `assist` capability (backend module + contract + bottom-anchored frontend panel)
- Related: ADR-0006 (API-only integration), ADR-0007 (federated authn, local authz)
- Supersedes: nothing. Complements the existing `ai` module (`ContractIntelligence`)

> This ADR is a **proposal for discussion**. It records the recommended architecture, the
> phased scope, and the decisions the team should ratify before implementation. A thin,
> non-functional UI prototype of the panel accompanies it (see *Prototype* at the end); no
> production code is delivered with this ADR.

## Context

The interface design handoff (`design_handoff_acmesuite/`) already sketches a per-module
**"Copilot"**: a `✦`-marked assistant that offers clickable suggestion chips
(`Forecast Q3`, `Nächste Aktionen`, `Top-Konten`, `Verlorene Deals`), returns a prose answer
(e.g. *"3 Deals über €80k stagnieren seit mehr als 7 Tagen … Empfehlung: heute eine
Follow-up-Sequenz starten."*), and appears in the activity feed as an actor
(*"Copilot erstellte die Q3-Zusammenfassung"*). The design specifies its chrome — square
corners, `var(--panel)` surface, `1px var(--line)` border, and the only heavy elevation in the
system, `box-shadow: 0 16px 48px var(--shadow)` — but leaves the popover unimplemented (the flag
`copilotA: false` and the `copilotSuggest`/`copilotAnswer` data model exist; no markup renders).
ACMEassist **productionizes** that sketch, anchored at the bottom of the app shell.

What already exists in the repo shapes the decision:

- **A domain `ai` module.** `ai.ContractIntelligence` is a *port* (`summarize` / `assessRisks` /
  `ask` over contract document text) with a deterministic `StubContractIntelligence` and an
  `AiProperties` toggle (`acme.ai.provider=stub|ollama`). It is deliberately narrow: NLP over a
  *document*, destined for a local Ollama/RAG backend. It is **not** an agent and has no callers
  yet.
- **Contract-first REST (ADR-0006).** Every module is a bounded context with a hand-written
  controller under `/api/<module>` validated against an OpenAPI spec in `api/`. Clients consume
  the suite **exclusively through these contracts** — never internal code. Per-operation role is
  declared as `x-required-role`.
- **Federated authn, local authz (ADR-0007).** Base issues an HS256 session JWT with a `role`
  claim (`WATCH`/`WORK`/`ADMIN`, hierarchy `ADMIN > WORK > WATCH`). `BaseSecurityConfig` enforces
  it as a resource server: `GET /api/**` needs `WATCH`+, other `/api/**` needs `WORK`+, master
  data adds `@PreAuthorize("hasRole('ADMIN')")`. Authorization is enforced by the Spring filter
  chain, **not** by application code.
- **Frontend class-contract (themes/README.md).** `src/` emits stable `acme-*` class names and
  three root attributes only; all visuals live in `themes/base/components.css` using tokens
  (`--panel`, `--line`, `--radius`, `--shadow`, `--accent`, `--font-mono` …). No inline styles,
  **no CDN** (fonts self-hosted via `@fontsource`). The login card already uses the exact design
  shadow: `box-shadow: 0 16px 48px var(--shadow)`. `useAuth()` exposes `user`, `canWrite`,
  `isAdmin`; shell nav exposes `activeMod` / `activeSubKey`. The `⌘K` box in `TopBar` is a
  **non-functional stub** — no handler binds it. The API client is a thin `fetch` wrapper that
  attaches the bearer token.

The question this ADR answers: **how do we build a cross-module, tool-using assistant that can
answer, summarize, draft, and (guarded) act — without ever exceeding what the signed-in user
could do by hand, and without violating the API-first, class-contract, and no-CDN conventions?**

## Decision

### 1. A new `assist` module — reuse the `ai` seam's *philosophy*, not its *interface*

Create a new Spring Modulith module `de.acmesoftware.acmesuite.assist` rather than overloading
`ContractIntelligence`. The two are different in kind:

| | `ai.ContractIntelligence` | `assist.AssistantEngine` (new) |
|---|---|---|
| Shape | Single-shot NLP over one document | Multi-turn, tool-using agent loop |
| Scope | Contract domain | Cross-module (CRM/HR/Build/Supply/Base) |
| Grounding | The passed-in text | Live data via the REST API, role-scoped |
| Side effects | None | Reads always; writes behind confirmation |
| Backend | Ollama/RAG (local) | Spring AI (default self-hosted Ollama; Claude/others optional) + langgraph4j |

We keep the *pattern* that makes `ai` good — a **port + a config-toggled provider + a
deterministic stub** — and apply it to the assistant:

- `assist.AssistantEngine` — the port (start a turn, stream events, run the tool loop).
- `assist.SpringAiAssistantEngine` — default provider, implemented over **Spring AI**
  (`ChatClient`/`ChatModel`) + a **langgraph4j** agent graph, server-side. The concrete model
  backend is swappable by config (default **self-hosted Ollama**; Claude/OpenAI/Azure optional) —
  see Decision 4.
- `assist.StubAssistantEngine` — deterministic, offline, active by default so the existing test
  suite and local dev stay green (mirrors `StubContractIntelligence` and the
  `acme.base.auth.enabled=false`-by-default stance). Toggled by
  `acme.assist.provider=stub|ollama|claude|…`.

`ContractIntelligence` is **reused as one of the assistant's tools** (`summarize_contract`,
`assess_contract_risks`), so the existing seam is a capability the agent can call, not something
we replace or duplicate.

> **Why a module, not an out-of-process plugin.** An out-of-process sidecar would need its own
> copy of authn/authz, its own network path to the API, and its own deploy. In a modular
> monolith on ACMEbase, the assistant is most naturally *another authenticated API client that
> happens to run in-process*. Keep it in-process for phase 1; the `AssistantEngine` port keeps
> extraction to a service possible later if scaling demands it.

### 2. Grounding = tool-use over the existing REST contracts, executed **as the user**

The assistant never touches the database or internal services directly. Its tools map to the
**existing OpenAPI operations** (contract-first, ADR-0006). Tool schemas can be **generated from
the OpenAPI specs** in `api/`, so the tool surface *is* the public contract and stays in sync by
construction.

The load-bearing guardrail: **every tool call is re-dispatched through the identical Spring
Security chain, carrying the user's own JWT.** When the model calls `create_quote`, the assistant
issues `POST /api/crm/quotes` as a loopback request authenticated with the caller's bearer token.
The `WATCH`/`WORK`/`ADMIN` URL rules and the `@PreAuthorize` master-data guards apply *byte for
byte* as they would for a manual request. Consequences:

- The assistant **cannot exceed the user's role** — a `WATCH` user's write tool-call returns 403,
  exactly as a manual attempt would. Authorization is proven by the same code that guards the UI,
  not re-implemented in the agent.
- No new bypass path is created (calling `@Service` beans directly *would* skip the URL-level
  `WATCH`/`WORK` gate, which today lives only at the web layer — so we deliberately go **through
  the HTTP surface**, not around it).
- It honors ADR-0006 literally: the co-pilot consumes the suite **only through the API**.

The loopback cost (in-process HTTP) is negligible for a modular monolith and optimizable later
(a shared `AuthenticatedApiDispatcher` could switch to an internal servlet dispatch while keeping
the security-context semantics identical). See *Open questions* for the read-tool authz subtlety.

### 3. Read vs. write separation — writes are *proposals*, confirmed by a human

- **Read tools** (mapped to `GET` operations) run automatically within a turn.
- **Write tools** (mapped to `POST`/`PATCH`) never execute inline. The model emits a **proposed
  action**; the backend returns it to the UI as a structured, human-readable confirmation card
  ("Create quote for *Vela Robotics*, 3 items, €82,400 — Confirm?"). Only an explicit
  `POST /api/base/assist/actions/{id}/confirm` executes it — and that execution re-runs the same
  role check server-side. Approval is **per-action and per-turn**; it is never inferred from
  conversation.

This mirrors the platform's existing e-approval instinct and the design's activity-feed model
(the assistant becomes an actor once an action is confirmed).

### 4. LLM integration — Spring AI (provider-agnostic) + langgraph4j, default self-hosted Ollama

Customers need either to **use their own AI** or to have **one brought along**. That rules out a
single hardwired hosted vendor and makes a provider abstraction the requirement, not a
convenience. So:

- **Spring AI is the abstraction.** The `AssistantEngine` port is implemented over Spring AI's
  `ChatClient`/`ChatModel`, so the backend is swappable by configuration: `spring-ai-ollama`
  (self-hosted), `spring-ai-anthropic` (Claude), OpenAI/Azure/Bedrock/Vertex, or a customer's own
  endpoint — all behind one interface. This **continues the trajectory the repo already set**:
  `ai.AiProperties` already declares `provider=stub|ollama` with the note *"tool calling +
  structured JSON output"*. `StubAssistantEngine` stays for tests/CI.
- **Default = self-hosted Ollama ("we bring one along").** Data stays on the customer's premises;
  nothing egresses. This is the primary, data-sovereign default. **Claude** (and other hosted
  providers) are opt-in for customers who accept egress and want top-tier tool-use quality; they
  are selected via `acme.assist.provider` — the code path is identical because Spring AI abstracts
  it. *(Model selection for the bundled default — a tool-calling LLM and a German-capable
  embedding model, ideally CPU-only — is captured in "Model selection" below.)*
- **Orchestration = langgraph4j.** The agent graph (tool loop, the read→answer / write→propose
  branches, confirmation gate) is modelled explicitly in langgraph4j rather than a bespoke loop,
  which keeps the write-interception and audit hooks first-class. It is model-agnostic — it runs
  over whatever Spring AI `ChatModel` is wired in.
- **Tool-calling is the hard capability requirement.** The whole design rests on function calling
  over the REST tools. Claude and the strong hosted models do this well; **Ollama tool-calling
  quality depends on the model** (only tool-capable models qualify). Crucially, the **guarded-write
  + confirmation** model protects *regardless of backend strength* — a weaker local model can be
  less useful but cannot do damage, because writes are still human-confirmed and role-checked.
- **Key/endpoint custody.** No credential ever reaches the browser. With hosted providers this is
  "API key server-side only"; with self-hosted Ollama it degrades gracefully to "the model
  endpoint is server-side only" — same guarantee.
- **Transport = SSE.** A new `POST /api/base/assist/messages` streams `text/event-stream`
  (assistant deltas, `tool_use` / `tool_result` markers, proposed-action events, terminal
  message). Spring AI exposes streaming as `Flux<>`, which bridges cleanly to SSE. This is the
  **first SSE endpoint** in the codebase; the stack is Spring MVC (blocking `@RestController`s),
  so use `SseEmitter`/`ResponseBodyEmitter`.
  - **Browser caveat:** native `EventSource` cannot set an `Authorization` header. The frontend
    already speaks `fetch` + bearer, so consume the stream with `fetch()` + `ReadableStream`
    (not `EventSource`). Noted so we don't accidentally push the token into a URL param
    (forbidden by the security rules).
- **No CDN.** Ollama, the model weights, and the Java dependencies are all self-hosted; Spring AI
  / langgraph4j are normal Gradle/Maven dependencies.

> **Version due-diligence.** Confirm a Spring AI release compatible with the **Spring Boot 4.1**
> line, and treat **langgraph4j** (a community JVM port of LangGraph) as less battle-tested than
> its Python origin — spike both before committing phase 1. Tracked in *Risks* and *Open
> questions*.

#### Model selection (bundled default)

Two Ollama models make up the "we bring one along" default: a **tool-calling LLM** (needed from
phase 1) and — for later RAG grounding (phase 4 / the `ai` module's document Q&A) — a
**German-capable embedding model** for vectoring. The tool-use core grounds via the REST API and
does **not** need embeddings. Both must be CPU-runnable, permissively licensed (commercial), and
good at German.

**Recommended default pairing:**

- **LLM: `qwen2.5:7b`** — Apache-2.0 (no MAU cap), carries Ollama's *Tools* badge with a reliable
  first-party template for Spring AI / langgraph4j function-calling, best-documented German quality
  per parameter among small models (German MMLU ≈ 68), and sits in the 7–8B **CPU sweet spot**
  (~3.7 GB Q4 weights, ~5–7 GB resident).
- **Embeddings: `snowflake-arctic-embed2:568m`** — Apache-2.0, the only *in-library* embedder with
  explicit, benchmarked **German** retrieval; 1024 dims (Matryoshka-truncatable), 8K context.

**Vetted alternatives** (same `AssistantEngine`, config-swappable): **IBM Granite 3.3-8B / Granite 4**
(Apache, enterprise-tuned for function-calling; Granite-4 3B is a good CPU fast-path), **Qwen3-8B**
(Apache, newer — but a *thinking* mode that may need disabling for the tool loop), **Mistral 7B**
(Apache, tool-calling less turnkey). Embedding swap: **`bge-m3`** (MIT, hybrid dense+sparse).

**Explicitly rejected for the default:** **Gemma 2/3** (excellent German but **no Ollama Tools
badge** → disqualified for the agent role), **Llama 3.1/3.3** (weakest small-model German + Meta
Community License is not OSI-open, needs legal review), **Ministral** and the German fine-tunes
(Teuken/SauerkrautLM/DiscoLM) (non-commercial or not in the official library with no tool template).

> **CPU-only, two-tier (decided — no GPU).** Single-shot Q&A/RAG at 7–8B Q4 is fine (~2–4 s on a
> modern Xeon), but a multi-step tool loop pays a prefill on every LLM call, so a 3–5 step turn
> lands in the **tens of seconds** on typical CPUs — snappy only where AMX/AVX-512 is present.
> Decision: **no GPU required; two-tier CPU routing** — a ~3B model (Granite/Phi) handles easy turns
> snappily, the 7B runs on demand for hard ones. Plus keep the model warm (`OLLAMA_KEEP_ALIVE=-1`),
> cap `num_ctx`, Q4_K_M, and minimize agent steps. Honest limit: **heavy 7B multi-step turns stay
> slow on CPU** (async-friendly); size the hardware floor per target CPU (AVX-512/AMX decisive).
>
> **Pin & re-verify at deploy:** Ollama library contents and *Tools* badges change; pin exact HF
> revisions for licensing (e.g. avoid `qwen2.5:3b` — currently non-commercial), and validate German
> quality on real CRM/HR content before committing.

**Sources & full evidence** (model shortlists, licenses, benchmarks, CPU-latency figures, and the
provenance of every input): [Appendix C — research & sources](ADR-0008-acmeassist-research.md).

### 5. Invocation & placement — bottom-anchored panel + ⌘K, context = module + entity

- **Placement.** A new `ACMEassist` component mounts as the **last child of `.acme-app`** (root is
  `position: relative; overflow: hidden`) in the `phase === 'authed'` branch — a bottom-anchored
  bar that expands into a panel. New classes (`acme-assist`, `acme-assist-panel`, …) go in
  `themes/base/components.css`, tokens only; elevation reuses `0 -16px 48px var(--shadow)` (the
  design shadow, mirrored upward).
- **⌘K tie-in.** The stub `⌘K` box in `TopBar` becomes the keyboard entry point to the same
  assistant/command surface — no existing handler to conflict with. The **top-bar search and the
  bottom co-pilot are two faces of one command surface**: ⌘K focuses it; the bottom panel is its
  persistent home and conversation history.
- **Context.** Each turn carries `{ module: activeMod, subView: activeSubKey, entityId? }` so the
  assistant knows *what the user is looking at*. Today the shell exposes `(activeMod,
  activeSubKey)` but **has no selected-entity concept** and `useShellState` is local to `App.tsx`
  — phase 1 uses the module/sub-view signal; a later slice lifts shell state to context and adds
  an entity signal.

### 6. Contract — a dedicated `api/acme-assist.yaml`, path `/api/base/assist`

Add a new contract `api/acme-assist.yaml` (v0.1.0) that `$ref`s shared components from
`acme-base.yaml`, consistent with the "each context its own spec" convention. Sketch:

```
POST /api/base/assist/messages         # SSE stream; body {conversationId?, message, context}
                                       # x-required-role: WATCH (any authenticated may converse)
GET  /api/base/assist/capabilities     # tools available to THIS user (role- and module-scoped)
                                       # x-required-role: WATCH
POST /api/base/assist/actions/{id}/confirm   # execute a pending write; re-checked server-side
                                       # x-required-role: WORK  (ADMIN for master-data actions)
GET  /api/base/assist/conversations/{id}     # history (optional, phase 2+)
```

Version-bump per repo rules: ship `acme-assist.yaml` at **0.1.0**; no change to the other four
specs. SSE responses are documented as a streaming `text/event-stream` body (OpenAPI expresses
the event contract in the description).

> **Security-rule change required.** `BaseSecurityConfig` currently sends *any* non-GET `/api/**`
> to `WORK`+. `POST /api/base/assist/messages` must be reachable by `WATCH` users (conversation
> is read-shaped). Add an explicit matcher:
> `.requestMatchers(POST, "/api/base/assist/messages", "/api/base/assist/capabilities").hasAnyRole(WATCH, WORK, ADMIN)`
> **before** the general `/api/**` write rule. The *write* safety still holds because the
> assistant's write **tools** are the ones gated — a `WATCH` conversation simply cannot get a
> write tool to execute.

### 7. Auditing

Every tool call (read and write) and every confirmed action is written to an `assist_audit`
table: `who` (user + role), `when`, `conversation`, `tool`, `target endpoint`, `arguments
(redacted)`, `outcome/status`. This gives the "Copilot as actor" activity-feed entries a real
source and makes the assistant's behavior reviewable — a requirement for anything that can write.

## Scope tiers / phases

| Phase | Capability | Guardrail surface | New moving parts |
|---|---|---|---|
| **1 (recommended slice)** | **Read-only, grounded Q&A + summarize over the active module**, streamed | Any authenticated user; read tools only → no confirmation flow, no mutation | `assist` module + `AssistantEngine` port + Spring AI/langgraph4j engine (Ollama default) + stub; `POST /messages` SSE; read tools from GET contracts; bottom panel UI; audit (reads) |
| **2** | **Draft** — compose a quote/email/summary *without persisting*; returns an editable proposal; cross-module context | Still read-only side effects; draft is inert until the user acts | Draft tool(s); richer context (lift shell state, entity signal); conversation history |
| **3** | **Guarded ACT** — create quote, flag supplier, etc. | Write tools → **proposed action + explicit confirm**, role-checked server-side, audited | Write tools from POST/PATCH contracts; `actions/{id}/confirm`; confirmation cards; `assist_audit` writes |
| **4** | **Command surface + proactive** — ⌘K palette across modules; proactive suggestions & activity-feed authoring; RAG grounding | Same authz model; rate/cost controls hardened | ⌘K wiring; suggestion engine; optional RAG/knowledge index |

## Recommended phase-1 slice

**"Ask the current module."** A `WATCH`+ user opens the bottom panel (or hits ⌘K), sees the
design's suggestion chips for the active module, and asks a question. The `assist` module runs a
Claude tool-loop whose tools are the **read (GET) operations of the active module**, each
executed as a loopback call with the user's JWT; the answer streams back token-by-token over SSE
into the panel. No writes, no confirmation flow, no entity concept required.

Why this slice:

- **Proves the whole spine** — LLM integration, SSE streaming, tool-use, and the
  execute-as-the-user authorization model — with the **lowest risk** (read-only, single module).
- **Delivers the design's visible UX** (chips + streamed prose answer, `✦`, the popover shadow)
  end to end.
- **Reuses everything** — the existing GET contracts become the tool surface for free; the stub
  engine keeps CI green; the class-contract keeps the UI themeable.
- Everything risky (mutation, confirmation, audit-of-writes, cross-module, proactivity) is
  deferred to phases 3–4 behind an already-proven authz seam.

## Agent catalog

Concrete "clever agents" to design *with* the platform — scoped personas on the one engine, each
mapped to the read/write + role + confirmation model above — are catalogued in
[Appendix A — agent catalog](ADR-0008-acmeassist-agents.md) (18 agents across all five modules,
plus the eight reusable agent patterns). It also raises the **service-identity** question for
proactive agents (they have no signed-in user, so they may only read/draft).

## AI governance — ISO/IEC 42001

The suite must respect **ISO/IEC 42001:2023** (AI management system). That standard is mostly
organizational (policies, roles, impact assessments, audits); the **software's job is the technical
controls + evidence**. Encouragingly, much is **already inherent in this design** — human-in-the-loop
confirmation, execute-as-the-user authorization, the audit spine, role-scoped data minimization,
the on-prem Ollama default, and source citations are all 42001-aligned controls. The genuinely new
build items (AI disclosure/labeling, model+prompt versioning in the audit, feedback/override
metrics, a kill switch, the agent risk registry, data-boundary/redaction/retention, an in-app model
card, and an admin governance console) plus the full mapping are in
[Appendix B — AI governance / ISO 42001](ADR-0008-acmeassist-governance.md).

## Alternatives considered

- **Extend `ai.ContractIntelligence` to become the assistant.** Rejected: it is a single-shot,
  document-scoped NLP port bound for local Ollama/RAG. Bolting an agentic, cross-module, tool-using,
  streaming loop onto it would blur two very different responsibilities and couple the assistant's
  lifecycle to the contract-NLP backend. We reuse its *pattern* and call it as a *tool* instead.
- **Out-of-process co-pilot plugin/sidecar.** Rejected for phase 1: duplicates authn/authz, adds a
  network hop and a deploy unit, and fights the modular-monolith grain. The `AssistantEngine` port
  leaves the door open if scale later demands extraction.
- **Let the assistant call internal `@Service` beans directly.** Rejected: today the
  `WATCH`/`WORK` gate lives at the URL layer, not on the services — direct calls would **bypass**
  it and let a `WATCH` user's tool mutate data. Going through the HTTP surface makes the authz
  identical to a manual call and honors API-first.
- **Hardwire one hosted vendor (e.g. the Anthropic Java SDK directly).** Rejected: customers need
  *either* their own AI *or* one we bring along, so a provider abstraction is a requirement, not a
  nicety. Spring AI gives us that abstraction with a self-hosted-Ollama default and Claude/others
  as opt-in — and a direct-SDK path could not offer the data-sovereign on-prem default.
- **Bespoke agent loop instead of langgraph4j.** Viable, but langgraph4j models the tool loop and
  the read/write branches explicitly, keeping the write-interception and audit hooks first-class.
  Accepted its lower maturity as a tracked risk rather than reinventing the orchestration.
- **LLM in the browser / credential in the client.** Rejected outright (prohibited): no API key or
  model endpoint reaches the browser. Server-side only, in config/env.
- **WebSocket transport.** Deferred: SSE is simpler, one-directional-streaming fits the
  request/stream-response shape, and it needs no new infra. Revisit only if bidirectional
  steering (interrupt mid-turn) becomes a hard requirement.
- **Fold assist paths into `acme-base.yaml`.** Reasonable (assist lives under `/api/base`), but a
  dedicated `acme-assist.yaml` keeps Base focused and treats assist as its own bounded context.
  Left as an open decision.

## Risks

- **Prompt injection via tool results.** Data returned from the API (customer notes, supplier
  names) is **untrusted** and could contain "act on me" text. Mitigations: the model is
  instructed to treat retrieved data as data, not instructions; **writes always require explicit
  human confirmation**; and — decisively — the LLM cannot craft a request that exceeds the user's
  role because authorization is enforced by Spring, not the model.
- **Over-trust of a confident answer.** Grounded answers can still be wrong. Mitigations: cite the
  source operation/records behind an answer; keep write actions human-gated; audit everything.
- **Local-model quality & tool-calling reliability.** The self-hosted-Ollama default trades quality
  for sovereignty: small CPU-runnable models are weaker at German and at reliable tool-calling than
  hosted Claude. Mitigations: pick a genuinely tool-capable model (see *Model selection*); keep
  phase-1 tool sets small and well-described; and note that the **guarded-write model caps the
  blast radius regardless of backend** — a weak model is less useful, not less safe. Customers who
  want top quality switch the provider to Claude/hosted.
- **CPU-only latency.** No-GPU inference plus a multi-step tool loop can be slow. Mitigations: SSE
  streaming so the user sees progress; keep the model warm (Ollama `keep_alive`); modest model
  size; size the tool loop. Quantify on target hardware during the phase-1 spike.
- **Spring AI / langgraph4j maturity & Boot 4.1 compatibility.** Spring AI must match the Spring
  Boot 4.1 line, and langgraph4j is a community JVM port. Mitigation: a compatibility + tool-calling
  spike before phase-1 commit; the `AssistantEngine` port isolates the rest of the system from the
  choice.
- **Cost / abuse (hosted providers only).** When a customer opts into a hosted backend, spend scales
  with use. Mitigations: per-user rate limits, a per-turn token budget, cost/observability on the
  `assist_audit` trail. (N/A for the self-hosted default beyond compute.)
- **Latency / UX.** Tool-loops over loopback HTTP add turns. Mitigations: SSE streaming so the
  user sees progress; keep phase-1 tool sets small and module-scoped.
- **First SSE in the stack.** New transport in a blocking MVC app. Mitigation: `SseEmitter` with a
  bounded executor; timeouts; the stub engine to test the transport without the LLM.
- **`useShellState` is local, no entity concept.** Rich "what am I looking at" context needs a
  refactor (lift to context, add entity signal). Phase 1 avoids it by using only module/sub-view.
- **Secret custody.** Any hosted-provider key (or the Ollama endpoint) is managed server-side until
  a real secret store exists — same caveat ADR-0007 already carries for the JWT secret and
  `SecretCipher` master key.

## Decisions (resolved open questions)

*Resolved during ADR review — 2026-07-12.*

1. **Read-tool authorization — role ceiling + per-agent toolset.** The assistant may read whatever
   the user's role allows via GET, narrowed in practice by each agent's small, explicit toolset
   (no separate global allowlist). Consistent with execute-as-the-user.
2. **Tool surface — generated + curated.** Tool schemas are generated from the OpenAPI specs (stay
   in sync) and hand-curated per agent (selection + descriptions).
3. **Contract home — a dedicated `api/acme-assist.yaml`** (v0.1.0), referencing base components.
4. **Models & hardware — `qwen2.5:7b` + `snowflake-arctic-embed2`, two-tier CPU routing, no GPU
   required.** A small model (Granite/Phi ~3B) handles easy turns snappily; the 7B runs on demand
   for hard ones. Hosted providers get a configurable per-user token/rate budget. *(Heavy
   multi-step turns on the 7B stay tens-of-seconds on CPU — async-friendly; the hardware floor is
   sized per target CPU, AVX-512/AMX decisive — see Model selection.)*
5. **Conversation persistence — persist from the start** (required anyway for audit G2 and the
   activity-feed provenance).
6. **Loopback first.** In-process loopback HTTP behind `AuthenticatedApiDispatcher`; the seam
   allows switching to internal servlet dispatch later for performance.
7. **Localization — bilingual DE/EN from the start** (system prompts, suggestion chips, and
   confirmation copy in both; locale-selected at runtime).
8. **Command surface — one component/state from phase 1.** ⌘K focuses the same assistant; the
   bottom panel is its home. (Internal ACMEsuite decision; no cross-product requirement.)

## Remaining open items

- **Hardware floor to certify** — the concrete minimum CPU/RAM profile (and the two-tier model
  pair) to validate on a target server before the phase-1 commit.
- **AI-governance policy sign-offs** — audit retention window, PII redaction, and the GDPR balance
  need DPO/legal sign-off (see [Appendix B](ADR-0008-acmeassist-governance.md)).

## Cross-reference — ACMEmailtrap (external prior art only)

**ACMEmailtrap is a separate, independent ACMEsoftware product — it is _not_ part of the
ACMEsuite.** It shares a vendor and a family resemblance in its design handoff, nothing more.
This ADR looked at its in-progress search feature purely as **external prior art**; ACMEassist
takes **no dependency** on it, shares **no code, contract, or theme** with it, and must not couple
its architecture to it. Cross-product "consistency" is explicitly a **non-goal** here — ACMEsuite
owns its own theme and command surface independently.

What the look confirmed (as reference, not as a coupling point):

- **It is search, not command.** ACMEmailtrap has *no* action/command layer — only retrieval
  (`GET /api/search` → grouped, capped, prefix/type-ahead results). So it offers **no prior art
  for the "command"/act half at all** — ACMEassist builds that entirely fresh. At most its
  *retrieval contract shape* (grouped + capped + type-ahead) is a familiar template if ACMEassist's
  ⌘K ever does structured search alongside conversation.
- **Its palette is unbuilt**, so there is no component to borrow even if we wanted to — ACMEsuite's
  ⌘K work is greenfield regardless.
- **Token-name overlap is coincidental heritage, not a shared system.** Both handoffs happen to use
  similar token names (`--panel`/`--line`/`--accent` …) and a `data-mode` idea, but they are
  *separate design systems maintained separately*. ACMEsuite must **not** import or track
  ACMEmailtrap's tokens; it keeps its own `themes/` as the single source of truth. (ACMEmailtrap
  also styles with inline `style={{}}` objects — which ACMEsuite's class-contract forbids outright,
  underlining that these are different codebases with different disciplines.)

**Net:** ACMEmailtrap is informative background, not a design input with any binding force.
Open question 8 below is therefore an **internal** ACMEsuite question (should ⌘K and the bottom
panel share one component?), with no cross-product alignment requirement.

## Prototype

A thin, **non-functional** UI prototype of the bottom-anchored panel accompanies this ADR
(`docs/adr/ADR-0008-acmeassist-prototype.html`) — static markup + the class-contract CSS
(tokens only, no inline styles, no CDN), showing the collapsed bar, the expanded panel with the
`✦` mark, German suggestion chips, a streamed-answer area, and a (disabled) confirmation card.
It renders no live data and calls no API; it exists to align on placement, chrome, and the
design's `0 16px 48px var(--shadow)` elevation before any implementation begins.

## Where this would live (code) — indicative

- Backend module: `assist/AssistantEngine`, `assist/SpringAiAssistantEngine` (Spring AI
  `ChatClient` + langgraph4j graph), `assist/StubAssistantEngine`, `assist/AssistProperties`
  (`provider=stub|ollama|claude|…`, model + endpoint config), `assist/tools/*`,
  `assist/AuthenticatedApiDispatcher`, `assist/web/AssistController`, `assist/audit/*`
- Reused seam: `ai/ContractIntelligence` (as a tool)
- Contract: `api/acme-assist.yaml` (v0.1.0)
- Security: matcher additions in `base/BaseSecurityConfig`
- Schema: `V23__assist_audit.sql`
- Frontend: `frontend/src/assist/*` (component + `assistApi.ts` + fetch/SSE client),
  classes in `frontend/themes/base/components.css`
