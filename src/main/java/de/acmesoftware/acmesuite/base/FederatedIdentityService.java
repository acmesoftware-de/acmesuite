package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps a federated identity to a local Base user. An unknown identity is provisioned as
 * {@code PENDING} (no access) so an admin can assign a role before the user can sign in — never
 * granting access from an IdP claim.
 */
@Service
public class FederatedIdentityService {

    private final BaseUserRepository users;

    public FederatedIdentityService(BaseUserRepository users) {
        this.users = users;
    }

    @Transactional
    public BaseUser findOrProvision(String providerId, String subject, String email, String displayName) {
        return users.findByAuthProviderAndExternalSubject(providerId, subject)
                .map(existing -> refresh(existing, email, displayName))
                .orElseGet(() -> users.save(new BaseUser(
                        UUID.randomUUID().toString().replace("-", ""),
                        null, email, displayName,
                        AccessRole.WATCH, UserStatus.PENDING, providerId, subject, null, false)));
    }

    private BaseUser refresh(BaseUser user, String email, String displayName) {
        boolean dirty = false;
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            dirty = true;
        }
        if (displayName != null && !displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
            dirty = true;
        }
        if (dirty) {
            users.save(user);
        }
        return user;
    }
}
