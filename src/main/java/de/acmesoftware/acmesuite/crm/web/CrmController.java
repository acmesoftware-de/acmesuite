package de.acmesoftware.acmesuite.crm.web;

import de.acmesoftware.acmesuite.crm.CrmService;
import de.acmesoftware.acmesuite.crm.CrmViews.ContactView;
import de.acmesoftware.acmesuite.crm.CrmViews.CustomerView;
import de.acmesoftware.acmesuite.crm.CrmViews.MoneyView;
import de.acmesoftware.acmesuite.crm.CrmViews.PriceListView;
import de.acmesoftware.acmesuite.crm.CrmViews.ProductView;
import de.acmesoftware.acmesuite.crm.CrmViews.ResolvedPriceView;
import de.acmesoftware.acmesuite.crm.domain.CustomerKind;
import de.acmesoftware.acmesuite.crm.domain.CustomerStatus;
import de.acmesoftware.acmesuite.crm.domain.PriceListItem;
import de.acmesoftware.acmesuite.crm.domain.PriceListKind;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ACMEcrm HTTP API (Slice 1): customers/resellers, products, price lists + price resolution. */
@RestController
@RequestMapping("/api/crm")
public class CrmController {

    private final CrmService crm;

    public CrmController(CrmService crm) {
        this.crm = crm;
    }

    // ── Customers ──
    @GetMapping("/customers")
    public List<CustomerView> customers(
            @RequestParam(required = false) CustomerKind kind,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String resellerId,
            @RequestParam(required = false) String q) {
        return crm.listCustomers(kind, status, resellerId, q);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerView> customer(@PathVariable String id) {
        return ResponseEntity.of(crm.getCustomer(id));
    }

    @PostMapping("/customers")
    public ResponseEntity<CustomerView> createCustomer(@RequestBody CustomerWrite req) {
        CustomerView v = crm.createCustomer(req.name(), req.kind(), req.status(), req.email(), req.country(),
                req.parentResellerId(), req.priceListId());
        return ResponseEntity.created(URI.create("/api/crm/customers/" + v.id())).body(v);
    }

    @PatchMapping("/customers/{id}")
    public CustomerView updateCustomer(@PathVariable String id, @RequestBody CustomerWrite req) {
        return crm.updateCustomer(id, req.name(), req.kind(), req.status(), req.email(), req.country(),
                req.parentResellerId(), req.priceListId());
    }

    // ── Contacts ──
    @GetMapping("/contacts")
    public List<ContactView> contacts(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String q) {
        return crm.listContacts(customerId, q);
    }

    @GetMapping("/contacts/{id}")
    public ResponseEntity<ContactView> contact(@PathVariable String id) {
        return ResponseEntity.of(crm.getContact(id));
    }

    @PostMapping("/contacts")
    public ResponseEntity<ContactView> createContact(@RequestBody ContactWrite req) {
        ContactView v = crm.createContact(req.customerId(), req.name(), req.role(), req.email(), req.phone(),
                req.primary(), req.newsletter());
        return ResponseEntity.created(URI.create("/api/crm/contacts/" + v.id())).body(v);
    }

    @PatchMapping("/contacts/{id}")
    public ContactView updateContact(@PathVariable String id, @RequestBody ContactWrite req) {
        return crm.updateContact(id, req.customerId(), req.name(), req.role(), req.email(), req.phone(),
                req.primary(), req.newsletter());
    }

    // ── Products ──
    @GetMapping("/products")
    public List<ProductView> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q) {
        return crm.listProducts(category, active, q);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductView> product(@PathVariable String id) {
        return ResponseEntity.of(crm.getProduct(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductView> createProduct(@RequestBody ProductWrite req) {
        ProductView v = crm.createProduct(req.sku(), req.name(), req.category(), req.unit(), req.active(),
                money(req.listPrice()));
        return ResponseEntity.created(URI.create("/api/crm/products/" + v.id())).body(v);
    }

    @PatchMapping("/products/{id}")
    public ProductView updateProduct(@PathVariable String id, @RequestBody ProductWrite req) {
        return crm.updateProduct(id, req.sku(), req.name(), req.category(), req.unit(), req.active(),
                money(req.listPrice()));
    }

    // ── Price lists ──
    @GetMapping("/price-lists")
    public List<PriceListView> priceLists(@RequestParam(required = false) PriceListKind kind) {
        return crm.listPriceLists(kind);
    }

    @GetMapping("/price-lists/{id}")
    public ResponseEntity<PriceListView> priceList(@PathVariable String id) {
        return ResponseEntity.of(crm.getPriceList(id));
    }

    @PostMapping("/price-lists")
    public ResponseEntity<PriceListView> createPriceList(@RequestBody PriceListWrite req) {
        List<PriceListItem> items = req.items() == null ? List.of() : req.items().stream()
                .map(i -> new PriceListItem(i.productId(), i.unitPrice(), i.minQuantity() == null ? 1 : i.minQuantity()))
                .toList();
        PriceListView v = crm.createPriceList(req.name(), req.currency(), req.kind(), req.validFrom(),
                req.validUntil(), items);
        return ResponseEntity.created(URI.create("/api/crm/price-lists/" + v.id())).body(v);
    }

    @GetMapping("/price")
    public ResolvedPriceView price(
            @RequestParam String customerId,
            @RequestParam String productId,
            @RequestParam(defaultValue = "1") int quantity) {
        return crm.resolvePrice(customerId, productId, quantity);
    }

    private static Money money(MoneyView m) {
        if (m == null || m.unlimited() || m.amount() == null) {
            return null;
        }
        return new Money(m.amount(), m.currency() == null ? "EUR" : m.currency());
    }

    public record CustomerWrite(String name, CustomerKind kind, CustomerStatus status, String email, String country,
                                String parentResellerId, String priceListId) {
    }

    public record ContactWrite(String customerId, String name, String role, String email, String phone,
                               Boolean primary, Boolean newsletter) {
    }

    public record ProductWrite(String sku, String name, String category, String unit, Boolean active,
                               MoneyView listPrice) {
    }

    public record PriceListWrite(String name, String currency, PriceListKind kind,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                                 List<ItemWrite> items) {
    }

    public record ItemWrite(String productId, BigDecimal unitPrice, Integer minQuantity) {
    }
}
