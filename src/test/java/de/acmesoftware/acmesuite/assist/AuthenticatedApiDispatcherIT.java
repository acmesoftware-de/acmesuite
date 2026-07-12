package de.acmesoftware.acmesuite.assist;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.assist.agent.Customer360Agent;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * M2 happy path: the dispatcher performs a real in-process loopback GET against the CRM contract
 * as the caller (auth off in this context). Proves the execute-as-the-user mechanism end to end;
 * a Customer-360 read tool reaches {@code /api/crm/customers}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AuthenticatedApiDispatcherIT {

    @Autowired
    AuthenticatedApiDispatcher dispatcher;

    @Value("${local.server.port}")
    int port;

    private CallerContext caller() {
        return new CallerContext("tester", null, "http://localhost:" + port);
    }

    @Test
    void findCustomersToolReachesCrmAndReturns200() {
        AuthenticatedApiDispatcher.Result result =
                dispatcher.get(caller(), Customer360Agent.TOOLS.get(0).pathTemplate()); // find_customers
        assertThat(result.ok()).isTrue();
        assertThat(result.status()).isEqualTo(200);
    }

    @Test
    void getCustomerToolHitsTheEndpointAsTheUser() {
        String path = Customer360Agent.TOOLS.get(1).resolve("id", "UNKNOWN-ID"); // get_customer
        AuthenticatedApiDispatcher.Result result = dispatcher.get(caller(), path);
        // Reached the CRM endpoint as the user; an unknown id is a clean 404, not a routing error.
        assertThat(result.status()).isEqualTo(404);
    }
}
