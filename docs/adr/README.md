# Architecture Decision Records

Significant, hard-to-reverse decisions for ACMEsuite, one file per decision
(`ADR-NNNN-title.md`). Format: Status, Context, Decision, Alternatives, Consequences.

## Log

- [ADR-0008](ADR-0008-acmeassist-copilot.md) — ACMEassist, an in-app co-pilot (**Proposed**):
  new `assist` module, tool-use over the REST contracts executed *as the signed-in user*,
  Spring AI + langgraph4j (default self-hosted Ollama; Claude/others optional) over SSE,
  bottom-anchored panel. Includes a non-functional UI prototype
  ([HTML](ADR-0008-acmeassist-prototype.html)), an
  [agent catalog appendix](ADR-0008-acmeassist-agents.md), and an
  [AI governance / ISO 42001 appendix](ADR-0008-acmeassist-governance.md).
- [ADR-0007](ADR-0007-federated-authn-local-authz.md) — Federated authentication, local
  authorization (Base-issued session JWT; provider plugins; envelope-encrypted secrets).

Earlier decisions (ADR-0003, ADR-0005, ADR-0006) are referenced inline in the code and
have not yet been backfilled as files here.
