package de.acmesoftware.acmesuite.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * Validity period (valid-from / valid-until). {@code until == null} means "open".
 *
 * <p>Deliberately kept lean: a full bi-temporal model is not needed here — this only requires
 * business validity, not an audit-proof history.
 */
@Embeddable
public record DateRange(
        @Column(name = "valid_from") LocalDate from,
        @Column(name = "valid_until") LocalDate until) {

    public DateRange {
        if (from != null && until != null && until.isBefore(from)) {
            throw new IllegalArgumentException("valid_until liegt vor valid_from");
        }
    }

    public static DateRange openFrom(LocalDate from) {
        return new DateRange(from, null);
    }

    public boolean isActiveOn(LocalDate date) {
        boolean started = from == null || !date.isBefore(from);
        boolean notEnded = until == null || !date.isAfter(until);
        return started && notEnded;
    }
}
