package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.BaseAuthService;
import de.acmesoftware.acmesuite.base.auth.AuthProvider;
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

    public AuthController(BaseAuthService auth, List<AuthProvider> providers) {
        this.auth = auth;
        this.providers = providers;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password())
                .map(r -> ResponseEntity.ok(new LoginResponse(r.token(), r.mustSetPassword(),
                        BaseViews.me(r.user()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Login options for the sign-in screen (local + configured federated providers). */
    @GetMapping("/providers")
    public List<ProviderView> providers() {
        return providers.stream()
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
