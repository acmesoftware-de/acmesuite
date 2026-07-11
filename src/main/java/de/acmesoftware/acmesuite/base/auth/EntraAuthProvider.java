package de.acmesoftware.acmesuite.base.auth;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Microsoft Entra ID (Azure AD) OIDC provider. Declares its config schema so an admin can set it
 * up in the Admin area; the actual OIDC redirect/callback login is wired in a follow-up. The role
 * of a user who signs in via Entra is still assigned locally in Base.
 */
@Component
public class EntraAuthProvider implements AuthProvider {

    public static final String ID = "entra";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Microsoft Entra ID";
    }

    @Override
    public AuthProviderKind kind() {
        return AuthProviderKind.OIDC;
    }

    @Override
    public List<ConfigField> configSchema() {
        return List.of(
                ConfigField.text("tenantId", "Directory (tenant) ID", true),
                ConfigField.text("clientId", "Application (client) ID", true),
                ConfigField.secret("clientSecret", "Client secret", true),
                ConfigField.url("redirectUri", "Redirect URI", true),
                ConfigField.text("allowedDomains", "Allowed email domains (comma-separated)", false));
    }
}
