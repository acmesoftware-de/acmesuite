package de.acmesoftware.acmesuite.base;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end check of the ACMEbase auth-core with authentication enabled: local login issues a
 * Base session JWT, the token authorizes reads for WATCH, write is denied for WATCH, and missing
 * or wrong credentials are rejected.
 */
@SpringBootTest(properties = "acme.base.auth.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class BaseAuthTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Autowired
    BaseUserRepository users;

    @Autowired
    PasswordEncoder encoder;

    private String seedWatchUser() {
        users.save(new BaseUser("u-watch-test-0001", "watchtester", "watch@acme.test", "Watch Tester",
                AccessRole.WATCH, UserStatus.ACTIVE, "local", null, encoder.encode("watch-pass-123"),
                false, Instant.now()));
        return "watchtester";
    }

    @Test
    void localLoginIssuesTokenAndAuthorizesReads() throws Exception {
        seedWatchUser();

        String body = mvc.perform(post("/api/base/auth/login").contentType(JSON)
                        .content("{\"username\":\"watchtester\",\"password\":\"watch-pass-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("WATCH"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        // The token identifies the user.
        mvc.perform(get("/api/base/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("watchtester"));

        // WATCH may read a suite API...
        mvc.perform(get("/api/crm/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // ...but not write.
        mvc.perform(post("/api/crm/customers").header("Authorization", "Bearer " + token)
                        .contentType(JSON).content("{\"name\":\"Nope\",\"kind\":\"DIRECT\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorizedAndBadCredentialsRejected() throws Exception {
        seedWatchUser();

        mvc.perform(get("/api/crm/customers")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/base/auth/login").contentType(JSON)
                        .content("{\"username\":\"watchtester\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
