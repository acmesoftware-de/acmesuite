package de.acmesoftware.acmesuite.assist.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.AssistProperties;
import de.acmesoftware.acmesuite.assist.AssistRequest;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.Reply;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ToolCall;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Unit test of the provider=ollama ReAct loop with a scripted chat client and a stub dispatcher —
 * no live model, no HTTP, no Spring. Verifies the loop dispatches the tool AS the user (correct
 * path) and emits ToolCall -> Message(+sources) -> Done. (The Ollama wire format itself is
 * validated against a live server in a later spike; here we pin the loop logic.)
 */
class OllamaAssistantEngineTest {

    @Test
    void reactLoopDispatchesToolAsUserThenAnswers() {
        ArrayDeque<Reply> script = new ArrayDeque<>(List.of(
                new Reply("", List.of(new ToolCall("get_customer", Map.of("id", "VELA-004")))),
                new Reply("Vela Robotics ist aktiv.", List.of())));
        OllamaChatClient chat = call -> script.poll();

        AtomicReference<String> dispatchedPath = new AtomicReference<>();
        AuthenticatedApiDispatcher dispatcher = new AuthenticatedApiDispatcher() {
            @Override
            public Result get(CallerContext caller, String path) {
                dispatchedPath.set(path);
                return new Result(200, "{\"id\":\"VELA-004\",\"name\":\"Vela Robotics\"}");
            }
        };

        AssistProperties props = new AssistProperties(true, "ollama",
                new AssistProperties.Ollama("http://localhost:11434", "granite4:3b", "qwen2.5:7b", 4096),
                new AssistProperties.Budget(5, 0), "de");

        OllamaAssistantEngine engine = new OllamaAssistantEngine(chat, dispatcher, props);
        List<AssistEvent> events = new ArrayList<>();
        engine.converse(
                new AssistRequest(null, "Wie steht es um Vela Robotics?",
                        new AssistRequest.Context("CRM", null, null)),
                new CallerContext("tester", null, "http://localhost:8080"),
                events::add);

        assertThat(dispatchedPath.get()).isEqualTo("/api/crm/customers/VELA-004");
        assertThat(events).anyMatch(AssistEvent.ToolCall.class::isInstance);
        AssistEvent.Message message = events.stream()
                .filter(AssistEvent.Message.class::isInstance)
                .map(AssistEvent.Message.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(message.text()).contains("Vela Robotics");
        assertThat(message.sources()).isNotEmpty();
        assertThat(events).last().isInstanceOf(AssistEvent.Done.class);
    }
}
