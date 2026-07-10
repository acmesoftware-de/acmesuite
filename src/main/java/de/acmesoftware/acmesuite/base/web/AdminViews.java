package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.auth.ConfigField;
import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import java.util.List;
import java.util.Map;

/** DTOs of the ACMEbase admin HTTP API (user/role management + provider configuration). */
public final class AdminViews {

    private AdminViews() {
    }

    // ── Users ──
    public record UserView(String id, String username, String email, String displayName, String role,
            String status, String authProvider) {
    }

    public record CreateUserRequest(String username, String displayName, String email, String role) {
    }

    public record CreateUserResponse(UserView user, String temporaryPassword) {
    }

    public record RoleRequest(String role) {
    }

    public record StatusRequest(String status) {
    }

    public static UserView user(BaseUser u) {
        return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(),
                u.getRole().name(), u.getStatus().name(), u.getAuthProvider());
    }

    // ── Provider configuration ──
    public record FieldView(String key, String label, String type, boolean required) {
    }

    public record ProviderConfigView(String providerId, String displayName, String kind, boolean enabled,
            boolean configured, List<FieldView> schema, Map<String, String> values,
            List<String> secretsSet) {
    }

    public record ProviderConfigUpsert(boolean enabled, Map<String, String> values) {
    }

    public static ProviderConfigView providerConfig(ProviderConfigService.State s) {
        List<FieldView> schema = s.schema().stream()
                .map(f -> new FieldView(f.key(), f.label(), f.type().name(), f.required()))
                .toList();
        return new ProviderConfigView(s.providerId(), s.displayName(), s.kind(), s.enabled(),
                s.configured(), schema, s.values(), List.copyOf(s.secretsSet()));
    }
}
