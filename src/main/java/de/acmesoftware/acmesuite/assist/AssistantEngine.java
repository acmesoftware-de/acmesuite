package de.acmesoftware.acmesuite.assist;

import java.util.function.Consumer;

/**
 * The assistant port (ADR-0008). An implementation runs one turn and pushes {@link AssistEvent}s
 * to {@code sink} as they are produced (deltas, tool calls, the final message). Deliberately a
 * port so the deterministic {@link StubAssistantEngine} and the later Spring AI + langgraph4j
 * engine are interchangeable without touching the web layer — mirroring {@code ai.ContractIntelligence}.
 *
 * <p>{@code caller} carries the identity and credentials to run tools <em>as that user</em>, so
 * authorization is enforced exactly as for a manual request (ADR-0008 Decision 2). The stub
 * performs no real access.
 */
public interface AssistantEngine {

    void converse(AssistRequest request, CallerContext caller, Consumer<AssistEvent> sink);
}
