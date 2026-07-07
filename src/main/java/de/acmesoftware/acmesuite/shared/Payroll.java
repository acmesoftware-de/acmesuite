package de.acmesoftware.acmesuite.shared;

import java.util.Map;

/**
 * Port: settles the personnel cost of a pay period (ACMEhr as the source for compensation type + rate).
 * The caller provides the actual hours worked per person (timesheet from arrival→end of day or
 * home office); the adapter applies the compensation rule:
 *
 * <ul>
 *   <li>Hourly wage (HOURLY): rate × actual hours — no hours during vacation/sickness ⇒ no payment.</li>
 *   <li>Salary (SALARIED): rate × 9 h × period days — flat, independent of attendance (management/leadership).</li>
 * </ul>
 */
public interface Payroll {

    /** Result of a payroll run (amounts in EUR). */
    record PayrollRun(long totalEur, int paidCount, long salariedEur, long hourlyEur) {
    }

    /**
     * Settles the pay period.
     *
     * @param workedHoursByPerson per personId the actual hours worked in the period (for hourly wage)
     * @param periodDays          length of the pay period in days (for the salary flat rate)
     */
    PayrollRun run(Map<String, Double> workedHoursByPerson, int periodDays);
}
