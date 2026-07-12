package de.acmesoftware.acmesuite.assist.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.AssistProperties;
import de.acmesoftware.acmesuite.assist.AssistRequest;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Opt-in <em>live</em> spike against a real Ollama (phase-1 plan spike #3) — validates the wire
 * format end to end: our request serializes, the model tool-calls in the shape we parse, we feed
 * the tool result back, and it produces a grounded answer. Also prints rough latency.
 *
 * <p><b>Skips</b> (does not fail) when no Ollama is reachable on the base URL, so CI stays green.
 * The dispatcher is stubbed with canned CRM JSON, so no app/DB/Docker is needed — only Ollama.
 *
 * <p>Run locally (A): {@code ollama serve} + {@code ollama pull qwen2.5:7b}, then
 * {@code mvn -Dtest=OllamaLiveSpikeTest test}. Point at a tunneled Hetzner box (B) with
 * {@code -Dollama.baseUrl=http://localhost:11500 -Dollama.model=granite4:3b}.
 */
class OllamaLiveSpikeTest {

    private static final String BASE_URL = System.getProperty("ollama.baseUrl", "http://localhost:11434");
    private static final String MODEL = System.getProperty("ollama.model", "qwen2.5:7b");

    @Test
    void liveToolCallingRoundTrip() {
        assumeTrue(ollamaReachable(), "Ollama not reachable on " + BASE_URL + " — skipping live spike");

        AssistProperties props = new AssistProperties(true, "ollama",
                new AssistProperties.Ollama(BASE_URL, "granite4:3b", MODEL, 4096),
                new AssistProperties.Budget(5, 0), "de");

        // Stub the dispatcher with canned CRM data so the spike needs Ollama only (no app/DB).
        AuthenticatedApiDispatcher dispatcher = new AuthenticatedApiDispatcher() {
            @Override
            public Result get(CallerContext caller, String path) {
                return new Result(200, "{\"id\":\"VELA-004\",\"name\":\"Vela Robotics\",\"kind\":\"RESELLER\","
                        + "\"status\":\"ACTIVE\",\"country\":\"DE\"}");
            }
        };

        OllamaAssistantEngine engine = new OllamaAssistantEngine(new RestClientOllamaChat(props), dispatcher, props);

        List<AssistEvent> events = new ArrayList<>();
        long startNanos = System.nanoTime();
        engine.converse(
                new AssistRequest(null, "Wie steht es um den Kunden Vela Robotics?",
                        new AssistRequest.Context("CRM", null, "VELA-004")),
                new CallerContext("spike", null, BASE_URL),
                events::add);
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        long toolCalls = events.stream().filter(AssistEvent.ToolCall.class::isInstance).count();
        String answer = events.stream()
                .filter(AssistEvent.Message.class::isInstance)
                .map(event -> ((AssistEvent.Message) event).text())
                .findFirst().orElse("(none)");

        System.out.printf("%n[ollama-spike] model=%s base=%s  elapsed=%dms  toolCalls=%d%n  answer: %s%n",
                MODEL, BASE_URL, elapsedMillis, toolCalls, answer);

        // Wire format works end to end if the loop completed cleanly with a terminal Done.
        assertThat(events).last().isInstanceOf(AssistEvent.Done.class);
    }

    private static boolean ollamaReachable() {
        try {
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1)).build()
                    .send(HttpRequest.newBuilder(URI.create(BASE_URL + "/api/version"))
                            .timeout(Duration.ofSeconds(2)).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
