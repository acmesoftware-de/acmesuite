package de.acmesoftware.acmesuite.assist;

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
 * Integration test of the SSE endpoint over a real HTTP server: POST a turn and read the streamed
 * body until the server completes the emitter. Proves the phase-1 slice end to end with the stub
 * engine (auth is off by default). Uses the JDK HTTP client to avoid SSE/async test flakiness.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AssistSseIT {

    @Value("${local.server.port}")
    int port;

    @Test
    void streamsCannedBriefOverSse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um Vela Robotics?\",\"context\":{\"module\":\"CRM\"}}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Vela Robotics");
        assertThat(response.body()).contains("event:message");
    }
}
