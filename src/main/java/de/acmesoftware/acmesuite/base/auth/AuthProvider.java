package de.acmesoftware.acmesuite.base.auth;

import java.util.List;

/**
 * SPI for an authentication provider plugin. Providers are Spring beans discovered by Base; each
 * declares its identity and a config {@link ConfigField} schema so the Admin UI can render its
 * settings generically. The login mechanics differ by {@link #kind()} (local password vs. OIDC
 * redirect) and are handled by the matching Base service.
 *
 * <p>The role a user gets is never derived from the provider — it is assigned locally in Base.
 */
public interface AuthProvider {

    /** Stable id, also used as {@code base_user.auth_provider} and in config records. */
    String id();

    /** Human-readable name shown on the login screen and in the Admin UI. */
    String displayName();

    AuthProviderKind kind();

    /** Config fields the Admin UI renders. Empty for providers without external config (local). */
    default List<ConfigField> configSchema() {
        return List.of();
    }
}
