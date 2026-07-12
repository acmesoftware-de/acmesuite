package de.acmesoftware.acmesuite;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Provides a real Postgres instance via Testcontainers for integration tests
 * (wired via {@link ServiceConnection} to Spring Boot's datasource). Requires a running
 * container runtime (Docker/colima).
 *
 * <p>The search index runs in-memory in tests ({@code acme.search.index-dir=:memory:}, set for the
 * whole test JVM by Surefire) so cached contexts don't clash on the on-disk index lock.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:18-alpine");
    }
}
