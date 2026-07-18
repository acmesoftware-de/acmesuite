package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.CrmViews.ContactView;
import de.acmesoftware.acmesuite.crm.CrmViews.CustomerView;
import de.acmesoftware.acmesuite.crm.CrmViews.PriceListView;
import de.acmesoftware.acmesuite.crm.CrmViews.ProductView;
import de.acmesoftware.acmesuite.crm.CrmViews.ResolvedPriceView;
import de.acmesoftware.acmesuite.crm.domain.Contact;
import de.acmesoftware.acmesuite.crm.domain.ContactRepository;
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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ACMEcrm application logic: customers/resellers, products, price lists + effective price resolution. */
@Service
@Transactional
public class CrmService {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final PriceListRepository priceLists;
    private final ContactRepository contacts;

    public CrmService(CustomerRepository customers, ProductRepository products, PriceListRepository priceLists,
                      ContactRepository contacts) {
        this.customers = customers;
        this.products = products;
        this.priceLists = priceLists;
        this.contacts = contacts;
    }

    // ── Customers ──
    @Transactional(readOnly = true)
    public List<CustomerView> listCustomers(CustomerKind kind, CustomerStatus status, String resellerId, String q) {
        String needle = q == null ? null : q.toLowerCase();
        return customers.findAll().stream()
                .filter(c -> kind == null || c.getKind() == kind)
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> resellerId == null || resellerId.equals(c.getParentResellerId()))
                .filter(c -> needle == null || c.getName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Customer::getName))
                .map(CustomerView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CustomerView> getCustomer(String id) {
        return customers.findById(id).map(CustomerView::of);
    }

    public CustomerView createCustomer(String name, CustomerKind kind, CustomerStatus status, String email,
                                       String country, String parentResellerId, String priceListId) {
        if (name == null || name.isBlank() || kind == null) {
            throw unprocessable("name and kind are required");
        }
        validatePriceList(priceListId);
        String id = "cust-" + UUID.randomUUID().toString().substring(0, 12);
        Customer c = new Customer(id, name, kind, status, email, country, parentResellerId, priceListId);
        return CustomerView.of(customers.save(c));
    }

    public CustomerView updateCustomer(String id, String name, CustomerKind kind, CustomerStatus status,
                                       String email, String country, String parentResellerId, String priceListId) {
        Customer c = customers.findById(id).orElseThrow(() -> notFound("Customer " + id + " unknown"));
        validatePriceList(priceListId);
        c.update(name, kind, status, email, country, parentResellerId, priceListId);
        return CustomerView.of(c);
    }

    // ── Contacts ──
    @Transactional(readOnly = true)
    public List<ContactView> listContacts(String customerId, String q) {
        String needle = q == null ? null : q.toLowerCase();
        return (customerId != null ? contacts.findByCustomerId(customerId) : contacts.findAll()).stream()
                .filter(c -> needle == null || c.getName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Contact::getName))
                .map(ContactView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ContactView> getContact(String id) {
        return contacts.findById(id).map(ContactView::of);
    }

    public ContactView createContact(String customerId, String name, String role, String email, String phone,
                                     Boolean primary, Boolean newsletter) {
        if (name == null || name.isBlank()) {
            throw unprocessable("name is required");
        }
        validateCustomer(customerId);
        String id = "contact-" + UUID.randomUUID().toString().substring(0, 12);
        Contact c = new Contact(id, customerId, name, role, email, phone,
                Boolean.TRUE.equals(primary), Boolean.TRUE.equals(newsletter));
        return ContactView.of(contacts.save(c));
    }

    public ContactView updateContact(String id, String customerId, String name, String role, String email,
                                     String phone, Boolean primary, Boolean newsletter) {
        Contact c = contacts.findById(id).orElseThrow(() -> notFound("Contact " + id + " unknown"));
        validateCustomer(customerId);
        c.update(customerId, name, role, email, phone, primary, newsletter);
        return ContactView.of(c);
    }

    private void validateCustomer(String customerId) {
        if (customerId != null && !customers.existsById(customerId)) {
            throw unprocessable("Customer " + customerId + " unknown");
        }
    }

    // ── Products ──
    @Transactional(readOnly = true)
    public List<ProductView> listProducts(String category, Boolean active, String q) {
        String needle = q == null ? null : q.toLowerCase();
        return products.findAll().stream()
                .filter(p -> category == null || category.equals(p.getCategory()))
                .filter(p -> active == null || p.isActive() == active)
                .filter(p -> needle == null
                        || p.getName().toLowerCase().contains(needle)
                        || p.getSku().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Product::getName))
                .map(ProductView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductView> getProduct(String id) {
        return products.findById(id).map(ProductView::of);
    }

    public ProductView createProduct(String sku, String name, String category, String unit, Boolean active,
                                     Money listPrice) {
        if (sku == null || sku.isBlank() || name == null || name.isBlank()) {
            throw unprocessable("sku and name are required");
        }
        String id = "prod-" + UUID.randomUUID().toString().substring(0, 12);
        Product p = new Product(id, sku, name, category, unit, active == null || active, listPrice);
        return ProductView.of(products.save(p));
    }

    public ProductView updateProduct(String id, String sku, String name, String category, String unit,
                                     Boolean active, Money listPrice) {
        Product p = products.findById(id).orElseThrow(() -> notFound("Product " + id + " unknown"));
        p.update(sku, name, category, unit, active, listPrice);
        return ProductView.of(p);
    }

    // ── Price lists ──
    @Transactional(readOnly = true)
    public List<PriceListView> listPriceLists(PriceListKind kind) {
        return (kind == null ? priceLists.findAll() : priceLists.findByKind(kind)).stream()
                .map(PriceListView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PriceListView> getPriceList(String id) {
        return priceLists.findById(id).map(PriceListView::of);
    }

    public PriceListView createPriceList(String name, String currency, PriceListKind kind, LocalDate from,
                                         LocalDate until, List<PriceListItem> items) {
        if (name == null || currency == null || kind == null) {
            throw unprocessable("name, currency and kind are required");
        }
        String id = "pl-" + UUID.randomUUID().toString().substring(0, 12);
        PriceList pl = new PriceList(id, name, currency, kind, new DateRange(from, until), items);
        return PriceListView.of(priceLists.save(pl));
    }

    // ── Price resolution ──
    @Transactional(readOnly = true)
    public ResolvedPriceView resolvePrice(String customerId, String productId, int quantity) {
        Customer c = customers.findById(customerId)
                .orElseThrow(() -> notFound("Customer " + customerId + " unknown"));
        Product p = products.findById(productId)
                .orElseThrow(() -> notFound("Product " + productId + " unknown"));
        int qty = Math.max(1, quantity);
        PriceList list = priceListFor(c);
        if (list != null) {
            Optional<PriceListItem> item = list.getItems().stream()
                    .filter(i -> i.getProductId().equals(productId) && i.getMinQuantity() <= qty)
                    .max(Comparator.comparingInt(PriceListItem::getMinQuantity)); // best tier
            if (item.isPresent()) {
                return new ResolvedPriceView(productId, customerId, qty, item.get().getUnitPrice(),
                        list.getCurrency(), list.getId(), "PRICE_LIST");
            }
        }
        Money lp = p.getListPrice();
        if (lp == null || lp.amount() == null) {
            throw unprocessable("No price could be determined for product " + productId);
        }
        return new ResolvedPriceView(productId, customerId, qty, lp.amount(), lp.currency(), null, "LIST_PRICE");
    }

    /** Effective price list for a customer: explicitly assigned, otherwise by kind (reseller/list). */
    private PriceList priceListFor(Customer c) {
        if (c.getPriceListId() != null) {
            return priceLists.findById(c.getPriceListId()).orElse(null);
        }
        PriceListKind want = c.getKind() == CustomerKind.RESELLER ? PriceListKind.RESELLER : PriceListKind.LIST;
        return priceLists.findByKind(want).stream().findFirst().orElse(null);
    }

    private void validatePriceList(String priceListId) {
        if (priceListId != null && !priceLists.existsById(priceListId)) {
            throw unprocessable("Price list " + priceListId + " unknown");
        }
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException unprocessable(String msg) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
    }
}
