package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.auth.LocalAuthProvider;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin management of the Base user directory (ADMIN only): list users, create local accounts, and
 * assign the local access role / account status. Role assignment lives here — never taken from an
 * IdP claim.
 */
@Service
public class UserAdminService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BaseUserRepository users;
    private final PasswordEncoder encoder;

    public UserAdminService(BaseUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public List<BaseUser> list() {
        return users.findAllByOrderByCreatedAtAsc();
    }

    /** Creates a local user with a one-time temporary password (must be changed on first login). */
    @Transactional
    public Created createLocalUser(String username, String displayName, String email, AccessRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username required");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new DuplicateUsernameException(username);
        }
        String tempPassword = randomPassword();
        BaseUser user = new BaseUser(newId(), username, email, displayName, role, UserStatus.ACTIVE,
                LocalAuthProvider.ID, null, encoder.encode(tempPassword), true);
        users.save(user);
        return new Created(user, tempPassword);
    }

    @Transactional
    public Optional<BaseUser> setRole(String userId, AccessRole role) {
        return users.findById(userId).map(u -> {
            u.setRole(role);
            return users.save(u);
        });
    }

    @Transactional
    public Optional<BaseUser> setStatus(String userId, UserStatus status) {
        return users.findById(userId).map(u -> {
            u.setStatus(status);
            return users.save(u);
        });
    }

    /** Grants or revokes the AUDIT capability (may view version history; ADR-0010). */
    @Transactional
    public Optional<BaseUser> setAuditor(String userId, boolean auditor) {
        return users.findById(userId).map(u -> {
            u.setAuditor(auditor);
            return users.save(u);
        });
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record Created(BaseUser user, String temporaryPassword) {
    }

    /** Signals a username clash (mapped to HTTP 409). */
    public static class DuplicateUsernameException extends RuntimeException {
        public DuplicateUsernameException(String username) {
            super("username already exists: " + username);
        }
    }
}
