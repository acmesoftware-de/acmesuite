package de.acmesoftware.acmesuite.process;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.shared.CountryRestriction;
import org.junit.jupiter.api.Test;

/** The multi-stage approval rules of "ACME offline" (pure logic). */
class ApprovalRoutingTest {

    private final ApprovalRouting routing = new ApprovalRouting();

    private static Restrictions purchase(Material m, CountryRestriction cr) {
        return Restrictions.forPurchase(m, cr);
    }

    private static Restrictions sales(Product p, CountryRestriction cr, boolean byAir) {
        return Restrictions.forSales(p, cr, byAir);
    }

    // ── Purchasing ──

    @Test
    void easilyAvailableMaterialsNeedNoApproval() {
        assertThat(routing.forPurchase(Material.FIGETIAXOS, 90_000, Restrictions.none(), false)).isEmpty();
        assertThat(routing.forPurchase(Material.WIDGETIX, 90_000, Restrictions.none(), false)).isEmpty();
    }

    @Test
    void smallPurchaseStopsAtProcurementLead() {
        var steps = routing.forPurchase(Material.DEMOKRATIUM, 5_000, Restrictions.none(), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Specialist Department", "Procurement", "Head of Procurement");
    }

    @Test
    void smallPurchaseSpotCheckAddsControlling() {
        var steps = routing.forPurchase(Material.DEMOKRATIUM, 5_000, Restrictions.none(), true);
        assertThat(steps).extracting(ApprovalStep::label).last().isEqualTo("Controlling");
    }

    @Test
    void midValueAddsAreaLeadAndAboveThresholdAddsControllingAndCfo() {
        var steps = routing.forPurchase(Material.DEMOKRATIUM, 60_000, Restrictions.none(), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Specialist Department", "Division Management", "Procurement", "Head of Procurement",
                        "Controlling", "CFO");
    }

    @Test
    void restrictedPurchaseGoesUpToBothManagingDirectorsWhenTariffsApply() {
        // Despotium: CO₂ + air freight → restriction, CO₂ ⇒ penalty tariffs/levies ⇒ both managing directors; 300k > 250k ⇒ CFO.
        var steps = routing.forPurchase(Material.DESPOTIUM, 300_000,
                purchase(Material.DESPOTIUM, CountryRestriction.NONE), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Specialist Department", "Division Management", "Procurement", "Head of Procurement",
                        "Controlling", "CFO", "Executive Management", "Executive Management II");
        assertThat(steps).extracting(ApprovalStep::fixedPersonKey).contains("u-gf-1", "u-gf-2");
    }

    @Test
    void restrictedPurchaseWithoutTariffsHasOneManagingDirectorAndNoCfoBelowControllingLimit() {
        // Electronics: only air freight → restriction, but no penalty tariffs/levies ⇒ 1 managing director; 100k ≤ 250k ⇒ no CFO.
        var steps = routing.forPurchase(Material.ELEKTRONIK, 100_000,
                purchase(Material.ELEKTRONIK, CountryRestriction.NONE), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Specialist Department", "Division Management", "Procurement", "Head of Procurement",
                        "Controlling", "Executive Management");
    }

    @Test
    void sanctionedSourceCountryTriggersBothManagingDirectors() {
        var steps = routing.forPurchase(Material.ELEKTRONIK, 20_000,
                purchase(Material.ELEKTRONIK, CountryRestriction.GENERAL), false);
        assertThat(steps).extracting(ApprovalStep::label).contains("Executive Management", "Executive Management II");
    }

    @Test
    void importTariffCountryAddsBothManagingDirectors() {
        var steps = routing.forPurchase(Material.DEMOKRATIUM, 20_000,
                purchase(Material.DEMOKRATIUM, CountryRestriction.IMPORT_TARIFF), false);
        assertThat(steps).extracting(ApprovalStep::label).contains("Executive Management", "Executive Management II");
    }

    // ── Sales ──

    @Test
    void smallProductsStayOnlineUnlessRestrictedOrAboveThreshold() {
        assertThat(routing.forSales(Product.NEXUS, 5_000, Restrictions.none(), false)).isEmpty();
        assertThat(routing.forSales(Product.NEXUS, 50_000, Restrictions.none(), false)).isNotEmpty();
    }

    @Test
    void smallSaleStopsAtSalesLead() {
        var steps = routing.forSales(Product.WIDGET, 5_000, Restrictions.none(), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Sales", "Specialist Department", "Head of Sales");
    }

    @Test
    void midSaleAddsControllingAndCfoOnlyAboveControllingLimit() {
        assertThat(routing.forSales(Product.WIDGET, 60_000, Restrictions.none(), false))
                .extracting(ApprovalStep::label)
                .containsExactly("Sales", "Specialist Department", "Head of Sales", "Controlling");
        assertThat(routing.forSales(Product.WIDGET, 300_000, Restrictions.none(), false))
                .extracting(ApprovalStep::label).contains("CFO");
    }

    @Test
    void dualUseSaleGoesToBothManagingDirectors() {
        var p = Product.TYPEWRITER;
        var steps = routing.forSales(p, 5_000, sales(p, CountryRestriction.NONE, false), false);
        assertThat(steps).extracting(ApprovalStep::label)
                .containsExactly("Sales", "Specialist Department", "Head of Sales", "Controlling", "CFO",
                        "Executive Management I", "Executive Management II");
    }

    // ── HR ──

    @Test
    void hrHireGoesReferentLeadGf() {
        assertThat(routing.forHr(ContractType.HR_HIRE)).extracting(ApprovalStep::label)
                .containsExactly("HR Associate", "Head of HR", "Executive Management");
    }

    @Test
    void hrTerminationInvolvesLegal() {
        assertThat(routing.forHr(ContractType.HR_TERMINATION))
                .extracting(ApprovalStep::unitKey).contains("ou-hr", "ou-legal");
    }
}
