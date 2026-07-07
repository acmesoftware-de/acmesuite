package de.acmesoftware.acmesuite.supply;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.process.ProcessEngine;
import de.acmesoftware.acmesuite.supply.SupplyService.LineInput;
import de.acmesoftware.acmesuite.supply.domain.MaterialKind;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** ACMEsupply: catalog/tiers/lead times + analog approval (procurement → procurement folder → approved). */
@SpringBootTest
@Import(TestcontainersConfig.class)
@TestPropertySource(properties = "acme.sim.autostart=false")
class SupplyServiceTest {

    @Autowired
    SupplyService supply;

    @Autowired
    ProcessEngine process;

    @Test
    void seedsSuppliersMaterialsAndContracts() {
        assertThat(supply.listSuppliers(null, null)).hasSizeGreaterThanOrEqualTo(3);
        assertThat(supply.listMaterials(MaterialKind.ENERGY, null))
                .extracting(SupplyViews.MaterialView::id).contains("mat-strom", "mat-kohle");
    }

    @Test
    void resolvesTierPriceAndLeadTime() {
        var one = supply.resolvePrice("sup-rohstoff", "mat-stahl", 1);
        assertThat(one.unitPrice()).isEqualByComparingTo("2.50");
        assertThat(one.leadTimeDays()).isEqualTo(14);

        var bulk = supply.resolvePrice("sup-rohstoff", "mat-stahl", 1000); // tier from 1000
        assertThat(bulk.unitPrice()).isEqualByComparingTo("2.10");
    }

    @Test
    void largeProcurementBecomesBlueFolderAndIsApprovedWhenSigned() {
        var o = supply.createOrder("sup-energie", List.of(new LineInput("mat-kohle", 200, null)), null, "SHIP");
        assertThat(o.total().amount()).isEqualByComparingTo("19600.00");        // 200 × 98 (tier from 100)
        assertThat(o.expectedDeliveryDate()).isEqualTo(o.orderDate().plusDays(21)); // lead time coal

        var sub = supply.submitOrder(o.id());
        assertThat(sub.status()).isEqualTo("PENDING_APPROVAL");

        // A blue procurement folder is waiting in circulation.
        String cid = process.nextPendingFor("u-einkauf-asst");
        assertThat(cid).isNotNull();

        boolean done = false;
        for (int i = 0; i < 5 && !done; i++) {
            done = process.signCurrentStep(cid);
        }
        assertThat(done).isTrue();
        assertThat(supply.getOrder(o.id())).get()
                .satisfies(v -> assertThat(v.status()).isEqualTo("APPROVED"));
    }
}
