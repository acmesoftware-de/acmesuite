package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.shared.ApprovalAuthority;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** ACMEhr adapter of the {@link ApprovalAuthority}: checks the person's effective approval limit. */
@Component
public class OrgApprovalAuthority implements ApprovalAuthority {

    private final HrService hr;

    public OrgApprovalAuthority(HrService hr) {
        this.hr = hr;
    }

    @Override
    public boolean canApprove(String personKey, BigDecimal amount, String currency, LocalDate on) {
        var limit = hr.effectiveApprovalLimit(personKey, null, on).maxAmount();
        if (limit.unlimited()) {
            return true;
        }
        return limit.amount() != null && limit.amount().compareTo(amount) >= 0;
    }
}
