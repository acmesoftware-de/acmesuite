package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import de.acmesoftware.acmesuite.base.token.SessionTokenService;
import java.time.Instant;
import java.util.Optional;
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
                        tokens.issue(u.getId(), u.getRole().name(), u.getDisplayName()),
                        u.isMustSetPassword(), u));
    }

    public Optional<BaseUser> find(String userId) {
        return users.findById(userId);
    }

    /** Sets (or rotates) a local user's password and clears the must-change flag. */
    @Transactional
    public void setPassword(String userId, String newPassword) {
        BaseUser user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("unknown user"));
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustSetPassword(false);
        user.touch(Instant.now());
        users.save(user);
    }

    public record LoginResult(String token, boolean mustSetPassword, BaseUser user) {
    }
}
