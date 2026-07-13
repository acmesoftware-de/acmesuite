# ADR-0008 · Appendix B — AI governance (ISO/IEC 42001)

- Status: **Proposed** (2026-07-12) — companion to [ADR-0008](ADR-0008-acmeassist-copilot.md)
- Purpose: translate **ISO/IEC 42001:2023** (AI Management System) into the concrete technical
  controls and evidence ACMEassist must build in.

> **Scope boundary.** ISO/IEC 42001 is a *management-system* standard (PDCA, clauses 4–10, plus
> the Annex A controls). **Most of conformity is organizational** — policies, roles, competence,
> internal audit, management review, and the written *AI system impact assessments*. Software
> cannot deliver those. What software delivers is the **technical controls** and the **evidence**
> (logs, records, documentation surfaces) the AIMS relies on. This appendix lists only that
> software part. The control-area mapping below is **indicative** — confirm exact Annex A controls
> with your certification auditor.
>
> **Adjacency.** ISO 42001 is commonly used to demonstrate **EU AI Act** diligence (this is an
> EU/DE product). Several items below (transparency/disclosure, logging, human oversight) also
> map to AI-Act obligations; treat 42001 as the umbrella.

## The headline: much is already inherent in the design

| ISO 42001 control area (Annex A, indicative) | Already in the ACMEassist design |
|---|---|
| Human oversight / responsible use | **Human-in-the-loop confirmation** for every write (Decision 3) — non-bypassable |
| Access control / authorization | **Role ceiling, execute-as-the-user** (Decision 2 + ADR-0007) — the assistant can never exceed the user |
| Data for AI — minimization & provenance | **Role-scoped REST grounding** (no raw DB), **source citations** behind answers |
| Data residency | **Self-hosted Ollama default** (Decision 4) — data need not leave the premises |
| Record-keeping / traceability | The **`assist_audit`** spine (Decision 7) |
| Life cycle — verification | **Deterministic `StubAssistantEngine`** + tests keep behavior reproducible |
| Intended use / misuse limits | **Scoped agent personas** with small toolsets (Appendix A) |

So the gap is not the architecture — it is making these **named, evidenced, and operable**, plus a
short list of genuinely new features.

## What we must still build in

| # | Build item | Maps to (42001 area) | Effort |
|---|-----------|----------------------|--------|
| G1 | **AI disclosure & content marking** — clear "you are interacting with AI" (the ✦ mark), and every AI-generated record/draft/action tagged as authored by ACMEassist (extend the activity-feed provenance already in the design) | Transparency / information to interested parties | Light |
| G2 | **Model & prompt versioning in the audit record** — capture provider, **model id + version**, prompt-template version, tool set, inputs/outputs, the confirming human, timestamp on every turn | Record-keeping; life-cycle traceability | Light–Med |
| G3 | **Feedback & override tracking** — thumbs/report on outputs, and a metric for how often humans **reject** proposals (the key quality/oversight-effectiveness KPI) | Monitoring; human oversight effectiveness | Med |
| G4 | **Kill switch & enable flags** — disable the assistant, a single agent, or a provider **instantly** per tenant/role (build on `acme.assist.provider=stub` + per-agent `enabled`) | Incident handling; operational control | Light–Med |
| G5 | **Agent registry with risk classification** — each agent carries an impact/risk tier + a reference to its AI impact assessment + affected parties (this is also the governance registry from Appendix A) | AI system impact assessment; responsible use | Med |
| G6 | **Data-boundary controls** — hosted-provider **opt-in**, an explicit **no-training** assurance for hosted APIs, **PII redaction in logs**, and retention/deletion of AI-interaction data (GDPR) | Data for AI; third-party relationships | Med |
| G7 | **System documentation / model-card surface** — in-app page: capabilities, limitations, intended use, current model/provider, known risks | Information to interested parties; documentation | Light–Med |
| G8 | **Admin AI-governance console** — review the audit trail, enable/disable agents & providers, see the active model, export evidence | Internal organization; monitoring | Med |
| G9 | **Monitoring dashboards** — override rate, error/refusal rate, latency; drift once RAG lands | Operation & monitoring | Med (later) |

### The genuinely new, in one line
Transparency/labeling (G1), **version capture** in the audit (G2), **feedback/override metrics**
(G3), a **kill switch** (G4), the **agent registry with risk tiers** (G5), **data-boundary +
redaction + retention** (G6), an **in-app model card** (G7), an **admin governance console** (G8),
and **monitoring** (G9). Everything else is naming and evidencing what the guardrail design
already does.

## Not software (organizational — flagged so the team owns them)

These are required for conformity but are **not** code: the AI policy and roles (an accountable AI
owner), competence/training, the written AI system impact assessments per agent, supplier due
diligence for any hosted provider, internal audit, and management review. The software features
above (esp. G5, G7, G8) *feed* these processes with evidence but do not replace them.

## Decision — a separate `AI_ADMIN` role (orthogonal grant)

ISO 42001 wants an **accountable AI role with separation of duties**. We therefore add a **distinct
`AI_ADMIN` grant** rather than folding AI governance into the business `ADMIN`.

**Orthogonal, not a fourth hierarchy level.** The business roles are a linear hierarchy
(`ADMIN > WORK > WATCH`, ADR-0007) over `/api/**`. AI governance is a *different axis*: an AI-Admin
governs the assistant but is not necessarily a business master-data admin, and a business ADMIN is
not automatically the AI owner. So `AI_ADMIN` is a **capability a user holds beside** their access
role — e.g. a `WORK` user can be the AI-Admin without gaining business-ADMIN rights.

| Aspect | Design |
|--------|--------|
| **Model** | A grant orthogonal to `WATCH/WORK/ADMIN`; a user has an access role **and optionally** `AI_ADMIN`. |
| **Token (extends ADR-0007)** | The Base JWT gains a second grant (e.g. `grants:["AI_ADMIN"]` / `aiAdmin:true`). `BaseSecurityConfig`'s converter — today one authority from `role` — additionally emits `ROLE_AI_ADMIN`. A deliberate, documented departure from "single role claim". |
| **Authorization** | `/api/base/assist/admin/**` requires `hasRole('AI_ADMIN')`. Conversation endpoints stay `WATCH`+; business `/api/**` rules are untouched. |
| **Who grants it** | The business `ADMIN` assigns/revokes `AI_ADMIN` via the existing user-admin surface. **SoD:** ADMIN decides *who* governs the AI; the AI-Admin *operates* governance and **cannot** grant business roles, escalate business access, or grant `AI_ADMIN` to themselves. |
| **What it governs** | Enable/disable agents + per-role/tenant availability (G4/G5), provider/model config + allowlist + data-boundary (G6), view/export the assist audit (G2/G8), author impact assessments + risk tiers (G5), the kill switch (G4), monitoring (G9), model-card content (G7). It grants **no** business write access. |
| **Bootstrap** | The break-glass `ADMIN` (ADR-0007) holds `AI_ADMIN` at first run and delegates it; no separate break-glass path needed. |

## Agent admin UI (G8 — the governance console)

A dedicated, **`AI_ADMIN`-gated** surface — its own top-level tab (so separation of duties is
*visible*, not buried inside business Admin). It follows the module-registry pattern (a new
`ModuleDef` with an `aiAdminOnly` gate mirroring the existing `adminOnly`) and the theme
**class-contract** (new `acme-*` classes in `themes/base/components.css`, tokens only, no inline
styles, no CDN). Five sub-views:

1. **Agenten** *(default)* — a table of every agent from the registry. Columns: **Agent · Modul ·
   Autonomie-Stufe** (a badge reusing the ladder colors — read=green, draft=blue, write=amber) **·
   Min-Rolle · Status** (Ein/Aus, per Rolle/Tenant) **· Risiko-Tier · Override-Rate · zuletzt
   genutzt**. Row → **detail drawer**: toolset + endpoints, the (versioned, read-only) system
   prompt, the AI impact assessment (edit), an availability matrix (roles × tenants), and the
   enable/disable toggle. This is the operational heart — enabling/disabling an agent is one click.
2. **Provider & Modell** — active provider (Ollama default / Claude / …), model id **+ version**,
   endpoint, the approved-model allowlist, data-boundary controls (hosted opt-in · no-training ·
   retention · PII-redaction), and the **global kill switch**.
3. **Audit** — the `assist_audit` trail with filters (user / agent / date / outcome); a turn opens
   to show prompt, tool calls, model+version, and the confirming human; **export** for evidence.
4. **Monitoring** — a KPI bar (override rate · error/refusal rate · avg latency); drift once RAG
   lands. Reuses the existing `acme-kpi` pattern.
5. **Model-Card** — edit the in-app documentation (capabilities · limits · intended use · current
   model), which the concierge (agent #16) also surfaces to end users.

The autonomy-tier badge and the read/draft/write coloring are shared with the agent catalog's
autonomy ladder, so the governance UI and the design language stay one system. A thin,
non-functional mockup of the **Agenten** screen can follow the existing prototype approach
(`ADR-0008-acmeassist-prototype.html`) if the team wants a visual spec artifact.

## Decisions (resolved) & phasing

*Resolved during ADR review — 2026-07-12.*

- **Certification target — "designed to 42001".** Build cert-ready; no formal certification now.
  This scopes the phasing: **phase 1 ships G1 (disclosure/marking), G2 (model+prompt versioning in
  the audit), G4 (kill switch/enable flags)**; G3/G5–G9 follow as the write/proactive tiers land.
- **Accountable AI role — a separate orthogonal `AI_ADMIN` grant**, carried as a **`grants[]`
  array** claim (extensible), with the converter change in `BaseSecurityConfig` emitting
  `ROLE_AI_ADMIN` (see *Decision* above).
- **Impact-assessment workflow — authored & versioned in the admin console (G8)**, stored
  in-product, referenced from the agent registry (G5).
- **Audit immutability — append-only + tamper-evident** (e.g. hash-chained). Retention: keep the
  *event*, **pseudonymize/redact the *person*** so a GDPR deletion request is satisfied without
  destroying the audit trail.

## Remaining open items (need policy/legal sign-off)

- **Retention window & PII-redaction policy.** The concrete retention period, exactly what is
  redacted in logs, and how PII in prompts is treated for a **hosted** provider vs. the **local**
  model — all need **DPO/legal sign-off**. (Local Ollama default keeps data on-prem, which
  simplifies this materially.)
