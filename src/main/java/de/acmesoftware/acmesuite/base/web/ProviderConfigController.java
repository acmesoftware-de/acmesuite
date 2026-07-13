package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.web.AdminViews.ProviderConfigUpsert;
import de.acmesoftware.acmesuite.base.web.AdminViews.ProviderConfigView;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin configuration of federated auth providers (ADMIN only). Secret values are write-only:
 * accepted on upsert (envelope-encrypted), never returned.
 */
@RestController
@RequestMapping("/api/base/auth/provider-configs")
public class ProviderConfigController {

    private final ProviderConfigService configs;

    public ProviderConfigController(ProviderConfigService configs) {
        this.configs = configs;
    }

    @GetMapping
    public List<ProviderConfigView> list() {
        return configs.list().stream().map(AdminViews::providerConfig).toList();
    }

    @PutMapping("/{providerId}")
    public ResponseEntity<ProviderConfigView> upsert(@PathVariable String providerId,
            @RequestBody ProviderConfigUpsert req) {
        try {
            ProviderConfigService.State state = configs.upsert(providerId, req.enabled(), req.values());
            return ResponseEntity.ok(AdminViews.providerConfig(state));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{providerId}")
    public ResponseEntity<Void> delete(@PathVariable String providerId) {
        configs.delete(providerId);
        return ResponseEntity.noContent().build();
    }
}
