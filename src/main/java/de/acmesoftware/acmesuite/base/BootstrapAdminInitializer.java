package de.acmesoftware.acmesuite.base;

import de.acmesoftware.acmesuite.base.auth.LocalAuthProvider;
import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates a local break-glass admin at first startup when no ADMIN exists yet — the bootstrap out
 * of the chicken-and-egg (an admin is needed to assign roles, but nobody has ADMIN initially).
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code acme.base.auth.bootstrap.admin-password} is set (recommended for real deployments):
 *       that is the admin's password. Nothing secret is logged, and no forced change — the operator
 *       chose it.</li>
 *   <li>unset (zero-config dev): a random one-time password is logged once and must be changed on
 *       first login.</li>
 * </ul>
 */
@Component
class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final BaseUserRepository users;
    private final PasswordEncoder encoder;
    private final AuthProperties props;

    BootstrapAdminInitializer(BaseUserRepository users, PasswordEncoder encoder, AuthProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(AccessRole.ADMIN)) {
            return;
        }
        String username = props.getBootstrap().getAdminUsername();
        String configured = props.getBootstrap().getAdminPassword();
        boolean generated = configured == null || configured.isBlank();
        String password = generated ? randomPassword() : configured;

        BaseUser admin = new BaseUser(
                UUID.randomUUID().toString().replace("-", ""),
                username, null, "Administrator",
                AccessRole.ADMIN, UserStatus.ACTIVE, LocalAuthProvider.ID, null,
                // Force a change only for the generated temporary password.
                encoder.encode(password), generated, Instant.now());
        users.save(admin);

        if (generated) {
            log.warn("""

                    ==================================================================
                     ACMEbase bootstrap admin created (no ADMIN existed).
                       username:            {}
                       temporary password:  {}
                     You must change this password on first login.
                     Set acme.base.auth.bootstrap.admin-password to avoid the log-only
                     password in deployments where you cannot read the logs.
                    ==================================================================""",
                    username, password);
        } else {
            log.info("ACMEbase bootstrap admin '{}' created from the configured password "
                    + "(acme.base.auth.bootstrap.admin-password).", username);
        }
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
