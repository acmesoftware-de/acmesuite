package de.acmesoftware.acmesuite.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Monetary amount as a value object (amount + currency). Embedded, among other things, for power-of-attorney
 * limits and cost-center budgets.
 */
@Embeddable
public record Money(
        @Column(name = "amount") BigDecimal amount,
        @Column(name = "currency", length = 3) String currency) {

    public Money {
        if (amount != null && currency == null) {
            throw new IllegalArgumentException("Amount without currency");
        }
        if (currency != null) {
            // throws if not a valid ISO-4217 code
            Currency.getInstance(currency);
        }
    }

    public static Money euro(String amount) {
        return new Money(new BigDecimal(amount), "EUR");
    }

    /** Marker for "unlimited" (e.g. general power of attorney without an amount limit). */
    public static Money unlimited() {
        return new Money(null, null);
    }

    public boolean isUnlimited() {
        return amount == null;
    }
}
