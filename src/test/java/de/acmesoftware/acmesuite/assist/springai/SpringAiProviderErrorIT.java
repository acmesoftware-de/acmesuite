package de.acmesoftware.acmesuite.assist.springai;

import static org.assertj.core.api.Assertions.assertThat;

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
 * A provider failure must degrade gracefully: the SSE stream ends with {@code event:error} +
 * {@code event:done} at HTTP 200, never a 500. Deterministic and offline — the OpenAI base URL
 * points at a dead port, so the very first call is refused. No API key or external network needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "acme.assist.provider=openai",
                "spring.ai.openai.api-key=test",
                "spring.ai.openai.base-url=http://localhost:1"
        })
@Import(TestcontainersConfig.class)
class SpringAiProviderErrorIT {

    @Value("${local.server.port}")
    int port;

    @Test
    void providerFailureStreamsErrorEventNot500() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um Vela Robotics?\",\"context\":{\"module\":\"CRM\"}}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("event:error");
        assertThat(response.body()).contains("event:done");
    }
}
