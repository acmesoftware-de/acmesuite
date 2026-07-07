package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.domain.Customer;
import de.acmesoftware.acmesuite.crm.domain.CustomerKind;
import de.acmesoftware.acmesuite.crm.domain.CustomerRepository;
import de.acmesoftware.acmesuite.crm.domain.CustomerStatus;
import de.acmesoftware.acmesuite.crm.domain.PriceList;
import de.acmesoftware.acmesuite.crm.domain.PriceListItem;
import de.acmesoftware.acmesuite.crm.domain.PriceListKind;
import de.acmesoftware.acmesuite.crm.domain.PriceListRepository;
import de.acmesoftware.acmesuite.crm.domain.Product;
import de.acmesoftware.acmesuite.crm.domain.ProductRepository;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Seeds a small, stable ACMEcrm demo dataset: analog products, list/reseller prices, customers. */
@Component
@Order(2)
public class CrmSeeder implements ApplicationRunner {

    private final ProductRepository products;
    private final PriceListRepository priceLists;
    private final CustomerRepository customers;

    public CrmSeeder(ProductRepository products, PriceListRepository priceLists, CustomerRepository customers) {
        this.products = products;
        this.priceLists = priceLists;
        this.customers = customers;
    }

    private record P(String id, String sku, String name, String category, String price) {
    }

    @Override
    public void run(ApplicationArguments args) {
        if (products.count() > 0) {
            return; // idempotent
        }
        List<P> catalog = List.of(
                new P("prod-radio", "ACME-RADIO-01", "Tube Radio \"Nostalgia\"", "Audio", "149.00"),
                new P("prod-turntable", "ACME-TT-02", "Turntable \"Vinyl Deluxe\"", "Audio", "299.00"),
                new P("prod-tape", "ACME-TAPE-03", "Cassette Deck \"Tape Master\"", "Audio", "199.00"),
                new P("prod-super8", "ACME-CAM-04", "Super 8 Film Camera", "Photo/Film", "449.00"),
                new P("prod-phone", "ACME-PHONE-05", "Rotary Dial Telephone", "Phone", "89.00"),
                new P("prod-typewriter", "ACME-TW-06", "Typewriter \"Clack\"", "Office", "259.00"));
        for (P p : catalog) {
            products.save(new Product(p.id(), p.sku(), p.name(), p.category(), "pcs", true,
                    new Money(new BigDecimal(p.price()), "EUR")));
        }

        // List price list = catalog prices.
        List<PriceListItem> listItems = new ArrayList<>();
        // Reseller price list = ~20% discount, additional tier from 10 units (~30%).
        List<PriceListItem> resellerItems = new ArrayList<>();
        for (P p : catalog) {
            BigDecimal base = new BigDecimal(p.price());
            listItems.add(new PriceListItem(p.id(), base, 1));
            resellerItems.add(new PriceListItem(p.id(), scale(base, "0.80"), 1));
            resellerItems.add(new PriceListItem(p.id(), scale(base, "0.70"), 10));
        }
        priceLists.save(new PriceList("pl-list", "Standard List 2026", "EUR", PriceListKind.LIST,
                DateRange.openFrom(null), listItems));
        priceLists.save(new PriceList("pl-reseller", "Reseller List 2026", "EUR", PriceListKind.RESELLER,
                DateRange.openFrom(null), resellerItems));

        customers.save(new Customer("cust-kontor", "Nordic Trading GmbH", CustomerKind.CUSTOMER,
                CustomerStatus.ACTIVE, "einkauf@kontor-nord.de", "DE", null, null));
        customers.save(new Customer("cust-retro", "Retro Reseller AG", CustomerKind.RESELLER,
                CustomerStatus.ACTIVE, "partner@retro-reseller.de", "DE", null, "pl-reseller"));
        customers.save(new Customer("cust-vintage", "Vintage Distribution e.K.", CustomerKind.RESELLER,
                CustomerStatus.ACTIVE, "info@vintage-vertrieb.de", "AT", null, "pl-reseller"));
        customers.save(new Customer("cust-mueller", "Nostalgia Shop Müller", CustomerKind.CUSTOMER,
                CustomerStatus.ACTIVE, "mueller@nostalgie.de", "DE", "cust-retro", null));
    }

    private static BigDecimal scale(BigDecimal base, String factor) {
        return base.multiply(new BigDecimal(factor)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
