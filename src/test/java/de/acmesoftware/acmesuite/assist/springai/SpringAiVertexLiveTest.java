package de.acmesoftware.acmesuite.assist.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Opt-in live smoke test of Google <b>Vertex AI</b> Gemini (GCP project + Application Default
 * Credentials), as opposed to the AI-Studio API-key path in {@link SpringAiGoogleLiveTest}. Enabled
 * only when {@code GOOGLE_CLOUD_PROJECT} is set and ADC exists — otherwise it skips and the config
 * defaults keep the context in (harmless) API-key mode, so CI stays green.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "acme.assist.provider=google",
                "spring.ai.google.genai.vertex-ai=${GOOGLE_GENAI_VERTEX:false}",
                "spring.ai.google.genai.project-id=${GOOGLE_CLOUD_PROJECT:}",
                "spring.ai.google.genai.location=${GOOGLE_CLOUD_LOCATION:global}",
                "spring.ai.google.genai.chat.options.model=${GOOGLE_GENAI_MODEL:gemini-2.5-flash}"
        })
@Import(TestcontainersConfig.class)
class SpringAiVertexLiveTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void liveVertexGeminiTurnStreamsOverSse() throws Exception {
        assumeTrue(vertexConfigured(), "GOOGLE_CLOUD_PROJECT / ADC not set — skipping live Vertex test");

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um den Kunden Vela Robotics?\","
                                + "\"context\":{\"module\":\"CRM\"}}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[springai-vertex-live] status=" + response.statusCode() + "\n" + response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("event:message");
        assertThat(response.body()).contains("event:done");
    }

    private static boolean vertexConfigured() {
        String project = System.getenv("GOOGLE_CLOUD_PROJECT");
        Path adc = Path.of(System.getProperty("user.home"), ".config", "gcloud",
                "application_default_credentials.json");
        return project != null && !project.isBlank() && Files.exists(adc);
    }
}
