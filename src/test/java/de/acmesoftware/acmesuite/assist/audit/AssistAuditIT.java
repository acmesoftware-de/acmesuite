package de.acmesoftware.acmesuite.assist.audit;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * G2: every turn leaves one audit row (provider, model, tools, outcome, hash). Uses the stub
 * provider so no model is needed. Auth is off in this context, so the user is "anonymous".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "acme.assist.provider=stub")
@Import(TestcontainersConfig.class)
class AssistAuditIT {

    @Autowired
    AssistAuditRepository repository;

    @Value("${local.server.port}")
    int port;

    @Test
    void everyTurnIsAuditedWithProviderToolsAndHash() throws Exception {
        long before = repository.count();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/base/assist/messages"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"message\":\"Wie steht es um Vela Robotics?\",\"context\":{\"module\":\"CRM\"}}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        AssistAudit row = awaitLatest(before);
        assertThat(row.getProvider()).isEqualTo("stub");
        assertThat(row.getModel()).isEqualTo("stub");
        assertThat(row.getOutcome()).isEqualTo("ok");
        assertThat(row.getAgent()).isEqualTo("customer-360");
        assertThat(row.getTools()).contains("get_customer");
        assertThat(row.getHash()).isNotBlank();
    }

    // The audit row is written right after the SSE stream completes (async finally) — poll briefly.
    private AssistAudit awaitLatest(long before) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (repository.count() > before) {
                return repository.findTopByOrderByIdDesc().orElseThrow();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("no audit row written within timeout");
    }
}
