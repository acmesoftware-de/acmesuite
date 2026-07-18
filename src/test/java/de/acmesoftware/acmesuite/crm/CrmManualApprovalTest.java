package de.acmesoftware.acmesuite.crm;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.crm.SalesService.LineInput;
import de.acmesoftware.acmesuite.process.ProcessEngine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * ACME analog: an order requiring approval creates a signature folder in circulation; its signature
 * releases the order (event coupling CRM ↔ process). Scheduler off, so that the signing here is
 * driven deterministically by hand.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@TestPropertySource(properties = "acme.sim.autostart=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CrmManualApprovalTest {

    @Autowired
    SalesService sales;

    @Autowired
    ProcessEngine process;

    @Test
    void largeOrderBecomesFolderAndIsApprovedWhenSigned() {
        var o = sales.createOrder("cust-kontor", null, List.of(new LineInput("prod-radio", 100, null, null)), null);
        var sub = sales.submitOrder(o.id());
        assertThat(sub.status()).isEqualTo("PENDING_APPROVAL"); // above the threshold → folder

        // A sales folder for this order is now waiting in circulation.
        String cid = process.nextPendingFor("u-vertrieb-asst");
        assertThat(cid).isNotNull();

        // Signature(s) → the response releases the order.
        boolean done = false;
        for (int i = 0; i < 5 && !done; i++) {
            done = process.signCurrentStep(cid);
        }
        assertThat(done).isTrue();
        assertThat(sales.getOrder(o.id())).get()
                .satisfies(v -> assertThat(v.status()).isEqualTo("APPROVED"));
    }
}
