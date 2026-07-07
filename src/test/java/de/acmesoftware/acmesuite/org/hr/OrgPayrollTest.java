package de.acmesoftware.acmesuite.org.hr;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.domain.CompensationType;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** ACMEhr payroll: default compensation (management/leadership salary, rest hourly wage) + pay rule (flat rate vs. actual hours). */
@SpringBootTest
@Import(TestcontainersConfig.class)
class OrgPayrollTest {

    @Autowired
    OrgPayroll payroll;
    @Autowired
    HrService hr;

    @Test
    void seedsCompensationDefaults() {
        assertThat(hr.getEmployee("u-gf-1").orElseThrow().compType()).isEqualTo("SALARIED");
        assertThat(hr.getEmployee("u-gf-1").orElseThrow().hourlyRate()).isEqualByComparingTo("50.00");
        assertThat(hr.getEmployee("u-einkauf-lead").orElseThrow().compType()).isEqualTo("SALARIED");
        assertThat(hr.getEmployee("u-einkauf-lead").orElseThrow().hourlyRate()).isEqualByComparingTo("35.00");
        assertThat(hr.getEmployee("u-gf-1-asst").orElseThrow().compType()).isEqualTo("HOURLY");
        assertThat(hr.getEmployee("u-gf-1-asst").orElseThrow().hourlyRate()).isEqualByComparingTo("25.00");
    }

    @Test
    void salariedPaidFlatHourlyPaidForHours() {
        var base = payroll.run(Map.of(), 7);
        var withHours = payroll.run(Map.of("u-einkauf-asst", 10.0), 7); // employed hourly-wage person
        // Salary is independent of the actual hours, the hourly wage scales with them.
        assertThat(withHours.salariedEur()).isEqualTo(base.salariedEur());
        assertThat(withHours.hourlyEur() - base.hourlyEur()).isEqualTo(250); // 25 €/h × 10 h
        assertThat(base.salariedEur()).isGreaterThanOrEqualTo(6300); // ≥ 2 managing directors × 50 × 9 × 7
    }

    @Test
    void switchingToSalariedRaisesFixedPayroll() {
        long before = payroll.run(Map.of(), 7).salariedEur();
        hr.updateCompensation("u-einkauf-asst", CompensationType.SALARIED, new BigDecimal("25.00"));
        try {
            long after = payroll.run(Map.of(), 7).salariedEur();
            assertThat(after - before).isEqualTo(25 * 9 * 7); // 1575 € flat rate added
        } finally {
            hr.updateCompensation("u-einkauf-asst", CompensationType.HOURLY, new BigDecimal("25.00"));
        }
    }
}
