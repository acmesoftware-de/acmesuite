package de.acmesoftware.acmesuite.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.acmesoftware.acmesuite.base.domain.BaseUser;
import de.acmesoftware.acmesuite.base.domain.BaseUserRepository;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit test of the two bootstrap-admin modes (configured password vs. generated). */
class BootstrapAdminInitializerTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AuthProperties props(String adminPassword) {
        AuthProperties p = new AuthProperties();
        p.getBootstrap().setAdminPassword(adminPassword);
        return p;
    }

    private BaseUser runAndCapture(BaseUserRepository repo, AuthProperties props) {
        new BootstrapAdminInitializer(repo, encoder, props).run(null);
        ArgumentCaptor<BaseUser> captor = ArgumentCaptor.forClass(BaseUser.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void configuredPasswordIsUsedVerbatimWithoutForcedChange() {
        BaseUserRepository repo = mock(BaseUserRepository.class);
        when(repo.existsByRole(AccessRole.ADMIN)).thenReturn(false);

        BaseUser admin = runAndCapture(repo, props("Configured-Pw-123"));

        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(AccessRole.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.isMustSetPassword()).isFalse();
        assertThat(encoder.matches("Configured-Pw-123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void blankPasswordGeneratesRandomAndForcesChange() {
        BaseUserRepository repo = mock(BaseUserRepository.class);
        when(repo.existsByRole(AccessRole.ADMIN)).thenReturn(false);

        BaseUser admin = runAndCapture(repo, props("  "));

        assertThat(admin.isMustSetPassword()).isTrue();
        assertThat(admin.getPasswordHash()).isNotBlank();
        // A random password is not the (blank) configured one.
        assertThat(encoder.matches("  ", admin.getPasswordHash())).isFalse();
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        BaseUserRepository repo = mock(BaseUserRepository.class);
        when(repo.existsByRole(AccessRole.ADMIN)).thenReturn(true);

        new BootstrapAdminInitializer(repo, encoder, props("whatever")).run(null);

        verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
