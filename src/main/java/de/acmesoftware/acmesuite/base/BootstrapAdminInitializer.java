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
 * of the chicken-and-egg (an admin is needed to assign roles, but nobody has ADMIN initially). A
 * random one-time password is logged once; the admin is forced to change it on first login.
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
        String tempPassword = randomPassword();
        BaseUser admin = new BaseUser(
                UUID.randomUUID().toString().replace("-", ""),
                username, null, "Administrator",
                AccessRole.ADMIN, UserStatus.ACTIVE, LocalAuthProvider.ID, null,
                encoder.encode(tempPassword), true, Instant.now());
        users.save(admin);

        log.warn("""

                ==================================================================
                 ACMEbase bootstrap admin created (no ADMIN existed).
                   username:            {}
                   temporary password:  {}
                 You must change this password on first login.
                ==================================================================""",
                username, tempPassword);
    }

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
