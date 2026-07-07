package de.acmesoftware.acmesuite.crm;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.crm.CrmViews.ProductView;
import de.acmesoftware.acmesuite.crm.domain.CustomerKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** ACMEcrm application logic against the seeded demo data. */
@SpringBootTest
@Import(TestcontainersConfig.class)
class CrmServiceTest {

    @Autowired
    CrmService crm;

    @Test
    void seedsCatalogAndCustomers() {
        assertThat(crm.listProducts(null, null, null)).extracting(ProductView::id).contains("prod-radio");
        assertThat(crm.listCustomers(null, null, null, null)).hasSizeGreaterThanOrEqualTo(4);
        assertThat(crm.listCustomers(CustomerKind.RESELLER, null, null, null))
                .extracting(CrmViews.CustomerView::id).contains("cust-retro", "cust-vintage");
    }

    @Test
    void resolvesListResellerAndStaffelPrices() {
        // Direct customer → list price.
        var direct = crm.resolvePrice("cust-kontor", "prod-radio", 1);
        assertThat(direct.source()).isEqualTo("PRICE_LIST");
        assertThat(direct.unitPrice()).isEqualByComparingTo("149.00");

        // Reseller → 20 % discount …
        var reseller = crm.resolvePrice("cust-retro", "prod-radio", 1);
        assertThat(reseller.unitPrice()).isEqualByComparingTo("119.20");

        // … and from 10 units the better tier (30 %).
        var bulk = crm.resolvePrice("cust-retro", "prod-radio", 10);
        assertThat(bulk.unitPrice()).isEqualByComparingTo("104.30");
    }

    @Test
    void createsProductAndCustomer() {
        var p = crm.createProduct("ACME-TEST-99", "Test Device", "Test", "pcs", true, null);
        assertThat(crm.getProduct(p.id())).isPresent();
        var c = crm.createCustomer("Test Customer", CustomerKind.CUSTOMER, null, "t@example.com", "DE", null, null);
        assertThat(crm.getCustomer(c.id())).get().satisfies(v -> assertThat(v.status()).isEqualTo("PROSPECT"));
    }
}
