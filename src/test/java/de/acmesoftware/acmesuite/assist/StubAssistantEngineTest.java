package de.acmesoftware.acmesuite.assist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test of the deterministic stub engine — no Spring context, no DB. Guarantees the
 * phase-1 "canned brief" contract: a leading tool call, a run of deltas that reconstruct the
 * final message, cited sources, and a terminal Done.
 */
class StubAssistantEngineTest {

    @Test
    void emitsDeterministicBriefSequence() {
        List<AssistEvent> events = new ArrayList<>();
        new StubAssistantEngine().converse(
                new AssistRequest(null, "Wie steht es um Vela Robotics?",
                        new AssistRequest.Context("CRM", "vertrieb", null)),
                "tester", events::add);

        assertThat(events).first().isInstanceOf(AssistEvent.ToolCall.class);
        assertThat(events).last().isEqualTo(new AssistEvent.Done("stub-conv"));

        AssistEvent.Message message = events.stream()
                .filter(AssistEvent.Message.class::isInstance)
                .map(AssistEvent.Message.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(message.text()).contains("Vela Robotics");
        assertThat(message.sources()).isNotEmpty();

        String assembled = events.stream()
                .filter(AssistEvent.Delta.class::isInstance)
                .map(event -> ((AssistEvent.Delta) event).text())
                .collect(Collectors.joining())
                .trim();
        assertThat(assembled).isEqualTo(message.text());
    }
}
