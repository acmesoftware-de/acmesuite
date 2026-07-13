package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.domain.BaseUser;

/** DTOs of the ACMEbase auth HTTP API. */
public final class BaseViews {

    private BaseViews() {
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String token, boolean mustSetPassword, MeView user) {
    }

    // `auditor` = the orthogonal AUDIT capability (ADR-0010) so the UI can offer version history.
    public record MeView(String id, String username, String email, String displayName, String role,
            String status, boolean auditor) {
    }

    public record PasswordRequest(String newPassword) {
    }

    /** A login option offered on the sign-in screen (local or a federated provider). */
    public record ProviderView(String id, String displayName, String kind) {
    }

    public static MeView me(BaseUser u) {
        return new MeView(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(),
                u.getRole().name(), u.getStatus().name(), u.isAuditor());
    }
}
