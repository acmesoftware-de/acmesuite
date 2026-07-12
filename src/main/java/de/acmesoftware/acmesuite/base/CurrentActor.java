package de.acmesoftware.acmesuite.base;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The identity to stamp on writes / revisions — the signed-in user, or {@code "system"}. */
public final class CurrentActor {

    public static final String SYSTEM = "system";

    private CurrentActor() {
    }

    public static String current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        String name = auth.getName();
        return name == null || name.isBlank() ? SYSTEM : name;
    }
}
