package de.acmesoftware.acmesuite.base;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the ACMEbase auth ({@code acme.base.auth.*}).
 *
 * <p>Default {@code enabled=false}: the suite APIs are open — suitable for local development, and
 * keeps the existing test suite green. Set to {@code true} to enable authentication; then the
 * role rules ({@link AccessRole}) apply via a Base-issued session JWT (bearer).
 *
 * <p>Model: identity may be <em>federated</em> (Entra/OIDC provider plugins) or <em>local</em>
 * (password), but the access role is always assigned <em>locally</em> in Base by an admin.
 */
@ConfigurationProperties("acme.base.auth")
public class AuthProperties {

    /** Role enforcement on the suite APIs on/off. */
    private boolean enabled = false;

    private final Jwt jwt = new Jwt();
    private final Crypto crypto = new Crypto();
    private final Bootstrap bootstrap = new Bootstrap();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    /** Base session token (HS256). The same secret signs and validates. */
    public static class Jwt {
        /** HS256 signing secret (>= 32 bytes). Override in production. */
        private String secret;
        /** Token lifetime. */
        private Duration ttl = Duration.ofHours(12);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    /** AES-GCM envelope encryption of provider secrets at rest. */
    public static class Crypto {
        /** Master key, base64-encoded 32 bytes (AES-256). Override in production. */
        private String masterKey;

        public String getMasterKey() {
            return masterKey;
        }

        public void setMasterKey(String masterKey) {
            this.masterKey = masterKey;
        }
    }

    /** Local break-glass admin created at first startup when no ADMIN exists. */
    public static class Bootstrap {
        private String adminUsername = "admin";

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }
    }
}
