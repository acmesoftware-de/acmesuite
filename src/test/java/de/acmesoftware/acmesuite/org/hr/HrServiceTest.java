package de.acmesoftware.acmesuite.org.hr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.domain.AbsenceStatus;
import de.acmesoftware.acmesuite.org.domain.AbsenceType;
import de.acmesoftware.acmesuite.org.domain.PowerOfAttorneyType;
import de.acmesoftware.acmesuite.org.domain.SignatureRule;
import de.acmesoftware.acmesuite.org.hr.HrViews.MoneyView;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

/** ACMEhr application logic against the seeded canonical ACME org. */
@SpringBootTest
@Import(TestcontainersConfig.class)
class HrServiceTest {

    @Autowired
    HrService hr;

    @Autowired
    de.acmesoftware.acmesuite.shared.AbsenceCalendar calendar;

    @Test
    void listsEmployees() {
        assertThat(hr.listEmployees(null, null, null)).hasSize(99);
        assertThat(hr.listEmployees(null, null, "jefa"))
                .extracting(HrViews.EmployeeView::id).contains("u-gf-1");
        assertThat(hr.getEmployee("u-gf-1")).get()
                .satisfies(e -> assertThat(e.jobTitle()).isEqualTo("Geschäftsführerin"));
    }

    @Test
    void plansVacationAndReportsSick() {
        var vac = hr.createAbsence("u-einkauf-1", AbsenceType.VACATION,
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 31), "u-einkauf-lead", "Sommer");
        assertThat(vac.status()).isEqualTo("PLANNED");      // vacation starts as planned
        assertThat(vac.type()).isEqualTo("VACATION");
        assertThat(vac.substituteId()).isEqualTo("u-einkauf-lead");
        assertThat(vac.workingDays()).isEqualTo(10);         // 2 full weeks without the weekend

        var sick = hr.createAbsence("u-einkauf-1", AbsenceType.SICK,
                LocalDate.of(2026, 6, 24), LocalDate.of(2026, 6, 26), null, null);
        assertThat(sick.status()).isEqualTo("APPROVED");     // sick note reported directly

        assertThat(hr.absencesOf("u-einkauf-1")).extracting(HrViews.AbsenceView::id)
                .contains(vac.id(), sick.id());
        assertThat(hr.listAbsences(null, AbsenceType.SICK, null, null, null))
                .extracting(HrViews.AbsenceView::id).contains(sick.id());

        var approved = hr.updateAbsence(vac.id(), AbsenceStatus.APPROVED, null, null, null, null, false);
        assertThat(approved.status()).isEqualTo("APPROVED");

        hr.deleteAbsence(vac.id());
        assertThat(hr.getAbsence(vac.id())).isEmpty();
    }

    @Test
    void updatesOverlay() {
        var updated = hr.updateEmployee("u-einkauf-1", "Senior Einkäufer", null, null, null, null);
        assertThat(updated.jobTitle()).isEqualTo("Senior Einkäufer");
    }

    @Test
    void approvalLimitDerivedFromPowerThenExplicitOverrides() {
        var on = LocalDate.of(2024, 6, 1);
        // Head of Procurement: 250k limit from power of attorney.
        var derived = hr.effectiveApprovalLimit("u-einkauf-lead", "acme", on);
        assertThat(derived.source()).isEqualTo("POWER_OF_ATTORNEY");
        assertThat(derived.maxAmount().amount()).isEqualByComparingTo("250000");

        // Explicit limit overrides the derived one.
        hr.setApprovalLimit("u-einkauf-lead", null, new MoneyView(new BigDecimal("400000"), "EUR", false), null, null);
        var explicit = hr.effectiveApprovalLimit("u-einkauf-lead", null, on);
        assertThat(explicit.source()).isEqualTo("EXPLICIT");
        assertThat(explicit.maxAmount().amount()).isEqualByComparingTo("400000");
    }

    @Test
    void grantsAndRevokesPowerOfAttorney() {
        var granted = hr.grantPower("u-einkauf-1", "acme", PowerOfAttorneyType.HANDLUNGSVOLLMACHT,
                SignatureRule.SOLE, new MoneyView(new BigDecimal("5000"), "EUR", false), "Einkauf",
                LocalDate.of(2024, 1, 1), null);
        assertThat(granted.revoked()).isFalse();
        assertThat(hr.powersOf("u-einkauf-1")).extracting(HrViews.PowerOfAttorneyView::id).contains(granted.id());

        var revoked = hr.revokePower(granted.id());
        assertThat(revoked.revoked()).isTrue();
        // Signatories for 4,000 € on the reference date do NOT include the revoked power of attorney.
        assertThat(hr.signatories("acme", new BigDecimal("4000"), "EUR", LocalDate.of(2024, 6, 1)))
                .extracting(HrViews.PowerOfAttorneyView::id).doesNotContain(granted.id());
    }

    @Test
    void absenceCalendarMapsSimDayToEnteredVacation() {
        // Start = 2026-01-01 → day 10 = 2026-01-11.
        assertThat(calendar.dateOf(10)).isEqualTo(LocalDate.of(2026, 1, 11));

        hr.createAbsence("u-einkauf-1", AbsenceType.VACATION,
                LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 15), null, null);

        assertThat(calendar.absencesOnDay(10)).containsEntry("u-einkauf-1", "VACATION"); // within the range
        assertThat(calendar.absencesOnDay(200)).doesNotContainKey("u-einkauf-1");        // outside
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> hr.createAbsence("does-not-exist", AbsenceType.VACATION,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThatThrownBy(() -> hr.createAbsence("u-gf-1", AbsenceType.VACATION,
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 1), null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
    }
}
