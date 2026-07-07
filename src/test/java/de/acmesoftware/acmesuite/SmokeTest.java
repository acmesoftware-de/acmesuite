package de.acmesoftware.acmesuite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The Spring context (ACMEbase + persistence) starts against a real Postgres. */
@SpringBootTest
@Import(TestcontainersConfig.class)
class SmokeTest {

    @Test
    void contextLoads() {
    }
}
