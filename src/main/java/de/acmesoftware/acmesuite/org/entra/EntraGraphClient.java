package de.acmesoftware.acmesuite.org.entra;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.Optional;

/**
 * Thin MS-Graph client for user provisioning ({@code https://graph.microsoft.com/v1.0}).
 * Idempotency is the caller's responsibility ({@link EntraProvisioner}); this client only wraps the
 * four calls lookup/create/update/setManager and the Bearer token per request.
 */
class EntraGraphClient {

    private final RestClient graph;
    private final EntraTokenSource token;

    EntraGraphClient(RestClient graph, EntraTokenSource token) {
        this.graph = graph;
        this.token = token;
    }

    /** Object id for the UPN, or empty on 404 (idempotency key = UPN). */
    Optional<String> findUserId(String userPrincipalName) {
        try {
            GraphUserRef u = graph.get().uri("/users/{upn}", userPrincipalName)
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

    /** Creates a user and returns their Entra {@code oid}. */
    String create(Map<String, Object> body) {
        GraphUserRef u = graph.post().uri("/users")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(GraphUserRef.class);
        if (u == null || u.id() == null) {
            throw new IllegalStateException("Entra create returned no id");
        }
        return u.id();
    }

    /** Updates the mutable attributes of a user. */
    void update(String oid, Map<String, Object> body) {
        graph.patch().uri("/users/{id}", oid)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toBodilessEntity();
    }

    /** Sets the manager relationship ({@code manager/$ref}). */
    void setManager(String oid, String managerOid) {
        Map<String, Object> ref = Map.of("@odata.id", "https://graph.microsoft.com/v1.0/users/" + managerOid);
        graph.put().uri("/users/{id}/manager/$ref", oid)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).body(ref)
                .retrieve().toBodilessEntity();
    }

    private String bearer() {
        return "Bearer " + token.token();
    }

    /** From Graph responses we read only the {@code id}; Jackson ignores the remaining fields. */
    record GraphUserRef(String id) {
    }
}
