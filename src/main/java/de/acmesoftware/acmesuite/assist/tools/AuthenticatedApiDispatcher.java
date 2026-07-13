package de.acmesoftware.acmesuite.assist.tools;

import de.acmesoftware.acmesuite.assist.CallerContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Executes a read call against the suite's own REST API <em>as the signed-in user</em> — an
 * in-process loopback that forwards the caller's {@code Authorization} header, so authorization
 * runs through the exact same Spring Security chain as a manual request (ADR-0008 Decision 2).
 *
 * <p>Consequence: the assistant can never exceed the caller's role. An unentitled call comes back
 * {@code 401/403} here, byte-for-byte as it would for the user directly — never fabricated data.
 * This is why the assistant goes <em>through</em> the HTTP surface, not around it (API-first,
 * ADR-0006). Phase 1 is read-only; write dispatch (with confirmation) arrives in phase 3.
 */
@Component
public class AuthenticatedApiDispatcher {

    /** The outcome of a dispatched call: the HTTP status and the raw response body. */
    public record Result(int status, String body) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        /** True when the same security chain denied the call (unauthenticated or wrong role). */
        public boolean denied() {
            return status == 401 || status == 403;
        }
    }

    /** {@code GET path} on the caller's own server, carrying the caller's credentials. */
    public Result get(CallerContext caller, String path) {
        try {
            var response = RestClient.create().get()
                    .uri(caller.baseUrl() + path)
                    .headers(headers -> {
                        if (caller.authorization() != null) {
                            headers.set("Authorization", caller.authorization());
                        }
                    })
                    .retrieve()
                    .toEntity(String.class);
            return new Result(response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException e) {
            return new Result(e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }
}
