package de.acmesoftware.acmesuite.assist.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Opt-in live end-to-end test of the migrated engine: with {@code acme.assist.provider=ollama} the
 * real {@link SpringAiAssistantEngine} drives Spring AI's ChatClient against a local Ollama, its
 * Customer-360 tools hit the real CRM endpoints (as the user), and the answer streams over SSE.
 * Skips (does not fail) when Ollama is unreachable, so CI stays green.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "acme.assist.provider=ollama")
@Import(TestcontainersConfig.class)
class SpringAiOllamaLiveTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void liveOllamaTurnStreamsOverSse() throws Exception {
        assumeTrue(ollamaReachable(), "Ollama not reachable on :11434 — skipping live migration test");

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um den Kunden Vela Robotics?\","
                                + "\"context\":{\"module\":\"CRM\"}}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[springai-ollama-live] status=" + response.statusCode()
                + "\n" + response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("event:message");
        assertThat(response.body()).contains("event:done");
    }

    private static boolean ollamaReachable() {
        try {
            HttpResponse<String> version = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1)).build()
                    .send(HttpRequest.newBuilder(URI.create("http://localhost:11434/api/version"))
                            .timeout(Duration.ofSeconds(2)).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            return version.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
