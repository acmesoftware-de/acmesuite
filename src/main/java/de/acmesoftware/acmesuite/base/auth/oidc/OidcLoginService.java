package de.acmesoftware.acmesuite.base.auth.oidc;

import de.acmesoftware.acmesuite.base.AuthProperties;
import de.acmesoftware.acmesuite.base.FederatedIdentityService;
import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcClient.Identity;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcClient.Metadata;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcClient.OidcException;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import de.acmesoftware.acmesuite.base.token.SessionTokenService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Drives the OIDC authorization-code flow entirely server-side (the client secret never reaches
 * the browser). Login is started by building the IdP authorization URL; on callback the code is
 * exchanged, the id_token verified, the identity linked to a local user, and — only for an ACTIVE
 * user — a Base session token issued. The {@code state} is a short-lived signed JWT carrying the
 * provider id and nonce, so no server-side session is needed.
 */
@Service
public class OidcLoginService {

    private static final String STATE_PURPOSE = "oidc-state";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProviderConfigService providerConfigs;
    private final OidcClient oidc;
    private final FederatedIdentityService identities;
    private final SessionTokenService sessionTokens;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthProperties.Oidc props;

    public OidcLoginService(ProviderConfigService providerConfigs, OidcClient oidc,
            FederatedIdentityService identities, SessionTokenService sessionTokens,
            JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, AuthProperties authProps) {
        this.providerConfigs = providerConfigs;
        this.oidc = oidc;
        this.identities = identities;
        this.sessionTokens = sessionTokens;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.props = authProps.getOidc();
    }

    /** Builds the IdP authorization URL to redirect the browser to. */
    public String startLogin(String providerId) {
        Map<String, String> config = enabledConfig(providerId);
        Metadata meta = oidc.discover(oidc.issuer(providerId, config));
        String nonce = randomToken();
        String state = signState(providerId, nonce);
        return oidc.authorizationUrl(meta, require(config, "clientId"), require(config, "redirectUri"),
                scopes(config), state, nonce);
    }

    /** Handles the IdP callback; returns an issued token (ACTIVE) or PENDING. */
    public CallbackResult handleCallback(String code, String state) {
        StateClaims sc = verifyState(state);
        Map<String, String> config = enabledConfig(sc.providerId());
        Metadata meta = oidc.discover(oidc.issuer(sc.providerId(), config));
        String idToken = oidc.exchangeCodeForIdToken(meta, code, require(config, "redirectUri"),
                require(config, "clientId"), require(config, "clientSecret"));
        Identity id = oidc.verifyIdToken(idToken, meta.issuer(), require(config, "clientId"), sc.nonce(),
                meta.jwksUri());
        if (id.subject() == null) {
            throw new OidcException("id_token has no subject");
        }
        BaseUser user = identities.findOrProvision(sc.providerId(), id.subject(), id.email(),
                id.displayName());
        if (user.getStatus() == UserStatus.ACTIVE) {
            String token = sessionTokens.issue(user.getId(), user.getRole().name(),
                    user.getDisplayName(), user.isAuditor());
            return CallbackResult.authed(token);
        }
        return CallbackResult.pending();
    }

    public String postLoginUrl() {
        return props.getPostLoginUrl();
    }

    private Map<String, String> enabledConfig(String providerId) {
        return providerConfigs.resolvedConfig(providerId)
                .orElseThrow(() -> new OidcException("provider not enabled: " + providerId));
    }

    private String scopes(Map<String, String> config) {
        Set<String> scopes = new LinkedHashSet<>();
        for (String s : props.getDefaultScopes().split("\\s+")) {
            if (!s.isBlank()) {
                scopes.add(s);
            }
        }
        String extra = config.get("scopes");
        if (extra != null) {
            for (String s : extra.split("\\s+")) {
                if (!s.isBlank()) {
                    scopes.add(s);
                }
            }
        }
        return String.join(" ", scopes);
    }

    private String signState(String providerId, String nonce) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("acmebase")
                .issuedAt(now)
                .expiresAt(now.plus(props.getStateTtl()))
                .subject(providerId)
                .claim("purpose", STATE_PURPOSE)
                .claim("nonce", nonce)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private StateClaims verifyState(String state) {
        try {
            Jwt jwt = jwtDecoder.decode(state);
            if (!STATE_PURPOSE.equals(jwt.getClaimAsString("purpose"))) {
                throw new OidcException("invalid state");
            }
            return new StateClaims(jwt.getSubject(), jwt.getClaimAsString("nonce"));
        } catch (OidcException e) {
            throw e;
        } catch (Exception e) {
            throw new OidcException("invalid or expired state", e);
        }
    }

    private static String require(Map<String, String> config, String key) {
        String v = config.get(key);
        if (v == null || v.isBlank()) {
            throw new OidcException("provider config missing '" + key + "'");
        }
        return v;
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record StateClaims(String providerId, String nonce) {
    }

    /** Outcome of a callback: an issued session token, or PENDING (awaiting role assignment). */
    public record CallbackResult(String status, String token) {
        public static CallbackResult authed(String token) {
            return new CallbackResult("authed", token);
        }

        public static CallbackResult pending() {
            return new CallbackResult("pending", null);
        }
    }
}
