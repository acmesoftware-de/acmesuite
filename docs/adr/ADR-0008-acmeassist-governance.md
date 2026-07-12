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

## New open questions (added to ADR-0008)

- **Audit immutability & retention.** Is `assist_audit` append-only/tamper-evident, and what is
  the retention window vs. GDPR deletion rights? (These pull in opposite directions — decide the
  balance.)
- **PII handling & redaction policy.** What is redacted in logs, and how do we treat PII in prompts
  sent to a hosted provider vs. the local model?
- **Impact-assessment workflow.** Is the per-agent AI impact assessment authored in the admin
  console (G8) and versioned in-product, or kept as external documents referenced by G5?
- **Accountable AI role.** Do we add an explicit AI-governance capability to the role model (an
  "AI admin" beyond `ADMIN`), or fold it into `ADMIN`?
- **Certification target.** Are we pursuing actual ISO 42001 certification (drives rigor and
  timing), or "designed to 42001" as a diligence posture? This scopes how much of G1–G9 is phase-1
  vs. later.
