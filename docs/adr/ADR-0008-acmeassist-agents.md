# ADR-0008 · Appendix A — ACMEassist agent catalog

- Status: **Proposed** (2026-07-12) — companion to [ADR-0008](ADR-0008-acmeassist-copilot.md)
- Purpose: a concrete catalog of "clever agents" to design *with* the platform, so the guardrail
  model is validated against real use-cases from day one.

> An **agent** here is not a separate service. It is a **scoped persona** on the one
> `AssistantEngine` (Spring AI + langgraph4j): a system prompt + a **small, explicit toolset** (a
> subset of the REST operations) + a **role ceiling** + a **trigger**. Same engine, same
> execute-as-the-user authorization (see ADR-0008 Decision 2), same write-confirmation gate
> (Decision 3). Adding an agent = adding a definition, not a deployment.

## Design principles (from multi-agent experience)

These shaped the catalog and should constrain every future agent:

1. **Scoped personas beat one god-agent.** A narrow toolset + a specific job gives far fewer
   wrong tool-calls than one assistant holding every endpoint. Each agent below owns a *small*
   set of tools.
2. **Draft, don't send. Propose, don't commit.** Anything outward-facing (an email, a message) or
   persistent (a record) is produced as a **draft/proposal** a human releases. The model never
   presses "send".
3. **Stage writes as _unverified / pending_.** New records land in a quarantined state (mirroring
   the platform's `PENDING` user and e-approval instincts) and are promoted by a human — not
   written as final. Bad extraction becomes a review item, never live data.
4. **The role ceiling is the safety net, not the prompt.** An agent can *attempt* only what the
   signed-in user could; Spring enforces it. Prompts define *intent*, authorization defines
   *limits*. Never rely on the prompt for safety.
5. **Proactive ≠ autonomous.** Event- or schedule-triggered agents have **no signed-in user**, so
   they may only **read** and **draft**. Every resulting write waits for a human to confirm it
   **under their own role**. (See *Open question: service identity*.)
6. **Compose via hand-off.** Big jobs are chains of small agents (triage → enrich → draft →
   route), each verifiable, rather than one long opaque run.
7. **Keep context small and grounded.** Feed each agent the current module/entity + only the tool
   results it needs — not the whole world. Cite the source records behind every answer.

## The two you named

| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| — | **Web-form reply drafter** | Inbound contact via web form | Drafts sensible reply options grounded in CRM context; human edits & sends | read + **draft** | WORK to send | Draft only; sending is a human action |
| — | **Unknown-sender intake** | Email from an unrecognized address | Creates an **unverified** contact + opportunity from the mail; queued for review | **write (staged)** | WORK | Records land `UNVERIFIED`; promoted by a human |

## 16 more

### CRM / Sales
| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| 1 | **Quote drafter** | User ask / an opportunity | Assembles a draft quote — line items + resolved prices (`GET /api/crm/price`) — for confirmation | write-**propose** | WORK | Confirmation card → `POST /quotes` |
| 2 | **Stale-deal chaser** | Proactive (daily) | Finds deals stagnating > N days (the design's own example) and drafts a follow-up sequence + next actions | read + **draft** | WORK | Drafts only; human sends |
| 3 | **Duplicate detective** | Proactive / on demand | Flags likely-duplicate customers/contacts, proposes a merge | read + **propose** | ADMIN | Master-data change → explicit confirm |
| 4 | **Customer-360 briefer** | Before a call/meeting | Summarizes a customer across orders, quotes, open items | **read-only** | WATCH | No writes |

### Supply / Procurement
| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| 5 | **Supplier-risk flagger** | On demand / new contract | Runs `ai.ContractIntelligence.assessRisks` over contract text + terms; proposes "flag supplier" (their example) | read + **propose** | WORK | Reuses the `ai` seam; flag needs confirm |
| 6 | **Procurement drafter** | Projected material shortfall | Drafts a procurement request (→ e-approval); human confirms | write-**propose** | WORK | Confirmation → e-approval flow |
| 7 | **Lead-time watchdog** | Proactive | Alerts when a supplier lead time threatens a committed order; proposes expedite / alternate source | read + **alert** | WATCH→WORK | Alert read-only; action proposed |

### Build / Production
| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| 8 | **Feasibility checker** | "Can we fulfill contract X?" | Calls the Build **feasibility API**, explains the scarce resource, lays out options | **read-only** | WATCH | Pure reasoning over the API |
| 9 | **BOM what-if** | On demand | Explains a bill of materials; simulates capacity impact of a hypothetical order | **read-only** | WATCH | No writes |
| 10 | **Capacity-conflict alerter** | Proactive | Flags overbooked production windows; proposes rescheduling | read + **propose** | WORK | Reschedule is a confirmed write |

### HR / Org
| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| 11 | **Deputy finder** | Someone marked absent | Uses absence + power-of-attorney + approval limits to propose the right deputy/coverage | read + **propose** | WORK | Assignment confirmed by a human |
| 12 | **Approval-router** | Order/procurement needs sign-off | Routes it to whoever may sign that amount (approval limits, minus who's absent) | read + **propose** | WORK | Routes only; the approver still decides |
| 13 | **Onboarding provisioner** | New hire created | Drafts the provisioning steps (ties to HR→Entra, ADR-0005) for admin review | **draft** | ADMIN | Draft checklist; admin executes |

### Base / cross-cutting
| # | Agent | Trigger | Does | R/W | Min role | Guardrail |
|---|-------|---------|------|-----|----------|-----------|
| 14 | **Digest agent** | Scheduled (daily/weekly) | Role-scoped digest — the design's *"Copilot erstellte die Q3-Zusammenfassung"* — drafted for review | read + **draft** | per-recipient | Read+draft only; no autonomous send |
| 15 | **Audit sentinel** | Proactive | Watches `assist_audit` + domain events for anomalies; surfaces to admins | **read-only** | ADMIN | Read-only; alerts a human |
| 16 | **In-app concierge** | User ask ("how do I…?", "explain this screen") | Grounded help using the active module + `/assist/capabilities` | **read-only** | WATCH | No domain writes |

## Coverage at a glance

- **By tier:** read-only 6 · draft 5 · propose-write 7 (nothing auto-commits).
- **By module:** CRM 6 · Supply 3 · Build 3 · HR 3 · Base 3 — every module has at least one agent.
- **By trigger:** reactive/on-demand 11 · proactive/scheduled 7 — proactive agents are all
  read-or-draft, per principle 5.
- **Reuses existing seams:** `ai.ContractIntelligence` (#5), the Build feasibility API (#8), the
  e-approval flow (#6, #12), HR absence/deputy/approval-limits (#11, #12), HR→Entra (#13).

## Reusable agent patterns

Every agent above is an instance of one of eight patterns — the transferable vocabulary for
adding more later:

1. **Brief / summarize** (read-only) — #4, #8, #9, #16
2. **Draft-don't-send** (read + draft) — web-form, #2, #13, #14
3. **Extract-to-staged** (write as `UNVERIFIED`) — unknown-sender intake
4. **Propose-write** (confirm to commit) — #1, #6, #10
5. **Reconcile / dedupe** (propose master-data change) — #3
6. **Risk / anomaly flag** — #5, #15
7. **Watch & alert** (proactive read) — #7, capacity/lead-time
8. **Route & match** (propose an assignment) — #11, #12

## Open questions this raises (add to ADR-0008)

- **Service identity for proactive agents.** Scheduled/event agents have no signed-in user. Do
  they run as a constrained **read-only service principal** that can only *stage drafts* (proposed
  default), or do we introduce an explicit "agent runs on behalf of user X" delegation with X's
  role? This gates when the proactive tiers (7 of 18) can ship.
- **Agent registry & governance.** Where are agent definitions declared (config vs. code vs. an
  admin-managed registry), and can an admin enable/disable agents per tenant/role?
- **Draft store.** Drafts (replies, digests, quotes) need somewhere to live before a human acts —
  reuse `assist_audit`, or a dedicated `assist_draft` staging table?
- **`UNVERIFIED` lifecycle.** What states does a staged record move through, who may promote it,
  and what happens to rejected ones (discard vs. keep for audit)?
- **Phasing.** Suggested: read-only agents (#4, #8, #9, #16) alongside the phase-1 slice; draft
  agents in phase 2; propose-write and staged-write agents with phase 3; proactive agents last,
  gated on service identity.
