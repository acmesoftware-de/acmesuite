package de.acmesoftware.acmesuite.org.entra;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers the HR-&gt;Entra provisioning. Whether it is only planned or actually written is decided by
 * {@code acme.entra.dry-run}. Only present when {@code acme.entra.enabled=true}.
 */
@RestController
@RequestMapping("/api/integration/entra")
@ConditionalOnProperty(name = "acme.entra.enabled", havingValue = "true")
class EntraProvisionController {

    private final EntraProvisioner provisioner;

    EntraProvisionController(EntraProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @PostMapping("/provision")
    ProvisionSummary provision() {
        return provisioner.provision();
    }
}
