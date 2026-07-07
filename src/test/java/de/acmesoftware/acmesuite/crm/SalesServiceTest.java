package de.acmesoftware.acmesuite.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.crm.SalesService.LineInput;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

/** ACMEcrm sales + e-approval against the seeded data (prices + ACMEhr signing authority). */
@SpringBootTest
@Import(TestcontainersConfig.class)
class SalesServiceTest {

    @Autowired
    SalesService sales;

    private static LineInput radio(int qty) {
        return new LineInput("prod-radio", qty, null, null);
    }

    @Test
    void smallOrderIsAutoApprovedOnSubmit() {
        var o = sales.createOrder("cust-kontor", null, List.of(radio(10)), null);
        assertThat(o.status()).isEqualTo("CREATED");
        assertThat(o.total().amount()).isEqualByComparingTo("1490.00"); // 10 × 149 (list)

        var sub = sales.submitOrder(o.id());
        assertThat(sub.status()).isEqualTo("APPROVED"); // below the threshold
        assertThat(sub.approval().required()).isFalse();
    }

    @Test
    void largeOrderNeedsAuthorizedApprover() {
        var o = sales.createOrder("cust-kontor", null, List.of(radio(100)), null);
        assertThat(o.total().amount()).isEqualByComparingTo("14900.00");

        var sub = sales.submitOrder(o.id());
        assertThat(sub.status()).isEqualTo("PENDING_APPROVAL");

        // Buyer without a sufficient approval limit → rejected (422).
        assertThatThrownBy(() -> sales.decideOrder(o.id(), "u-einkauf-1", true, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");

        // Managing director (general power of attorney ∞) approves.
        var dec = sales.decideOrder(o.id(), "u-gf-1", true, "ok");
        assertThat(dec.status()).isEqualTo("APPROVED");
        assertThat(dec.approval().approverId()).isEqualTo("u-gf-1");

        var ful = sales.fulfillOrder(o.id());
        assertThat(ful.status()).isEqualTo("FULFILLED");
    }

    @Test
    void quoteUsesResellerPriceAndConvertsToOrder() {
        var q = sales.createQuote("cust-retro", null, List.of(radio(1)));
        assertThat(q.netTotal()).isEqualByComparingTo("119.20"); // reseller −20 %

        var ord = sales.convertQuoteToOrder(q.id());
        assertThat(ord.quoteId()).isEqualTo(q.id());
        assertThat(sales.getQuote(q.id())).get().satisfies(v -> assertThat(v.status()).isEqualTo("ACCEPTED"));
    }
}
