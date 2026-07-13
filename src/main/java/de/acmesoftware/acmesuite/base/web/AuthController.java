package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.AuthProperties;
import de.acmesoftware.acmesuite.base.BaseAuthService;
import de.acmesoftware.acmesuite.base.auth.AuthProvider;
import de.acmesoftware.acmesuite.base.auth.AuthProviderKind;
import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.web.BaseViews.BootstrapStatusView;
import de.acmesoftware.acmesuite.base.web.BaseViews.ClaimAdminRequest;
import de.acmesoftware.acmesuite.base.web.BaseViews.LoginRequest;
import de.acmesoftware.acmesuite.base.web.BaseViews.LoginResponse;
import de.acmesoftware.acmesuite.base.web.BaseViews.MeView;
import de.acmesoftware.acmesuite.base.web.BaseViews.PasswordRequest;
import de.acmesoftware.acmesuite.base.web.BaseViews.ProviderView;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ACMEbase auth HTTP API: login, current user, self-service password, login options. */
@RestController
@RequestMapping("/api/base/auth")
public class AuthController {

    /** Minimum length for a self-chosen password. */
    private static final int MIN_PASSWORD_LENGTH = 10;

    private final BaseAuthService auth;
    private final List<AuthProvider> providers;
    private final ProviderConfigService providerConfigs;
    private final AuthProperties authProps;

    public AuthController(BaseAuthService auth, List<AuthProvider> providers,
            ProviderConfigService providerConfigs, AuthProperties authProps) {
        this.auth = auth;
        this.providers = providers;
        this.providerConfigs = providerConfigs;
        this.authProps = authProps;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password())
                .map(r -> ResponseEntity.ok(new LoginResponse(r.token(), r.mustSetPassword(),
                        BaseViews.me(r.user()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Whether the self-claim screen should replace the normal login form: only when
     * {@code acme.base.auth.bootstrap.allow-self-claim=true} AND no admin exists yet.
     */
    @GetMapping("/bootstrap-status")
    public BootstrapStatusView bootstrapStatus() {
        boolean needsSetup = authProps.getBootstrap().isAllowSelfClaim() && auth.needsBootstrap();
        return new BootstrapStatusView(needsSetup);
    }

    /**
     * Claims the initial admin account with an operator-chosen password. 404s unless self-claim
     * is enabled and still needed (see {@link #bootstrapStatus()}) — same shape as a disabled
     * feature, so it doesn't leak whether an admin already exists to an unauthenticated caller.
     */
    @PostMapping("/claim-admin")
    public ResponseEntity<LoginResponse> claimAdmin(@RequestBody ClaimAdminRequest req) {
        if (!authProps.getBootstrap().isAllowSelfClaim() || !auth.needsBootstrap()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String pw = req.password();
        if (pw == null || pw.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().build();
        }
        String username = (req.username() == null || req.username().isBlank())
                ? authProps.getBootstrap().getAdminUsername() : req.username().trim();
        try {
            var result = auth.claimAdmin(username, pw);
            return ResponseEntity.ok(new LoginResponse(result.token(), result.mustSetPassword(),
                    BaseViews.me(result.user())));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /** Login options for the sign-in screen: local always, federated only when enabled. */
    @GetMapping("/providers")
    public List<ProviderView> providers() {
        var enabled = providerConfigs.enabledProviderIds();
        return providers.stream()
                .filter(p -> p.kind() == AuthProviderKind.LOCAL || enabled.contains(p.id()))
                .map(p -> new ProviderView(p.id(), p.displayName(), p.kind().name()))
                .toList();
    }

    @GetMapping("/me")
    public ResponseEntity<MeView> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return auth.find(jwt.getSubject())
                .map(u -> ResponseEntity.ok(BaseViews.me(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(@AuthenticationPrincipal Jwt jwt, @RequestBody PasswordRequest req) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String pw = req.newPassword();
        if (pw == null || pw.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().build();
        }
        auth.setPassword(jwt.getSubject(), pw);
        return ResponseEntity.noContent().build();
    }
}
