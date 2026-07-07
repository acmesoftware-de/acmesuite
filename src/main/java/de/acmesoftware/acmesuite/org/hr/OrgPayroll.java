package de.acmesoftware.acmesuite.org.hr;

import de.acmesoftware.acmesuite.org.domain.CompensationType;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.shared.Payroll;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ACMEhr adapter for the {@link Payroll} port: computes personnel costs from the persons' compensation
 * data and the actual hours reported for the period. Salary (managing director/management) is paid
 * at a flat 9 h/day across all period days, hourly wage only for hours actually worked.
 */
@Component
@Transactional(readOnly = true)
public class OrgPayroll implements Payroll {

    static final BigDecimal SALARIED_HOURS_PER_DAY = new BigDecimal("9");

    private final PersonRepository persons;

    public OrgPayroll(PersonRepository persons) {
        this.persons = persons;
    }

    @Override
    public PayrollRun run(Map<String, Double> workedHoursByPerson, int periodDays) {
        BigDecimal salaried = BigDecimal.ZERO;
        BigDecimal hourly = BigDecimal.ZERO;
        int paid = 0;
        for (Person p : persons.findAll()) {
            if (!p.isActive() || p.isApplicant()) {
                continue;
            }
            BigDecimal pay;
            if (p.getCompType() == CompensationType.SALARIED) {
                pay = p.getHourlyRate().multiply(SALARIED_HOURS_PER_DAY).multiply(BigDecimal.valueOf(periodDays));
                salaried = salaried.add(pay);
            } else {
                double hours = workedHoursByPerson.getOrDefault(p.getId(), 0.0);
                pay = p.getHourlyRate().multiply(BigDecimal.valueOf(hours));
                hourly = hourly.add(pay);
            }
            if (pay.signum() > 0) {
                paid++;
            }
        }
        return new PayrollRun(toEur(salaried.add(hourly)), paid, toEur(salaried), toEur(hourly));
    }

    private static long toEur(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
