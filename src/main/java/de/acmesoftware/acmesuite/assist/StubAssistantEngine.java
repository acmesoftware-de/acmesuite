package de.acmesoftware.acmesuite.assist;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic placeholder engine (active while {@code acme.assist.provider=stub}, the default) —
 * so the SSE transport, the web layer and CI already work before Spring AI / Ollama is wired in.
 * Streams a fixed Customer-360 brief (the Appendix-A reference agent's example), token by token,
 * with a tool call and cited sources — no model, no real data access.
 */
@Component
@ConditionalOnProperty(prefix = "acme.assist", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubAssistantEngine implements AssistantEngine {

    private static final String BRIEF =
            "Vela Robotics (Reseller, aktiv). 2 offene Angebote über €82.400. "
            + "Letzte Bestellung vor 6 Wochen. Empfehlung: heute eine Follow-up-Sequenz starten. [stub]";

    @Override
    public void converse(AssistRequest request, CallerContext caller, Consumer<AssistEvent> sink) {
        sink.accept(new AssistEvent.ToolCall("get_customer", "VELA-004"));
        for (String word : BRIEF.split(" ")) {
            sink.accept(new AssistEvent.Delta(word + " "));
        }
        sink.accept(new AssistEvent.Message(BRIEF, List.of("Kunde VELA-004", "Angebot Q-1187")));
        String conversationId = request.conversationId() == null ? "stub-conv" : request.conversationId();
        sink.accept(new AssistEvent.Done(conversationId));
    }
}
