package de.acmesoftware.acmesuite.org.domain;

/**
 * Compensation type of a person (switchable in ACMEhr).
 *
 * <ul>
 *   <li>{@link #HOURLY} — hourly wage: the hours actually worked are paid (timesheet from
 *       arrival→end of work or home office). No pay for vacation/sickness.</li>
 *   <li>{@link #SALARIED} — salary: a flat 9 h per day is paid, every day — regardless of
 *       vacation, sickness, or weekend. Standard for management and leadership.</li>
 * </ul>
 */
public enum CompensationType {
    HOURLY,
    SALARIED
}
