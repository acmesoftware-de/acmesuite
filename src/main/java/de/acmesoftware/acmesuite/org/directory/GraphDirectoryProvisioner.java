package de.acmesoftware.acmesuite.org.directory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * The Microsoft Graph adapter of {@link DirectoryProvisioner} (ADR-0011 §3): the first directory,
 * not the only possible shape. It owns Graph's quirks so the port and the orchestration stay free
 * of them.
 *
 * <p><b>The two quirks that must not leak upward:</b>
 * <ol>
 *   <li><b>Attributes and the password profile travel in two separate requests.</b> They hang on
 *       two different permissions ({@code User.ReadWrite.All} vs. {@code User-PasswordProfile.
 *       ReadWrite.All}). Sent in one PATCH, a directory that has not granted the password
 *       permission rejects the WHOLE call with 403 — and the harmless attributes, the work mail
 *       among them, never land. This was a real, repeated failure; here it is structural: attributes
 *       in {@link #updateAttributes}, the password alone in {@link #setInitialPassword}.</li>
 *   <li><b>On create the password is inline</b>, because the create call requires one.</li>
 * </ol>
 *
 * <p>{@code mail} is set as a real attribute (not just {@code userPrincipalName}): the whole point
 * is that the person becomes reachable downstream, and the login name is not a mailbox.
 */
class GraphDirectoryProvisioner implements DirectoryProvisioner {

    private static final Logger log = LoggerFactory.getLogger(GraphDirectoryProvisioner.class);

    private final RestClient graph;
    /** The bearer token per request -- a supplier so tests need no login round-trip. */
    private final Supplier<String> bearerToken;

    GraphDirectoryProvisioner(RestClient graph, Supplier<String> bearerToken) {
        this.graph = graph;
        this.bearerToken = bearerToken;
    }

    @Override
    public Optional<String> findObjectId(String loginName) {
        try {
            GraphUserRef u = graph.get().uri("/users/{upn}", loginName)
                    .header("Authorization", bearer())
                    .retrieve().body(GraphUserRef.class);
            return u == null ? Optional.empty() : Optional.ofNullable(u.id());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public String create(DirectoryPerson person, String initialPassword) {
        Map<String, Object> body = attributes(person);
        // Create-only fields: the login name, its local part, and — required at creation — the
        // password inline (the one place attributes and password share a request, by Graph's rule).
        body.put("userPrincipalName", person.loginName());
        body.put("mailNickname", localPart(person.loginName()));
        body.put("passwordProfile", Map.of(
                "forceChangePasswordNextSignIn", false,
                "password", initialPassword));
        GraphUserRef u = graph.post().uri("/users")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(GraphUserRef.class);
        if (u == null || u.id() == null) {
            throw new IllegalStateException("Graph create returned no id");
        }
        return u.id();
    }

    @Override
    public void updateAttributes(String objectId, DirectoryPerson person) {
        // NO passwordProfile here — see the class comment. mail rides with the attributes.
        graph.patch().uri("/users/{id}", objectId)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(attributes(person))
                .retrieve().toBodilessEntity();
    }

    @Override
    public boolean setInitialPassword(String objectId, String password) {
        try {
            graph.patch().uri("/users/{id}", objectId)
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("passwordProfile", Map.of(
                            "forceChangePasswordNextSignIn", false,
                            "password", password)))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 403) {
                // The password permission is not granted. Attributes already landed in their own
                // request — the account is usable, only its password stays as the directory had it.
                log.warn("directory: password permission refused for {} (attributes were written)", objectId);
                return false;
            }
            throw e;
        }
    }

    @Override
    public void disable(String objectId) {
        graph.patch().uri("/users/{id}", objectId)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("accountEnabled", false))
                .retrieve().toBodilessEntity();
    }

    @Override
    public void addToGroup(String objectId, String groupName) {
        String groupId = groupIdByName(groupName);
        Map<String, Object> ref = Map.of("@odata.id",
                "https://graph.microsoft.com/v1.0/directoryObjects/" + objectId);
        try {
            graph.post().uri("/groups/{gid}/members/$ref", groupId)
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).body(ref)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Already a member -> Graph answers 400 "already exist". Idempotent: not an error.
            HttpStatusCode s = e.getStatusCode();
            if (s.value() != 400 || !e.getResponseBodyAsString().contains("already exist")) {
                throw e;
            }
        }
    }

    /** The mutable, permission-cheap attributes — the set both create and update share. */
    private static Map<String, Object> attributes(DirectoryPerson p) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("accountEnabled", p.active());
        b.put("displayName", p.displayName());
        b.put("givenName", p.givenName());
        b.put("surname", p.surname());
        // The deliverable address as a real attribute -- the reason this whole seam exists.
        if (notBlank(p.mail())) {
            b.put("mail", p.mail());
        }
        if (notBlank(p.employeeId())) {
            b.put("employeeId", p.employeeId());
        }
        if (notBlank(p.jobTitle())) {
            b.put("jobTitle", p.jobTitle());
        }
        if (notBlank(p.department())) {
            b.put("department", p.department());
        }
        return b;
    }

    private String groupIdByName(String name) {
        String filter = "displayName eq '" + name.replace("'", "''") + "'";
        GraphGroupPage page = graph.get()
                .uri(b -> b.path("/groups").queryParam("$filter", filter).queryParam("$select", "id").build())
                .header("Authorization", bearer())
                .retrieve().body(GraphGroupPage.class);
        if (page == null || page.value() == null || page.value().isEmpty()) {
            throw new IllegalStateException("directory group not found: " + name);
        }
        return page.value().get(0).id();
    }

    private static String localPart(String loginName) {
        int at = loginName.indexOf('@');
        return at < 0 ? loginName : loginName.substring(0, at);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String bearer() {
        return "Bearer " + bearerToken.get();
    }

    record GraphUserRef(String id) {
    }

    record GraphGroupPage(java.util.List<GraphUserRef> value) {
    }
}
