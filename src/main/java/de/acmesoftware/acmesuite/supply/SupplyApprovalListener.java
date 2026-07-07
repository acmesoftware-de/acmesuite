package de.acmesoftware.acmesuite.supply;

import de.acmesoftware.acmesuite.shared.ContractApproved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** "Purchasing folder signed" from the bustle → approve procurement (no-op for a foreign reference). */
@Component
public class SupplyApprovalListener {

    private final SupplyService supply;

    public SupplyApprovalListener(SupplyService supply) {
        this.supply = supply;
    }

    @EventListener
    public void onContractApproved(ContractApproved e) {
        supply.markApproved(e.ref(), e.approverKey());
    }
}
