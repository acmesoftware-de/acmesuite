package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.auth.oidc.OidcLoginService;
import de.acmesoftware.acmesuite.base.auth.oidc.OidcLoginService.CallbackResult;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints of the OIDC redirect flow. {@code /start} sends the browser to the IdP;
 * {@code /callback} exchanges the code server-side and hands the browser back to the frontend with
 * the Base session token (or an error) in the URL fragment.
 */
@RestController
@RequestMapping("/api/base/auth/oidc")
public class OidcController {

    private static final Logger log = LoggerFactory.getLogger(OidcController.class);

    private final OidcLoginService oidc;

    public OidcController(OidcLoginService oidc) {
        this.oidc = oidc;
    }

    @GetMapping("/{providerId}/start")
    public ResponseEntity<Void> start(@PathVariable String providerId) {
        try {
            return redirect(oidc.startLogin(providerId));
        } catch (RuntimeException e) {
            log.warn("OIDC start failed for provider '{}': {}", providerId, e.getMessage());
            return redirect(frontend("oidc_error=config"));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null) {
            log.warn("OIDC callback returned error: {}", error);
            return redirect(frontend("oidc_error=idp"));
        }
        if (code == null || state == null) {
            return redirect(frontend("oidc_error=error"));
        }
        try {
            CallbackResult result = oidc.handleCallback(code, state);
            if ("authed".equals(result.status())) {
                return redirect(frontend("token=" + result.token()));
            }
            return redirect(frontend("oidc_error=pending"));
        } catch (RuntimeException e) {
            log.warn("OIDC callback failed: {}", e.getMessage());
            return redirect(frontend("oidc_error=error"));
        }
    }

    private String frontend(String fragment) {
        String base = oidc.postLoginUrl();
        return base + (base.contains("#") ? "&" : "#") + fragment;
    }

    private static ResponseEntity<Void> redirect(String target) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
