package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.domain.Contact;
import de.acmesoftware.acmesuite.crm.domain.ContactRepository;
import de.acmesoftware.acmesuite.crm.domain.Customer;
import de.acmesoftware.acmesuite.crm.domain.CustomerKind;
import de.acmesoftware.acmesuite.crm.domain.CustomerRepository;
import de.acmesoftware.acmesuite.crm.domain.CustomerStatus;
import de.acmesoftware.acmesuite.crm.domain.Deal;
import de.acmesoftware.acmesuite.crm.domain.DealRepository;
import de.acmesoftware.acmesuite.crm.domain.DealSource;
import de.acmesoftware.acmesuite.crm.domain.MailDirection;
import de.acmesoftware.acmesuite.crm.domain.MailMessage;
import de.acmesoftware.acmesuite.crm.domain.MailThread;
import de.acmesoftware.acmesuite.crm.domain.MailThreadRepository;
import de.acmesoftware.acmesuite.crm.domain.PipelineStage;
import de.acmesoftware.acmesuite.crm.domain.PriceList;
import de.acmesoftware.acmesuite.crm.domain.PriceListItem;
import de.acmesoftware.acmesuite.crm.domain.PriceListKind;
import de.acmesoftware.acmesuite.crm.domain.PriceListRepository;
import de.acmesoftware.acmesuite.crm.domain.Product;
import de.acmesoftware.acmesuite.crm.domain.ProductRepository;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    private final ContactRepository contacts;
    private final DealRepository deals;
    private final MailThreadRepository threads;

    public CrmSeeder(ProductRepository products, PriceListRepository priceLists, CustomerRepository customers,
                     ContactRepository contacts, DealRepository deals, MailThreadRepository threads) {
        this.products = products;
        this.priceLists = priceLists;
        this.customers = customers;
        this.contacts = contacts;
        this.deals = deals;
        this.threads = threads;
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

        seedContactsPipelineMail();
    }

    /** Contacts, pipeline deals and mail threads for the seeded customers (see api/acme-crm.yaml). */
    private void seedContactsPipelineMail() {
        // ── Contacts (one primary per customer) ──
        contacts.save(new Contact("contact-kontor", "cust-kontor", "Michael Braun", "CPO",
                "braun@kontor-nord.de", "+49 40 123456", true, false));
        contacts.save(new Contact("contact-retro", "cust-retro", "Karin Frei", "Einkauf",
                "frei@retro-reseller.de", "+49 30 234567", true, true));
        contacts.save(new Contact("contact-vintage", "cust-vintage", "Sara Mena", "CTO",
                "mena@vintage-vertrieb.de", "+43 1 345678", true, true));
        contacts.save(new Contact("contact-mueller", "cust-mueller", "Tom Roth", null,
                "roth@nostalgie.de", "+49 89 456789", true, false));

        // ── Pipeline deals (stage stored; ageDays derives from stage_since) ──
        deals.save(deal("deal-kontor", DealSource.ORDER, "cust-kontor", "contact-kontor", "Nordic Trading GmbH",
                "Michael Braun · CPO", PipelineStage.VERHANDLUNG, "JS", "Jana Schmidt", "84000",
                "Nachfassen offen · vor 3 T", 3));
        deals.save(deal("deal-vintage", DealSource.QUOTE, "cust-vintage", "contact-vintage",
                "Vintage Distribution e.K.", "Sara Mena · CTO", PipelineStage.ANGEBOT, "AL", "Amir Lang", "152000",
                "Angebot gesendet · vor 18 Min", 6));
        deals.save(deal("deal-retro", DealSource.QUOTE, "cust-retro", "contact-retro", "Retro Reseller AG",
                "Karin Frei · Einkauf", PipelineStage.QUALIFIZIERT, "MW", "Mara Weiss", "41000",
                "E-Mail geöffnet · vor 5 Std", 2));
        deals.save(deal("deal-mueller", DealSource.LEAD, "cust-mueller", "contact-mueller", "Nostalgia Shop Müller",
                "Tom Roth", PipelineStage.NEU, "JS", "Jana Schmidt", "12000", "Neuer Lead · heute", 0));
        deals.save(deal("deal-kontor-won", DealSource.ORDER, "cust-kontor", "contact-kontor", "Nordic Trading GmbH",
                "Michael Braun · CPO", PipelineStage.GEWONNEN, "MW", "Mara Weiss", "305000", "Vertrag · gestern", 12));

        // ── Mail threads ──
        threads.save(new MailThread("thread-angebot", "cust-kontor", "contact-kontor", "Angebot Rahmen XL · 800 Stk",
                Instant.parse("2026-07-10T14:05:00Z"), List.of("Michael Braun", "Jana Schmidt"), List.of(
                        new MailMessage("m1", MailDirection.INBOUND, "Michael Braun", "Jana Schmidt",
                                Instant.parse("2026-07-08T09:12:00Z"),
                                "Könnt ihr 800 Rahmen XL bis Ende Q3 liefern?",
                                "Hallo Jana, könnt ihr 800 Rahmen XL bis Ende Q3 liefern? Bitte um ein Angebot."),
                        new MailMessage("m2", MailDirection.OUTBOUND, "Jana Schmidt", "Michael Braun",
                                Instant.parse("2026-07-09T11:40:00Z"), "Angebot anbei, Termin machbar.",
                                "Hallo Michael, Angebot anbei. Termin ist machbar bei Bestätigung diese Woche."),
                        new MailMessage("m3", MailDirection.INBOUND, "Michael Braun", "Jana Schmidt",
                                Instant.parse("2026-07-10T14:05:00Z"), "Sieht gut aus, wir melden uns.",
                                "Danke, sieht gut aus – wir stimmen intern ab und melden uns."))));
        threads.save(new MailThread("thread-vertrag", "cust-vintage", "contact-vintage", "Vertrag unterschrieben",
                Instant.parse("2026-07-11T09:00:00Z"), List.of("Sara Mena", "Amir Lang"), List.of(
                        new MailMessage("m1", MailDirection.OUTBOUND, "Amir Lang", "Sara Mena",
                                Instant.parse("2026-07-10T15:10:00Z"), "Finaler Vertrag zur Unterschrift.",
                                "Anbei der finale Vertrag zur Unterschrift."),
                        new MailMessage("m2", MailDirection.INBOUND, "Sara Mena", "Amir Lang",
                                Instant.parse("2026-07-11T09:00:00Z"), "Unterschrieben zurück.",
                                "Unterschrieben zurück – freuen uns auf die Zusammenarbeit!"))));
    }

    private static Deal deal(String id, DealSource source, String customerId, String contactId, String company,
                             String contact, PipelineStage stage, String ownerInitials, String ownerName,
                             String value, String lastActivity, int ageDays) {
        return new Deal(id, source, customerId, contactId, company, contact, stage, ownerInitials, ownerName,
                new Money(new BigDecimal(value), "EUR"), null, null, lastActivity, null,
                LocalDate.now().minusDays(ageDays));
    }

    private static BigDecimal scale(BigDecimal base, String factor) {
        return base.multiply(new BigDecimal(factor)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
