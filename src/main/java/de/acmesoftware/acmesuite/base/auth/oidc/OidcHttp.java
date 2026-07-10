package de.acmesoftware.acmesuite.base.auth.oidc;

import java.util.Map;

/**
 * Small HTTP seam for the OIDC flow (discovery, JWKS, token exchange). Isolating the network
 * boundary behind an interface keeps {@link OidcClient} deterministically testable with a canned
 * IdP, while the crypto (id_token signature) is exercised for real.
 */
public interface OidcHttp {

    /** GET a JSON object (discovery document, JWKS). */
    Map<String, Object> getJson(String url);

    /** POST an {@code application/x-www-form-urlencoded} body and read a JSON object (token). */
    Map<String, Object> postForm(String url, Map<String, String> form);
}
