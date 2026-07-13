package de.acmesoftware.acmesuite.base;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
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
 * The orthogonal AUDIT capability (ADR-0010): only holders may read version history, an admin
 * grants/revokes it, and an ADMIN <em>without</em> AUDIT is still forbidden — it is not part of
 * the access-role hierarchy.
 */
@SpringBootTest(properties = "acme.base.auth.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class BaseAuditRoleTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;
    @Autowired
    BaseUserRepository users;
    @Autowired
    PasswordEncoder encoder;

    private BaseUser seed(String id, String username, AccessRole role, boolean auditor) {
        BaseUser u = new BaseUser(id, username, username + "@acme.test", username, role,
                UserStatus.ACTIVE, "local", null, encoder.encode(username + "-pass"), false);
        u.setAuditor(auditor);
        return users.save(u);
    }

    private String tokenFor(String username) throws Exception {
        String body = mvc.perform(post("/api/base/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "-pass\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void auditorMayReadHistoryOthersMayNot() throws Exception {
        // A WATCH user WITH the AUDIT capability.
        seed("u-audit-01", "auditor", AccessRole.WATCH, true);
        // An ADMIN WITHOUT the AUDIT capability.
        seed("u-admin-noaudit", "plainadmin", AccessRole.ADMIN, false);

        String auditor = tokenFor("auditor");
        String plainAdmin = tokenFor("plainadmin");

        // The AUDIT holder sees the history endpoint.
        mvc.perform(get("/api/base/users/{id}/history", "u-admin-noaudit")
                        .header("Authorization", "Bearer " + auditor))
                .andExpect(status().isOk());

        // A powerful ADMIN without AUDIT does not — history is orthogonal to the access role.
        mvc.perform(get("/api/base/users/{id}/history", "u-audit-01")
                        .header("Authorization", "Bearer " + plainAdmin))
                .andExpect(status().isForbidden());

        // The `me` view exposes the capability so the UI can offer the history affordance.
        String me = mvc.perform(get("/api/base/auth/me").header("Authorization", "Bearer " + auditor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((Boolean) JsonPath.read(me, "$.auditor")).isTrue();
    }

    @Test
    void adminGrantsAndRevokesAuditCapability() throws Exception {
        seed("u-admin-grant", "grantadmin", AccessRole.ADMIN, false);
        seed("u-target", "target", AccessRole.WORK, false);
        String admin = tokenFor("grantadmin");

        // Grant.
        mvc.perform(put("/api/base/users/{id}/auditor", "u-target")
                        .header("Authorization", "Bearer " + admin).contentType(JSON)
                        .content("{\"auditor\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditor").value(true))
                // The last-change stamp is visible to everyone (ADR-0010); the version number is not.
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        // Revoke.
        mvc.perform(put("/api/base/users/{id}/auditor", "u-target")
                        .header("Authorization", "Bearer " + admin).contentType(JSON)
                        .content("{\"auditor\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditor").value(false));
    }
}
