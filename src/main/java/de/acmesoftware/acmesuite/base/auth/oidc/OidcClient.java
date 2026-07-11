package de.acmesoftware.acmesuite.base.auth.oidc;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.acmesoftware.acmesuite.base.auth.EntraAuthProvider;
import de.acmesoftware.acmesuite.base.auth.OidcAuthProvider;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Minimal OpenID Connect authorization-code client. Handles both the Entra provider (issuer
 * derived from the tenant id) and the generic OIDC provider (issuer from config). Discovery
 * documents are cached per issuer. The id_token signature is verified for real against the IdP's
 * JWKS; only the network calls go through the {@link OidcHttp} seam.
 */
@Component
public class OidcClient {

    private final OidcHttp http;
    private final Map<String, Metadata> discoveryCache = new ConcurrentHashMap<>();

    public OidcClient(OidcHttp http) {
        this.http = http;
    }

    /** OIDC issuer for a provider, from its stored config. */
    public String issuer(String providerId, Map<String, String> config) {
        if (EntraAuthProvider.ID.equals(providerId)) {
            String tenant = require(config, "tenantId");
            return "https://login.microsoftonline.com/" + tenant + "/v2.0";
        }
        if (OidcAuthProvider.ID.equals(providerId)) {
            return stripTrailingSlash(require(config, "issuerUri"));
        }
        throw new OidcException("provider is not OIDC-capable: " + providerId);
    }

    /** Loads + caches the discovery metadata for an issuer. */
    public Metadata discover(String issuer) {
        return discoveryCache.computeIfAbsent(issuer, iss -> {
            Map<String, Object> doc = http.getJson(iss + "/.well-known/openid-configuration");
            return new Metadata(
                    str(doc, "issuer"),
                    str(doc, "authorization_endpoint"),
                    str(doc, "token_endpoint"),
                    str(doc, "jwks_uri"));
        });
    }

    public String authorizationUrl(Metadata meta, String clientId, String redirectUri, String scope,
            String state, String nonce) {
        return UriComponentsBuilder.fromUriString(meta.authorizationEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("response_mode", "query")
                .build()
                .encode()
                .toUriString();
    }

    /** Exchanges the authorization code for tokens; returns the raw id_token (JWT). */
    public String exchangeCodeForIdToken(Metadata meta, String code, String redirectUri,
            String clientId, String clientSecret) {
        Map<String, Object> token = http.postForm(meta.tokenEndpoint(), Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri,
                "client_id", clientId,
                "client_secret", clientSecret));
        Object idToken = token.get("id_token");
        if (idToken == null) {
            throw new OidcException("token response has no id_token");
        }
        return idToken.toString();
    }

    /** Verifies the id_token (signature via JWKS, issuer, audience, expiry, nonce) and extracts identity. */
    public Identity verifyIdToken(String idToken, String issuer, String clientId, String nonce,
            String jwksUri) {
        try {
            SignedJWT jwt = SignedJWT.parse(idToken);
            JWKSet jwks = JWKSet.parse(http.getJson(jwksUri));
            String kid = jwt.getHeader().getKeyID();
            RSAKey key = (RSAKey) (kid != null ? jwks.getKeyByKeyId(kid)
                    : jwks.getKeys().isEmpty() ? null : jwks.getKeys().get(0));
            if (key == null) {
                throw new OidcException("no matching signing key in JWKS");
            }
            if (!jwt.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
                throw new OidcException("id_token signature invalid");
            }
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            if (!issuer.equals(c.getIssuer())) {
                throw new OidcException("id_token issuer mismatch");
            }
            if (c.getAudience() == null || !c.getAudience().contains(clientId)) {
                throw new OidcException("id_token audience mismatch");
            }
            if (c.getExpirationTime() == null || c.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new OidcException("id_token expired");
            }
            if (nonce != null && !nonce.equals(c.getStringClaim("nonce"))) {
                throw new OidcException("id_token nonce mismatch");
            }
            String subject = c.getStringClaim("oid"); // Entra: stable per-tenant object id.
            if (subject == null) {
                subject = c.getSubject();
            }
            String email = c.getStringClaim("email");
            if (email == null) {
                email = c.getStringClaim("preferred_username");
            }
            return new Identity(subject, email, c.getStringClaim("name"));
        } catch (OidcException e) {
            throw e;
        } catch (Exception e) {
            throw new OidcException("id_token validation failed: " + e.getMessage(), e);
        }
    }

    private static String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (v == null || v.isBlank()) {
            throw new OidcException("provider config missing '" + key + "'");
        }
        return v;
    }

    private static String str(Map<String, Object> doc, String key) {
        Object v = doc.get(key);
        if (v == null) {
            throw new OidcException("discovery document missing '" + key + "'");
        }
        return v.toString();
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** OIDC discovery metadata (the subset we use). */
    public record Metadata(String issuer, String authorizationEndpoint, String tokenEndpoint,
            String jwksUri) {
    }

    /** Identity extracted from a verified id_token. */
    public record Identity(String subject, String email, String displayName) {
    }

    public static class OidcException extends RuntimeException {
        public OidcException(String message) {
            super(message);
        }

        public OidcException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
