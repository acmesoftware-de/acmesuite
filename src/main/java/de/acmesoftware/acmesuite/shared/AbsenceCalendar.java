package de.acmesoftware.acmesuite.shared;

import java.time.LocalDate;
import java.util.Map;

/**
 * Port: provides the absences effective on a given day from ACMEhr (data source). The adapter
 * maps a day index → calendar date (configured start day).
 */
public interface AbsenceCalendar {

    /** Calendar date of the day index (day 0 = start date). */
    LocalDate dateOf(long simDay);

    /** Absences effective on the day: person key → type ("VACATION"/"SICK"/…). */
    Map<String, String> absencesOnDay(long simDay);
}
