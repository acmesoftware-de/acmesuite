# ADR-0008 · Appendix C — Research & sources

- Status: **Proposed** (2026-07-12) — companion to [ADR-0008](ADR-0008-acmeassist-copilot.md)
- Purpose: anchor the **evidence** behind ADR-0008's claims (esp. model selection and CPU
  feasibility), and record the **provenance** of every input, so the proposal is reviewable and
  ISO-42001-traceable (see [Appendix B](ADR-0008-acmeassist-governance.md), G2/G7).

> **How to read these sources.** The model/hardware citations were compiled via an **automated
> web-research pass (mid-2026)**. Model-hub contents, capability badges, and licenses **drift** —
> **re-verify at implementation time** and pin exact revisions. CPU throughput figures vary ±2× by
> hardware (AMX/AVX-512 is the decisive factor). Treat this appendix as a documented starting
> point, not a frozen guarantee.

## Provenance of inputs

| Input | Role in the ADR | Where it lives |
|-------|-----------------|----------------|
| **Design handoff** — `CRM Dashboard.dc.html` + `README.md` | Source of the Copilot UX (the ✦ mark, `copilotSuggest`/`copilotAnswer`, the `0 16px 48px var(--shadow)` popover, German copy) that the prototype productionizes | Embedded: [`assets/design_handoff_acmesuite/`](assets/design_handoff_acmesuite/) (from `ACMEsuite Interface Design.zip`, design team) |
| **Repo survey** — `ai/` module, `base/BaseSecurityConfig`, `frontend/themes`, registry, `useAuth`, `api/*.yaml` | Source of the architecture constraints (execute-as-user authz, class-contract, contract-first, the existing Ollama trajectory) | This repository @ `0.3.0-dev` |
| **Sibling survey** — ACMEmailtrap search | External prior art **only** (separate product) — see [ADR-0008](ADR-0008-acmeassist-copilot.md) cross-reference | `~/acmemailtrap` (separate repo) |
| **Model research** — CPU-friendly, tool-capable, German-capable Ollama models | Backs the model-selection block in Decision 4 | Sources below; raw report embedded: [`assets/model-research-2026-07.md`](assets/model-research-2026-07.md) |

## Model-selection evidence

### Generative LLM (tool-calling, CPU, German)

- **Qwen2.5 (recommended default `qwen2.5:7b`)** — library & Tools badge:
  <https://ollama.com/library/qwen2.5> · Apache-2.0 (7/14/32B):
  <https://huggingface.co/Qwen/Qwen2.5-7B/blob/main/LICENSE> · German quality (German MMLU ≈ 68):
  <https://arxiv.org/pdf/2506.04079> · ⚠️ avoid `qwen2.5:3b` (non-commercial):
  <https://huggingface.co/Qwen/Qwen2.5-3B> · 72B custom license:
  <https://huggingface.co/Qwen/Qwen2.5-72B-Instruct/blob/main/LICENSE> · RAM/CPU:
  <https://ceur-ws.org/Vol-4164/paper11.pdf>, <https://localaimaster.com/blog/ollama-model-ram-vram-table>
- **IBM Granite 3.3 / Granite 4** — <https://ollama.com/library/granite3.3> ·
  <https://ollama.com/library/granite4> · enterprise function-calling + Apache-2.0:
  <https://www.ibm.com/new/announcements/ibm-granite-4-0-hyper-efficient-high-performance-hybrid-models>,
  <https://www.ibm.com/new/announcements/ibm-granite-3-0-open-state-of-the-art-enterprise-models>
- **Qwen3** — <https://ollama.com/library/qwen3> · Apache-2.0 all sizes:
  <https://huggingface.co/Qwen/Qwen3-8B/blob/main/LICENSE> · <https://huggingface.co/Qwen/Qwen3-14B>
  (note: *thinking* mode may need disabling for the tool loop)
- **Mistral 7B / Small** — <https://ollama.com/library/mistral> ·
  <https://ollama.com/library/mistral-small> · <https://mistral.ai/news/mistral-small-3-1/> ·
  ⚠️ Ministral is **not** in the Ollama library and is non-commercial:
  <https://help.mistral.ai/en/articles/347393>,
  <https://huggingface.co/mistralai/Ministral-8B-Instruct-2410>
- **Phi-4-mini** (CPU fast-path) — <https://ollama.com/library/phi4-mini> · MIT:
  <https://huggingface.co/microsoft/Phi-3.5-mini-instruct/blob/main/LICENSE> · weak German:
  <https://www.microsoft.com/en-us/research/wp-content/uploads/2024/12/P4TechReport.pdf>

**Rejected for the default (with the reason):**

- **Gemma 2/3** — strong German (<https://arxiv.org/pdf/2506.04079>) but **no Ollama Tools badge**:
  <https://ollama.com/library/gemma3> → disqualified for the agent role.
- **Llama 3.1/3.3** — <https://ollama.com/library/llama3.1> · license not OSI-open (700M-MAU,
  attribution/naming, AUP): <https://www.llama.com/llama3_1/license/>.
- **German fine-tunes** (not in the official library / no tool template) — Teuken:
  <https://huggingface.co/openGPT-X/Teuken-7B-instruct-commercial-v0.4> · SauerkrautLM:
  <https://huggingface.co/VAGOsolutions/SauerkrautLM-v2-14b-DPO> · DiscoLM:
  <https://huggingface.co/DiscoResearch/DiscoLM_German_7b_v1>

### Embedding model (multilingual/German, CPU)

- **snowflake-arctic-embed2 (recommended)** — <https://ollama.com/library/snowflake-arctic-embed2> ·
  1024 dims, 8K ctx, explicit German, Apache-2.0:
  <https://huggingface.co/Snowflake/snowflake-arctic-embed-l-v2.0>
- **bge-m3** (MIT, hybrid dense+sparse) — <https://ollama.com/library/bge-m3> ·
  <https://huggingface.co/BAAI/bge-m3>
- **paraphrase-multilingual** (light fallback, 512-token ctx limit) —
  <https://ollama.com/library/paraphrase-multilingual>
- Runner-up (vendor namespace, DE/EN-tuned) — `jina/jina-embeddings-v2-base-de`:
  <https://ollama.com/jina/jina-embeddings-v2-base-de>
- Skipped as English-only: `nomic-embed-text`, `mxbai-embed-large`.

### CPU-only feasibility (the honest latency caveat)

- 7–8B Q4 throughput + "CPU sweet spot" — <https://ceur-ws.org/Vol-4164/paper11.pdf>
- Tool-calling latency on CPU (≈1–8 s simple, up to ≈23 s complex per call) —
  <https://markaicode.com/benchmarks/tool-cpu-benchmark/>,
  <https://lyceum.technology/magazine/tool-calling-latency-llm-inference/>
- Keep-warm / `OLLAMA_KEEP_ALIVE`, `num_ctx` — <https://docs.ollama.com/faq>

## Open items for verification (before phase-1 commit)

1. Re-verify Ollama library tags + **Tools** badges and current licenses; pin exact HF revisions.
2. Validate German quality of the chosen LLM on **real ACMEsuite CRM/HR content** (not just
   benchmarks).
3. Benchmark tool-loop latency on the **actual target server** (AMX vs. non-AMX dominates).
4. Confirm embedding dimensions/license on the HF card for the exact tag pulled.
