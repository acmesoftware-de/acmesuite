package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.shared.ContractApproved;
import de.acmesoftware.acmesuite.shared.ContractRejected;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Receives the response from the approval workflow on the hiring folder: signed → the applicant
 * is hired; rejected → a 30-day block. Tolerant of foreign references (CRM/Supply).
 */
@Component
public class HrApprovalListener {

    /** Block duration after a hiring is rejected (days). */
    private static final int BLOCK_DAYS = 30;

    private final HrService hr;

    public HrApprovalListener(HrService hr) {
        this.hr = hr;
    }

    @EventListener
    public void onContractApproved(ContractApproved e) {
        hr.hireApplicant(e.ref());
    }

    @EventListener
    public void onContractRejected(ContractRejected e) {
        hr.blockApplicant(e.ref(), e.day() + BLOCK_DAYS);
    }
}
