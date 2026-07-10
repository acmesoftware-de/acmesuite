package de.acmesoftware.acmesuite.base.auth;

import org.springframework.stereotype.Component;

/**
 * The built-in local provider: username + password against the Base directory. Always present so
 * the break-glass admin can sign in even when no federated provider is configured or reachable.
 * Has no external config.
 */
@Component
public class LocalAuthProvider implements AuthProvider {

    public static final String ID = "local";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Local account";
    }

    @Override
    public AuthProviderKind kind() {
        return AuthProviderKind.LOCAL;
    }
}
