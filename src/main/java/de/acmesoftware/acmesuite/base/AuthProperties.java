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
    private final Oidc oidc = new Oidc();

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

    public Oidc getOidc() {
        return oidc;
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
        /**
         * Initial admin password. If set (recommended for deployments), it is the admin's
         * password — nothing is logged and no forced change. If blank, a random one-time password
         * is generated and logged, and must be changed on first login.
         */
        private String adminPassword;

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }
    }

    /** Federated OIDC redirect flow (Entra / generic OIDC). */
    public static class Oidc {
        /** Where Base redirects the browser after login, carrying the session token in the URL
         *  fragment. Default same-origin; set to the frontend origin in split dev setups. */
        private String postLoginUrl = "/";
        /** Base scopes always requested (provider config may add more). */
        private String defaultScopes = "openid email profile";
        /** Lifetime of the signed OIDC state (CSRF/nonce carrier). */
        private Duration stateTtl = Duration.ofMinutes(10);

        public String getPostLoginUrl() {
            return postLoginUrl;
        }

        public void setPostLoginUrl(String postLoginUrl) {
            this.postLoginUrl = postLoginUrl;
        }

        public String getDefaultScopes() {
            return defaultScopes;
        }

        public void setDefaultScopes(String defaultScopes) {
            this.defaultScopes = defaultScopes;
        }

        public Duration getStateTtl() {
            return stateTtl;
        }

        public void setStateTtl(Duration stateTtl) {
            this.stateTtl = stateTtl;
        }
    }
}
