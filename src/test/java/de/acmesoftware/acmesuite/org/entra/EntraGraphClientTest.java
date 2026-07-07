package de.acmesoftware.acmesuite.org.entra;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Graph plumbing against a mocked HTTP server (no real tenant). */
class EntraGraphClientTest {

    private static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private final EntraTokenSource token = () -> "tok";

    @Test
    void findUserId404ReturnsEmpty() {
        RestClient.Builder b = RestClient.builder().baseUrl(GRAPH);
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        EntraGraphClient c = new EntraGraphClient(b.build(), token);

        // RestClient encodes the @ in the path to %40 (Graph accepts it that way).
        server.expect(requestTo(GRAPH + "/users/j.jefa%40acme-group.io")).andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer tok"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(c.findUserId("j.jefa@acme-group.io")).isEmpty();
        server.verify();
    }

    @Test
    void createSendsAttributesAndReadsId() {
        RestClient.Builder b = RestClient.builder().baseUrl(GRAPH);
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        EntraGraphClient c = new EntraGraphClient(b.build(), token);

        server.expect(requestTo(GRAPH + "/users")).andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer tok"))
                .andExpect(jsonPath("$.userPrincipalName").value("j.jefa@acme-group.io"))
                .andExpect(jsonPath("$.accountEnabled").value(true))
                .andExpect(jsonPath("$.employeeId").value("u-gf-1"))
                .andExpect(jsonPath("$.passwordProfile.forceChangePasswordNextSignIn").value(false))
                .andExpect(jsonPath("$.mail").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"oid-new\"}", MediaType.APPLICATION_JSON));

        String oid = c.create(Map.of(
                "userPrincipalName", "j.jefa@acme-group.io",
                "accountEnabled", true,
                "employeeId", "u-gf-1",
                "passwordProfile", Map.of("forceChangePasswordNextSignIn", false, "password", "Ac1!0123456789abcdef")));

        assertThat(oid).isEqualTo("oid-new");
        server.verify();
    }

    @Test
    void setManagerSendsOdataRef() {
        RestClient.Builder b = RestClient.builder().baseUrl(GRAPH);
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        EntraGraphClient c = new EntraGraphClient(b.build(), token);

        server.expect(requestTo(GRAPH + "/users/oid-1/manager/$ref")).andExpect(method(PUT))
                .andExpect(jsonPath("$['@odata.id']").value(GRAPH + "/users/oid-2"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        c.setManager("oid-1", "oid-2");
        server.verify();
    }
}
