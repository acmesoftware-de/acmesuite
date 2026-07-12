# On-prem Ollama model research — raw report (mid-2026)

> **Provenance.** Verbatim output of the automated web-research pass that backs the model-selection
> block in [ADR-0008 Decision 4](../ADR-0008-acmeassist-copilot.md) and
> [Appendix C](../ADR-0008-acmeassist-research.md). Captured 2026-07-12. **Fast-moving** — Ollama
> library contents, capability badges, and licenses drift; re-verify and pin exact revisions at
> implementation time. Kept as-is for traceability (ISO 42001 evidence); not maintained.

---

Every claim is sourced. Fast-moving facts flagged. Ollama library listings and licenses change — re-verify at deploy time and pin the exact HF revision.

## A) Generative LLM shortlist (tool-calling, CPU-friendly, good German)

Disqualification rule applied: a model without a **Tools** capability badge on its `ollama.com/library` page is out for the generative role. That immediately removes **Gemma 2/3, Phi-4 (14B), and Phi-3.5** — none carry the Tools badge (Gemma3 has vision only). Source: https://ollama.com/library/gemma3, https://ollama.com/library/phi4, https://ollama.com/library/phi3.5. This is a notable finding: Gemma is one of the best small models for German (Gemma-2-9B German ARC 69.7, https://arxiv.org/pdf/2506.04079) but Ollama does not mark it tool-capable, so it's disqualified for the LLM slot here.

Ranked shortlist of what actually runs on Ollama **with** tool-calling:

### 1. Qwen2.5-7B / 14B — top pick
- Tags/sizes: `qwen2.5:7b`, `qwen2.5:14b` (family: 0.5/1.5/3/7/14/32/72b). Source: https://ollama.com/library/qwen2.5
- Tool-calling: **Yes** — Tools badge present; widely regarded strong small tool-caller. Source: https://ollama.com/library/qwen2.5
- German: **best-in-class for size** — German MMLU 68.2 (5-shot), MMLU-Pro 30.3, both above Gemma-2-9B and well above Llama-3.1-8B. Source: https://arxiv.org/pdf/2506.04079
- License: **Apache-2.0** for 0.5/1.5/7/14/32B (no MAU cap). Source: https://huggingface.co/Qwen/Qwen2.5-7B/blob/main/LICENSE. Avoid `qwen2.5:3b` (qwen-research, **non-commercial**, license history churn — pin commit) and `72b` (custom license, 100M-MAU clause). Sources: https://huggingface.co/Qwen/Qwen2.5-3B, https://huggingface.co/Qwen/Qwen2.5-72B-Instruct/blob/main/LICENSE
- RAM at Q4: 7B ≈ 3.7 GB weights (~5–7 GB with runtime/KV); 14B ≈ 9–10 GB. Sources: https://ceur-ws.org/Vol-4164/paper11.pdf, https://localaimaster.com/blog/ollama-model-ram-vram-table

### 2. IBM Granite 3.3-8B / Granite 4 — cleanest enterprise option
- Tags: `granite3.3:2b`, `granite3.3:8b`; `granite4:350m/1b/3b` (+ hybrid `-h` and MoE `7b-a1b`, `32b-a9b` variants). Sources: https://ollama.com/library/granite3.3, https://ollama.com/library/granite4
- Tool-calling: **Yes** — Tools badge; IBM positions Granite explicitly for enterprise function-calling/JSON. Sources: https://ollama.com/library/granite3.3, https://www.ibm.com/new/announcements/ibm-granite-4-0-hyper-efficient-high-performance-hybrid-models
- German: officially one of 12 supported languages; independent German benchmark numbers are scarce (flag). Source: https://ollama.com/library/granite3.3
- License: **Apache-2.0**, no MAU cap — the cleanest commercial story alongside Qwen3. Source: https://www.ibm.com/new/announcements/ibm-granite-3-0-open-state-of-the-art-enterprise-models
- RAM at Q4: 8B ≈ 5 GB weights; the small Granite 4 (1–3B, MoE) variants are attractive for the CPU fast-path.

### 3. Qwen3-8B / 14B — newest, fully Apache
- Tags: `qwen3:8b`, `qwen3:14b` (family up to 235B MoE). Source: https://ollama.com/library/qwen3
- Tool-calling: **Yes** — Tools badge; also carries a **thinking** badge (hybrid reasoning), which can add latency and change tool-call formatting — worth testing, and consider disabling thinking for the agent loop. Source: https://ollama.com/library/qwen3
- German: officially 100+/119 languages incl. German; strong multilingual, but public German-specific numbers are thinner than Qwen2.5 (flag). Source: https://huggingface.co/Qwen/Qwen3-14B
- License: **Apache-2.0 across all sizes** (Qwen deliberately simplified vs 2.5). Source: https://huggingface.co/Qwen/Qwen3-8B/blob/main/LICENSE

### 4. Mistral 7B / Mistral Small (22B/24B)
- Tags: `mistral:7b` (v0.3), `mistral-small:22b`/`24b`. Sources: https://ollama.com/library/mistral, https://ollama.com/library/mistral-small
- Tool-calling: **Yes** — Tools badge; but Mistral 7B v0.3 function-calling is noted to rely on raw-mode/correct chat template, i.e. less turnkey than Qwen/Granite — verify. Source: https://ollama.com/library/mistral
- German: officially supported; empirically fine but not a German leader at 7B. Source: https://mistral.ai/news/mistral-small-3-1/
- License: **Apache-2.0** for Mistral 7B and Mistral Small open weights. **Ministral is NOT here** — no official `ollama.com/library/ministral` (404), and Ministral is Mistral Research License / non-commercial. Sources: https://help.mistral.ai/en/articles/347393, https://huggingface.co/mistralai/Ministral-8B-Instruct-2410

### 5. Phi-4-mini (3.8B) — CPU fast-path only
- Tag: `phi4-mini:3.8b`. **Only** Phi variant with a Tools badge (plain Phi-4 and Phi-3.5 are not tool-badged). Source: https://ollama.com/library/phi4-mini
- Tool-calling: **Yes**. License: **MIT** (fully permissive). Source: https://huggingface.co/microsoft/Phi-3.5-mini-instruct/blob/main/LICENSE
- German: works but **weak point** — Microsoft states Phi-4 is primarily English-trained with worse non-English performance. Source: https://www.microsoft.com/en-us/research/wp-content/uploads/2024/12/P4TechReport.pdf. Use for the low-latency small-model tier, not as the primary German reasoner.

### Notes on the disqualified / German-tuned candidates
- **Llama 3.1/3.3**: tool-capable (`llama3.1:8b`, `llama3.3:70b`) but (a) empirically weakest small model on German reasoning, and (b) Meta Community License is **not OSI-open** — 700M-MAU clause, "Built with Llama" attribution, naming rule, acceptable-use policy, gated download. Needs legal review; avoid as the bundled default. Sources: https://ollama.com/library/llama3.1, https://www.llama.com/llama3_1/license/
- **Teuken-7B, SauerkrautLM, DiscoLM German**: none are in the **official** Ollama library — only community GGUF uploads (no tool-calling template guarantees). Teuken has no tool-calling; DiscoLM's is "very experimental"; SauerkrautLM-v2-14b (Qwen2.5-14B base) is the only one with real function-calling but is community-hosted. Given Qwen2.5-14B already gives the strong German backbone with a first-party Ollama tool template, the German fine-tunes don't earn their operational risk for the default. Sources: https://huggingface.co/openGPT-X/Teuken-7B-instruct-commercial-v0.4, https://huggingface.co/VAGOsolutions/SauerkrautLM-v2-14b-DPO, https://huggingface.co/DiscoResearch/DiscoLM_German_7b_v1

**The sweet spot:** 7–8B at Q4 is the documented "sweet spot for CPU chatbots" (https://ceur-ws.org/Vol-4164/paper11.pdf). Below that (3–4B) German and tool-reliability degrade; above it (14B) latency on CPU gets rough. Qwen2.5-7B hits the best German-per-parameter with reliable tools and clean Apache licensing.

## B) Embedding shortlist (multilingual/German, CPU-friendly, in the official library)

Key finding: `multilingual-e5` and `jina-v3` are **not** in the official `ollama.com/library` (only vendor/community namespaces); `nomic-embed-text` and `mxbai-embed-large` are **English-only** — skip both for German.

### 1. snowflake-arctic-embed2 — best explicit German, in library
- Tag: `snowflake-arctic-embed2:568m` (`:latest`), ~1.2 GB. Source: https://ollama.com/library/snowflake-arctic-embed2
- Dims: **1024** (Matryoshka-truncatable to 256, <3% loss); context **8192** (RoPE); 74 languages. Source: https://huggingface.co/Snowflake/snowflake-arctic-embed-l-v2.0
- German: **explicitly listed and benchmarked** (CLEF/MIRACL over DE/EN/ES/FR/IT; MTEB Retrieval 55.6). Source: Snowflake engineering blog + HF card
- License: **Apache-2.0**. CPU: ~568M, runs fine.

### 2. bge-m3 — best all-round multilingual, in library
- Tag: `bge-m3:567m` (`:latest`), ~1.2 GB. Source: https://ollama.com/library/bge-m3
- Dims: **1024**; context **8192**; 100+ languages incl. German; hybrid dense+sparse+ColBERT retrieval in one model. Source: https://huggingface.co/BAAI/bge-m3
- License: **MIT**. Pick this if you want hybrid dense/sparse retrieval.

### 3. paraphrase-multilingual — lightweight fallback, in library
- Tag: `paraphrase-multilingual:278m` (`:latest`), ~563 MB. Source: https://ollama.com/library/paraphrase-multilingual
- Dims: **768**; context **only 512 tokens** (real RAG limitation); 50+ languages incl. German; older 2019-era SBERT, weaker than the two above. License: **Apache-2.0**. Use only if RAM/CPU is very tight.

Runner-up if you'll accept a vendor namespace: `jina/jina-embeddings-v2-base-de` (768 dims, 8192 ctx, Apache-2.0, purpose-built German↔English, ~323 MB) — excellent for a specifically DE/EN corpus, but not `/library`-official. Source: https://ollama.com/jina/jina-embeddings-v2-base-de

## CPU-only feasibility for a multi-step tool agent

Honest answer: **marginal, and it degrades with each step.** Single-shot chat/RAG at 7–8B Q4 is fine (~2–4 s on a modern Xeon). But an agent loop pays a multi-second prefill over the tool schema on *every* LLM call, then decodes at memory-bandwidth-bound speeds.
- Throughput (7–8B Q4, measured): ~45–50 tok/s on a top Sapphire Rapids Xeon 8480+ (AMX), but only ~14 tok/s on EPYC 9354 and ~8 tok/s on an older Xeon. Prefill is ~13× faster per token than decode but still costs a few seconds per call. Sources: https://ceur-ws.org/Vol-4164/paper11.pdf, https://markaicode.com/benchmarks/tool-cpu-benchmark/
- Tool-calling latency measured at ~1–8 s (simple) up to ~23 s (complex) per call on CPU. A 3–5 step turn realistically lands in the tens of seconds on typical server CPUs — painful for snappy interactive use, tolerable for async/internal workflows. Source: https://lyceum.technology/magazine/tool-calling-latency-llm-inference/
- Numbers vary ±2× with CPU generation (AMX/AVX-512 presence is decisive), memory-channel population, and thread pinning.

Mitigations (in priority order): keep the model warm (`OLLAMA_KEEP_ALIVE=-1` — cold reload measured 8.4 s vs 0.7 s warm); use the smallest viable model / two-tier routing (Phi-4-mini or Granite-4-3B for easy turns, Qwen2.5-7B for hard); cap context (`num_ctx`, default 4096) to shrink prefill and KV; threads = physical cores (not SMT); stay at Q4_K_M (≈2× faster than Q8, and the quality sweet spot); populate all memory channels (bandwidth ≈ throughput); minimize agent steps and prefer parallel tool calls. Sources: https://docs.ollama.com/faq, https://ceur-ws.org/Vol-4164/paper11.pdf, https://markaicode.com/benchmarks/tool-cpu-benchmark/

## Recommended default bundled pairing

**`qwen2.5:7b` (LLM) + `snowflake-arctic-embed2:568m` (embeddings).**

Rationale: Qwen2.5-7B is the best-documented German quality per parameter among small models (German MMLU 68.2), carries Ollama's Tools badge with a reliable first-party template for Spring AI/langgraph4j function calling, is Apache-2.0 with no MAU cap, and sits in the 7–8B CPU sweet spot (~3.7 GB Q4 weights, ~5–7 GB resident). Arctic-Embed-2 is the only in-library embedding model with explicit, benchmarked German retrieval plus Apache-2.0, 8K context, and Matryoshka truncation to control vector-store size. Together they fit comfortably on an 8–16 GB CPU server and are both cleanly commercial. If you need a lighter fast-path, drop the LLM to `granite4` (3B, Apache, tools) for easy turns; if you want hybrid sparse+dense retrieval, swap the embedder to `bge-m3` (MIT).

### Top uncertainty flags
- Ollama library contents and capability badges change frequently — re-verify Tools badges and available sizes at deploy time.
- Pin exact HF revisions for licensing — Qwen2.5-3B in particular has license churn (currently non-commercial; avoid it).
- Granite and Qwen3 have sparse independent German-specific benchmark numbers; validate on your own German CRM/HR content before committing.
- CPU latency figures span ±2× by hardware; benchmark on your actual target server (AMX vs non-AMX is the biggest single factor).
- Embedding dimensions/licenses aren't shown on Ollama pages — confirm against the HF model card for the exact tag you pull.
