package de.acmesoftware.acmesuite.shared;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Port: may a person approve an amount on the reference date? Resolved via ACMEhr (approval limit
 * from power of attorney / explicit). Decouples the e-approval (CRM) from HR.
 */
public interface ApprovalAuthority {
    boolean canApprove(String personKey, BigDecimal amount, String currency, LocalDate on);
}
