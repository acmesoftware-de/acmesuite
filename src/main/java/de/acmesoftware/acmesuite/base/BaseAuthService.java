package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.auth.LocalAuthProvider;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import de.acmesoftware.acmesuite.base.token.SessionTokenService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local authentication + password lifecycle for ACMEbase. Federated login (Entra/OIDC) is added
 * by the provider plugins; both paths converge on {@link SessionTokenService} to issue the Base
 * session token carrying the locally assigned role.
 */
@Service
public class BaseAuthService {

    private final BaseUserRepository users;
    private final PasswordEncoder encoder;
    private final SessionTokenService tokens;

    public BaseAuthService(BaseUserRepository users, PasswordEncoder encoder, SessionTokenService tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    /** Verifies local credentials; empty result = invalid (unknown user or wrong password). */
    public Optional<LoginResult> login(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }
        return users.findByUsername(username)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getPasswordHash() != null && encoder.matches(rawPassword, u.getPasswordHash()))
                .map(u -> new LoginResult(
                        tokens.issue(u.getId(), u.getRole().name(), u.getDisplayName(), u.isAuditor()),
                        u.isMustSetPassword(), u));
    }

    public Optional<BaseUser> find(String userId) {
        return users.findById(userId);
    }

    /** True while no ADMIN exists yet (the self-claim window, {@code allow-self-claim=true}). */
    public boolean needsBootstrap() {
        return !users.existsByRole(AccessRole.ADMIN);
    }

    /**
     * Claims the initial admin account with an operator-chosen password (no forced change — they
     * chose it deliberately). Succeeds exactly once: the moment any ADMIN exists (from here or the
     * log-based fallback), this permanently stops working. This is a check-then-act guard, not a
     * hardened mutual-exclusion lock — acceptable because the caller (AuthController) only exposes
     * this when {@code allow-self-claim=true}, which itself must only be enabled on instances not
     * reachable by untrusted parties before the real operator claims the account.
     */
    @Transactional
    public LoginResult claimAdmin(String username, String rawPassword) {
        if (users.existsByRole(AccessRole.ADMIN)) {
            throw new IllegalStateException("an admin already exists");
        }
        BaseUser admin = new BaseUser(UUID.randomUUID().toString().replace("-", ""),
                username, null, "Administrator", AccessRole.ADMIN, UserStatus.ACTIVE,
                LocalAuthProvider.ID, null, encoder.encode(rawPassword), false);
        admin.setAuditor(true); // the claimed superuser may view version history (AUDIT)
        users.save(admin);
        return new LoginResult(
                tokens.issue(admin.getId(), admin.getRole().name(), admin.getDisplayName(), admin.isAuditor()),
                false, admin);
    }

    /** Sets (or rotates) a local user's password and clears the must-change flag. */
    @Transactional
    public void setPassword(String userId, String newPassword) {
        BaseUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("unknown user"));
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustSetPassword(false);
        users.save(user);
    }

    public record LoginResult(String token, boolean mustSetPassword, BaseUser user) {
    }
}
