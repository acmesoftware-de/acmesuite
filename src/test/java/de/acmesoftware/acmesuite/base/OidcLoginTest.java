package de.acmesoftware.acmesuite.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcHttp;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcLoginService;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcLoginService.CallbackResult;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * OIDC authorization-code flow with a mocked IdP at the HTTP boundary — discovery, JWKS and token
 * responses are stubbed, while the id_token is signed and verified for real (Nimbus RSA). Covers
 * the authorization URL, provisioning an unknown identity as PENDING, and issuing a Base token for
 * a known ACTIVE user.
 */
@SpringBootTest(properties = "acme.base.auth.enabled=true")
@Import(TestcontainersConfig.class)
@Transactional
class OidcLoginTest {

    private static final String CID = "client-123";
    private static final String REDIRECT = "https://app.test/api/base/auth/oidc/callback";
    private static final RSAKey RSA = generateKey();

    @MockitoBean
    OidcHttp http;

    @Autowired
    OidcLoginService oidc;
    @Autowired
    ProviderConfigService configs;
    @Autowired
    BaseUserRepository users;
    @Autowired
    JwtDecoder jwtDecoder;

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String configureIdp(String issuer) {
        configs.upsert("oidc", true, Map.of(
                "issuerUri", issuer, "clientId", CID, "clientSecret", "s3cr3t", "redirectUri", REDIRECT),
                "test");
        Map<String, Object> discovery = Map.of(
                "issuer", issuer,
                "authorization_endpoint", issuer + "/authorize",
                "token_endpoint", issuer + "/token",
                "jwks_uri", issuer + "/jwks");
        when(http.getJson(issuer + "/.well-known/openid-configuration")).thenReturn(discovery);
        when(http.getJson(issuer + "/jwks")).thenReturn(new JWKSet(RSA.toPublicJWK()).toJSONObject());
        return issuer;
    }

    private void stubToken(String issuer, String nonce, String subject) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).audience(CID).subject(subject)
                .claim("email", "alice@test").claim("name", "Alice Test").claim("nonce", nonce)
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(RSA.getKeyID()).build(),
                claims);
        jwt.sign(new RSASSASigner(RSA));
        when(http.postForm(eq(issuer + "/token"), anyMap()))
                .thenReturn(Map.of("id_token", jwt.serialize(), "access_token", "x"));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> out = new HashMap<>();
        String q = url.substring(url.indexOf('?') + 1);
        for (String pair : q.split("&")) {
            int i = pair.indexOf('=');
            out.put(pair.substring(0, i), URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    @Test
    void startBuildsAuthorizationUrl() {
        String issuer = configureIdp("https://idp.test/start");
        String url = oidc.startLogin("oidc");
        assertThat(url).startsWith(issuer + "/authorize");
        Map<String, String> q = query(url);
        assertThat(q.get("response_type")).isEqualTo("code");
        assertThat(q.get("client_id")).isEqualTo(CID);
        assertThat(q.get("redirect_uri")).isEqualTo(REDIRECT);
        assertThat(q.get("scope")).contains("openid");
        assertThat(q.get("state")).isNotBlank();
        assertThat(q.get("nonce")).isNotBlank();
    }

    @Test
    void unknownIdentityIsProvisionedPending() throws Exception {
        String issuer = configureIdp("https://idp.test/pending");
        Map<String, String> q = query(oidc.startLogin("oidc"));
        stubToken(issuer, q.get("nonce"), "subject-unknown");

        CallbackResult result = oidc.handleCallback("the-code", q.get("state"));

        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.token()).isNull();
        BaseUser provisioned = users.findByAuthProviderAndExternalSubject("oidc", "subject-unknown").orElseThrow();
        assertThat(provisioned.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(provisioned.getEmail()).isEqualTo("alice@test");
    }

    @Test
    void knownActiveIdentityGetsSessionToken() throws Exception {
        String issuer = configureIdp("https://idp.test/active");
        String subject = "subject-active";
        users.save(new BaseUser(UUID.randomUUID().toString().replace("-", ""), null, "alice@test",
                "Alice", AccessRole.WORK, UserStatus.ACTIVE, "oidc", subject, null, false, Instant.now()));

        Map<String, String> q = query(oidc.startLogin("oidc"));
        stubToken(issuer, q.get("nonce"), subject);

        CallbackResult result = oidc.handleCallback("the-code", q.get("state"));

        assertThat(result.status()).isEqualTo("authed");
        Jwt token = jwtDecoder.decode(result.token());
        assertThat(token.getClaimAsString("role")).isEqualTo("WORK");
    }
}
