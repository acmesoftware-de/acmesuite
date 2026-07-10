package de.acmesoftware.acmesuite.base.auth;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Generic OpenID Connect provider (any compliant IdP via its issuer/discovery URL). Declares its
 * config schema for the Admin area; the OIDC login flow is wired in a follow-up. Roles remain
 * locally assigned in Base.
 */
@Component
public class OidcAuthProvider implements AuthProvider {

    public static final String ID = "oidc";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "OpenID Connect";
    }

    @Override
    public AuthProviderKind kind() {
        return AuthProviderKind.OIDC;
    }

    @Override
    public List<ConfigField> configSchema() {
        return List.of(
                ConfigField.url("issuerUri", "Issuer URL", true),
                ConfigField.text("clientId", "Client ID", true),
                ConfigField.secret("clientSecret", "Client secret", true),
                ConfigField.url("redirectUri", "Redirect URI", true),
                ConfigField.text("scopes", "Scopes (space-separated)", false));
    }
}
