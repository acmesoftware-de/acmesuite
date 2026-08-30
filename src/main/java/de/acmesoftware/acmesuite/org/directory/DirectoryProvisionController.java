package de.acmesoftware.acmesuite.org.directory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers a directory-provisioning run. Whether it only plans or actually writes is decided by
 * {@code acme.directory.dry-run}. Present only when {@code acme.directory.enabled=true} — the same
 * gate as the beans it drives.
 *
 * <p>A scheduled run belongs beside this endpoint once a deployment wants it; the endpoint is the
 * manual counterpart and the one an external orchestrator calls.
 */
@RestController
@RequestMapping("/api/integration/directory")
@ConditionalOnProperty(name = "acme.directory.enabled", havingValue = "true")
class DirectoryProvisionController {

    private final DirectoryProvisioningService service;

    DirectoryProvisionController(DirectoryProvisioningService service) {
        this.service = service;
    }

    @PostMapping("/provision")
    ProvisioningRun provision() {
        return service.provision();
    }
}
