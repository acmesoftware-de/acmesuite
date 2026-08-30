package de.acmesoftware.acmesuite.org.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The Graph adapter's two quirks, pinned (ADR-0011 §3). These are the crux of the whole seam: the
 * split into two requests is exactly the failure that started this work -- a mail address that never
 * landed because it shared a call with a password the directory would not let us set.
 */
class GraphDirectoryProvisionerTest {

    private static final DirectoryPerson BETTY = new DirectoryPerson(
            "b.boss@acme-group.io", "Betty Boss", "Betty", "Boss", "b.boss@acme-group.io",
            "Geschäftsführerin", "Einkauf", "u-gf-1", true);

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GraphDirectoryProvisioner provisioner;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://graph.microsoft.com/v1.0");
        server = MockRestServiceServer.bindTo(builder).build();
        provisioner = new GraphDirectoryProvisioner(builder.build(), () -> "test-token");
    }

    @Test
    void updateWritesTheMailAttributeAndNoPasswordProfile() {
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/oid-1"))
                .andExpect(method(HttpMethod.PATCH))
                // The deliverable address rides with the attributes -- the reason this exists.
                .andExpect(jsonPath("$.mail").value("b.boss@acme-group.io"))
                .andExpect(jsonPath("$.jobTitle").value("Geschäftsführerin"))
                // And the password profile is NOT in this request: it hangs on a different permission.
                .andExpect(jsonPath("$.passwordProfile").doesNotExist())
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        provisioner.updateAttributes("oid-1", BETTY);
        server.verify();
    }

    @Test
    void thePasswordTravelsInItsOwnRequest() {
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/oid-1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.passwordProfile.password").value("secret-1"))
                // Only the password -- no attributes smuggled along.
                .andExpect(jsonPath("$.mail").doesNotExist())
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(provisioner.setInitialPassword("oid-1", "secret-1")).isTrue();
        server.verify();
    }

    @Test
    void aRefusedPasswordPermissionDoesNotFailTheRun() {
        // 403 on the password request: the attributes (in their own call) already landed, so this
        // must not throw -- it returns false, and the person keeps a usable account.
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/oid-1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(provisioner.setInitialPassword("oid-1", "secret-1")).isFalse();
        server.verify();
    }

    @Test
    void createSendsThePasswordInlineWithTheMailAttribute() {
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.userPrincipalName").value("b.boss@acme-group.io"))
                .andExpect(jsonPath("$.mail").value("b.boss@acme-group.io"))
                .andExpect(jsonPath("$.passwordProfile.password").value("init-pw"))
                .andRespond(withSuccess("{\"id\":\"new-oid\"}", MediaType.APPLICATION_JSON));

        assertThat(provisioner.create(BETTY, "init-pw")).isEqualTo("new-oid");
        server.verify();
    }

    @Test
    void findObjectIdReturnsEmptyOnNotFound() {
        server.expect(requestTo("https://graph.microsoft.com/v1.0/users/nobody%40acme-group.io"))
                .andRespond(withResourceNotFound());

        assertThat(provisioner.findObjectId("nobody@acme-group.io")).isEmpty();
        server.verify();
    }
}
