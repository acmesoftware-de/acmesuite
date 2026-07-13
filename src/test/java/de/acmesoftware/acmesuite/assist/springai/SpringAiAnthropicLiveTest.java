package de.acmesoftware.acmesuite.assist.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Opt-in live smoke test of the hosted <b>Claude</b> provider: with {@code acme.assist.provider=claude}
 * the real {@link SpringAiAssistantEngine} drives Spring AI's ChatClient against the Anthropic API,
 * its Customer-360 tools hit the real CRM endpoints (as the user), and the answer streams over SSE.
 * Skips (does not fail) when {@code ANTHROPIC_API_KEY} is absent, so CI stays green.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "acme.assist.provider=claude",
                "spring.ai.anthropic.chat.options.model=claude-haiku-4-5-20251001"
        })
@Import(TestcontainersConfig.class)
class SpringAiAnthropicLiveTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void liveClaudeTurnStreamsOverSse() throws Exception {
        assumeTrue(keyPresent("ANTHROPIC_API_KEY"), "ANTHROPIC_API_KEY not set — skipping live Claude test");

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um den Kunden Vela Robotics?\","
                                + "\"context\":{\"module\":\"CRM\"}}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[springai-claude-live] status=" + response.statusCode() + "\n" + response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("event:message");
        assertThat(response.body()).contains("event:done");
    }

    private static boolean keyPresent(String var) {
        String value = System.getenv(var);
        return value != null && !value.isBlank() && !value.equals("not-set");
    }
}
