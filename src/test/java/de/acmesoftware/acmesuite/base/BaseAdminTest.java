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
 * Admin surface with auth enabled: ADMIN manages users/roles and configures federated providers;
 * secrets are stored encrypted and never returned; non-admins are forbidden.
 */
@SpringBootTest(properties = "acme.base.auth.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class BaseAdminTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    MockMvc mvc;

    @Autowired
    BaseUserRepository users;

    @Autowired
    PasswordEncoder encoder;

    private void seed(String id, String username, AccessRole role) {
        users.save(new BaseUser(id, username, username + "@acme.test", username, role, UserStatus.ACTIVE,
                "local", null, encoder.encode(username + "-pass"), false));
    }

    private String tokenFor(String username) throws Exception {
        String body = mvc.perform(post("/api/base/auth/login").contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "-pass\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void adminManagesUsersAndRoles() throws Exception {
        seed("u-admin-adm-01", "admanager", AccessRole.ADMIN);
        String admin = tokenFor("admanager");

        // Create a WORK user -> 201 with a one-time temporary password.
        String created = mvc.perform(post("/api/base/users").header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content(
                                "{\"username\":\"newbie\",\"displayName\":\"New Bie\",\"email\":\"n@acme.test\",\"role\":\"WORK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("WORK"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String newId = JsonPath.read(created, "$.user.id");

        // List includes both.
        mvc.perform(get("/api/base/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='newbie')]").exists());

        // Promote to ADMIN, then disable.
        mvc.perform(put("/api/base/users/{id}/role", newId).header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(put("/api/base/users/{id}/status", newId).header("Authorization", "Bearer " + admin)
                        .contentType(JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void nonAdminIsForbiddenOnAdminSurface() throws Exception {
        seed("u-watch-adm-01", "peeker", AccessRole.WATCH);
        String watch = tokenFor("peeker");

        mvc.perform(get("/api/base/users").header("Authorization", "Bearer " + watch))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/base/auth/provider-configs").header("Authorization", "Bearer " + watch))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerConfigStoresSecretEncryptedAndEnablesLoginOption() throws Exception {
        seed("u-admin-adm-02", "cfgadmin", AccessRole.ADMIN);
        String admin = tokenFor("cfgadmin");

        // Schema is exposed with a SECRET field for the client secret.
        mvc.perform(get("/api/base/auth/provider-configs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.providerId=='entra')]").exists())
                .andExpect(jsonPath("$[?(@.providerId=='entra')].schema[?(@.type=='SECRET')]").exists());

        // Configure + enable Entra.
        mvc.perform(put("/api/base/auth/provider-configs/{id}", "entra")
                        .header("Authorization", "Bearer " + admin).contentType(JSON)
                        .content("{\"enabled\":true,\"values\":{\"tenantId\":\"t-123\",\"clientId\":\"c-456\","
                                + "\"clientSecret\":\"sh-hh\",\"redirectUri\":\"https://app/cb\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.values.tenantId").value("t-123"))
                // Secret is recorded as set but never echoed back.
                .andExpect(jsonPath("$.secretsSet[?(@=='clientSecret')]").exists())
                .andExpect(jsonPath("$.values.clientSecret").doesNotExist());

        // Public login options now include Entra.
        mvc.perform(get("/api/base/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='entra')]").exists());
    }
}
