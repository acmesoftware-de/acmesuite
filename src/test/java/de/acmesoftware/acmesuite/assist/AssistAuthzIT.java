package de.acmesoftware.acmesuite.assist;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * M2 "403 for the unentitled": with auth enabled and no session, a dispatched read is denied by
 * the same Spring Security chain — proving the assistant cannot bypass authorization. It goes
 * <em>through</em> the security chain, so an unentitled caller gets 401/403, never data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "acme.base.auth.enabled=true")
@Import(TestcontainersConfig.class)
class AssistAuthzIT {

    @Autowired
    AuthenticatedApiDispatcher dispatcher;

    @Value("${local.server.port}")
    int port;

    @Test
    void unentitledCallerIsDeniedByTheSameChain() {
        CallerContext anonymous = new CallerContext("anon", null, "http://localhost:" + port);
        AuthenticatedApiDispatcher.Result result = dispatcher.get(anonymous, "/api/crm/customers");
        assertThat(result.denied()).isTrue();
    }
}
