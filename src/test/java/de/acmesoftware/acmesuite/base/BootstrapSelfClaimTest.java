package de.acmesoftware.acmesuite.base;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end check of the self-claim bootstrap flow ({@code allow-self-claim=true}): the status
 * endpoint reports {@code needsSetup}, the first claim succeeds and signs the caller in without a
 * forced password change, and any further claim attempt is rejected.
 */
@SpringBootTest(properties = {
        "acme.base.auth.enabled=true",
        "acme.base.auth.bootstrap.allow-self-claim=true"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class BootstrapSelfClaimTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Test
    void reportsNeedsSetupClaimsOnceThenLocksOut() throws Exception {
        mvc.perform(get("/api/base/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsSetup").value(true));

        mvc.perform(post("/api/base/auth/claim-admin").contentType(JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Claimed-Pw-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustSetPassword").value(false))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(get("/api/base/auth/bootstrap-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsSetup").value(false));

        mvc.perform(post("/api/base/auth/claim-admin").contentType(JSON)
                        .content("{\"username\":\"someone-else\",\"password\":\"Another-Pw-123\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAPasswordThatIsTooShort() throws Exception {
        mvc.perform(post("/api/base/auth/claim-admin").contentType(JSON)
                        .content("{\"username\":\"admin\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
