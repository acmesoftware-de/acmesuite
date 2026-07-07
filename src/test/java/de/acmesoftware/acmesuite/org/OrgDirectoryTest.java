package de.acmesoftware.acmesuite.org;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.domain.AbsenceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test against the canonical ACME org (seeded via {@code AcmeOrgCatalog}/{@code OrgSeeder}).
 * Checks structure, overlay (reporting line/substitution/assistance), absences and the
 * signature/power-of-attorney logic.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class OrgDirectoryTest {

    @Autowired
    OrgDirectory org;

    @Autowired
    AbsenceRepository absences;

    @Test
    void seedsTheCanonicalCompany() {
        // One legal entity (ACME Group GmbH), 88 persons.
        assertThat(org.legalEntities()).singleElement()
                .satisfies(e -> assertThat(e.id()).isEqualTo("acme"));
        assertThat(org.persons()).hasSize(99); // 88 demo catalog + sales/HR/procurement assistance
        assertThat(org.groupRoots()).singleElement()
                .satisfies(e -> assertThat(e.platformTenantKey()).isEqualTo("acme-group"));
    }

    @Test
    void allUnitsBelongToTheOneCompany() {
        assertThat(org.orgUnitsOf("acme"))
                .extracting(v -> v.id())
                .contains("ou-gf", "ou-fb-a", "ou-fb-b", "ou-einkauf", "ou-finance",
                        "ou-team-b1-1", "ou-gremium");
    }

    @Test
    void overlayCarriesReportingDelegationAndAssistance() {
        // Reporting line: buyer reports to Head of Procurement.
        assertThat(org.person("u-einkauf-1")).get()
                .satisfies(p -> assertThat(p.managerId()).isEqualTo("u-einkauf-lead"));

        // Managing director: no manager, own assistant.
        assertThat(org.person("u-gf-1")).get().satisfies(p -> {
            assertThat(p.managerId()).isNull();
            assertThat(p.jobTitle()).isEqualTo("Geschäftsführerin");
            assertThat(p.assistantIds()).containsExactly("u-gf-1-asst");
        });

        // Department lead B1: two team leads as substitutes, shared assistant.
        assertThat(org.person("u-abt-b1-lead")).get().satisfies(p -> {
            assertThat(p.delegateIds()).hasSize(2).contains("u-b1-1-1", "u-b1-2-8");
            assertThat(p.assistantIds()).containsExactly("u-fb-b-asst");
        });
    }

    @Test
    void matrixMembershipAndAbsencesAreSeeded() {
        assertThat(org.person("u-compliance-1")).get()
                .satisfies(p -> assertThat(p.secondaryUnitIds()).contains("ou-gremium"));

        // Two absences with substitution (vacation department lead B1, treatment leave GF1).
        assertThat(absences.findAll()).hasSize(2);
        assertThat(absences.findByPerson_Id("u-abt-b1-lead")).singleElement()
                .satisfies(a -> assertThat(a.getSubstitute().getId()).isEqualTo("u-b1-1-1"));
    }

    @Test
    void signatoriesRespectMonetaryLimits() {
        var on = LocalDate.of(2024, 6, 1);

        // 100k: both managing directors (general ∞), CFO (Prokura ∞) and Head of Procurement (250k limit).
        assertThat(org.signatoriesFor("acme", new BigDecimal("100000"), "EUR", on))
                .extracting(p -> p.holderId())
                .containsExactlyInAnyOrder("u-gf-1", "u-gf-2", "u-finance-cfo", "u-einkauf-lead");

        // 300k: Head of Procurement drops out (over limit), the unlimited ones remain.
        assertThat(org.signatoriesFor("acme", new BigDecimal("300000"), "EUR", on))
                .extracting(p -> p.holderId())
                .containsExactlyInAnyOrder("u-gf-1", "u-gf-2", "u-finance-cfo")
                .doesNotContain("u-einkauf-lead");
    }
}
