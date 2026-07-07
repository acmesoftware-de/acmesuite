package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.shared.ContractApproved;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Receives the "folder signed" feedback from the folder workflow and approves the order. */
@Component
public class CrmApprovalListener {

    private final SalesService sales;

    public CrmApprovalListener(SalesService sales) {
        this.sales = sales;
    }

    @EventListener
    public void onContractApproved(ContractApproved e) {
        sales.markApproved(e.ref(), e.approverKey());
    }
}
