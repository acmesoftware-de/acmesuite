package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.CrmViews.ApprovalView;
import de.acmesoftware.acmesuite.crm.CrmViews.LineView;
import de.acmesoftware.acmesuite.crm.CrmViews.MoneyView;
import de.acmesoftware.acmesuite.crm.CrmViews.OrderView;
import de.acmesoftware.acmesuite.crm.CrmViews.QuoteView;
import de.acmesoftware.acmesuite.crm.domain.Customer;
import de.acmesoftware.acmesuite.crm.domain.CustomerRepository;
import de.acmesoftware.acmesuite.crm.domain.OrderLine;
import de.acmesoftware.acmesuite.crm.domain.OrderStatus;
import de.acmesoftware.acmesuite.crm.domain.ProductRepository;
import de.acmesoftware.acmesuite.crm.domain.Quote;
import de.acmesoftware.acmesuite.crm.domain.QuoteRepository;
import de.acmesoftware.acmesuite.crm.domain.QuoteStatus;
import de.acmesoftware.acmesuite.crm.domain.SalesOrder;
import de.acmesoftware.acmesuite.crm.domain.SalesOrderRepository;
import de.acmesoftware.acmesuite.shared.ApprovalAuthority;
import de.acmesoftware.acmesuite.shared.ManualApprovalRequested;
import de.acmesoftware.acmesuite.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ACMEcrm sales logic: quotes, orders and the eFreigabe (resolved via ACMEhr). */
@Service
@Transactional
public class SalesService {

    public record LineInput(String productId, int quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
    }

    private static final String CURRENCY = "EUR";

    private final QuoteRepository quotes;
    private final SalesOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final CrmService crm;
    private final ApprovalAuthority approvalAuthority;
    private final ApplicationEventPublisher events;
    private final BigDecimal threshold;

    public SalesService(QuoteRepository quotes, SalesOrderRepository orders, CustomerRepository customers,
                        ProductRepository products, CrmService crm, ApprovalAuthority approvalAuthority,
                        ApplicationEventPublisher events,
                        @Value("${acme.crm.approval-threshold-eur:10000}") BigDecimal threshold) {
        this.quotes = quotes;
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.crm = crm;
        this.approvalAuthority = approvalAuthority;
        this.events = events;
        this.threshold = threshold;
    }

    // ── Quotes ──
    @Transactional(readOnly = true)
    public List<QuoteView> listQuotes(String customerId, QuoteStatus status) {
        return (customerId != null ? quotes.findByCustomerId(customerId) : quotes.findAll()).stream()
                .filter(qu -> status == null || qu.getStatus() == status)
                .sorted(Comparator.comparing(Quote::getId))
                .map(this::toQuoteView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<QuoteView> getQuote(String id) {
        return quotes.findById(id).map(this::toQuoteView);
    }

    public QuoteView createQuote(String customerId, LocalDate validUntil, List<LineInput> lineInputs) {
        Customer c = requireCustomer(customerId);
        List<OrderLine> lines = resolveLines(c.getId(), lineInputs);
        Quote q = new Quote("q-" + shortId(), c.getId(), CURRENCY, validUntil, LocalDate.now(), lines);
        return toQuoteView(quotes.save(q));
    }

    public QuoteView updateQuote(String id, QuoteStatus status, LocalDate validUntil) {
        Quote q = quotes.findById(id).orElseThrow(() -> notFound("Quote " + id + " unknown"));
        if (status != null) {
            q.changeStatus(status);
        }
        if (validUntil != null) {
            q.setValidUntil(validUntil);
        }
        return toQuoteView(q);
    }

    public OrderView convertQuoteToOrder(String id) {
        Quote q = quotes.findById(id).orElseThrow(() -> notFound("Quote " + id + " unknown"));
        if (q.getStatus() == QuoteStatus.REJECTED || q.getStatus() == QuoteStatus.EXPIRED) {
            throw unprocessable("Quote " + id + " ist " + q.getStatus());
        }
        q.changeStatus(QuoteStatus.ACCEPTED);
        SalesOrder order = newOrder(q.getCustomerId(), q.getId(), List.copyOf(q.getLines()), null);
        return toOrderView(orders.save(order));
    }

    // ── Orders ──
    @Transactional(readOnly = true)
    public List<OrderView> listOrders(String customerId, OrderStatus status) {
        List<SalesOrder> base = customerId != null ? orders.findByCustomerId(customerId)
                : status != null ? orders.findByStatus(status) : orders.findAll();
        return base.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(Comparator.comparing(SalesOrder::getId))
                .map(this::toOrderView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderView> getOrder(String id) {
        return orders.findById(id).map(this::toOrderView);
    }

    /** Reset: discard all orders (fresh start). */
    public void deleteAllOrders() {
        orders.deleteAll();
    }

    /** Approved orders not yet fulfilled (demand for the daily production cycle), FIFO by order date. */
    @Transactional(readOnly = true)
    public List<OrderView> listApprovedOrders() {
        return orders.findByStatus(OrderStatus.APPROVED).stream()
                .sorted(Comparator.comparing(SalesOrder::getOrderDate).thenComparing(SalesOrder::getId))
                .map(this::toOrderView)
                .toList();
    }

    /**
     * Open orders = approved (APPROVED) or still in approval (PENDING_APPROVAL), FIFO. This is the
     * future material demand for demand-driven procurement (MRP): both states will still have to
     * supply the factory. Encapsulated here so the Supply module does not need to know the CRM status.
     */
    @Transactional(readOnly = true)
    public List<OrderView> listOpenOrders() {
        return java.util.stream.Stream.concat(
                        orders.findByStatus(OrderStatus.APPROVED).stream(),
                        orders.findByStatus(OrderStatus.PENDING_APPROVAL).stream())
                .sorted(Comparator.comparing(SalesOrder::getOrderDate).thenComparing(SalesOrder::getId))
                .map(this::toOrderView)
                .toList();
    }

    public OrderView createOrder(String customerId, String quoteId, List<LineInput> lineInputs, String note) {
        return createOrder(customerId, quoteId, lineInputs, note, "SHIP");
    }

    public OrderView createOrder(String customerId, String quoteId, List<LineInput> lineInputs, String note,
                                 String shippingMode) {
        Customer c = requireCustomer(customerId);
        List<OrderLine> lines = resolveLines(c.getId(), lineInputs);
        SalesOrder o = newOrder(c.getId(), quoteId, lines, note);
        o.setShippingMode(shippingMode);
        return toOrderView(orders.save(o));
    }

    /**
     * Submit for approval: at or above the threshold PENDING_APPROVAL, otherwise directly APPROVED. ACME
     * analog: above the threshold a signature folder is created in the folder workflow (event → process), which
     * approves the order once signed ({@link #markApproved}). The electronic path ({@link #decideOrder})
     * remains for "ACME digital".
     */
    public OrderView submitOrder(String id) {
        SalesOrder o = requireOrder(id);
        if (o.getStatus() != OrderStatus.CREATED) {
            throw unprocessable("Order " + id + " is " + o.getStatus() + " (not CREATED)");
        }
        boolean required = o.totalAmount().compareTo(threshold) > 0;
        o.submit(required);
        if (required) {
            events.publishEvent(new ManualApprovalRequested(o.getId(),
                    "Order " + customerName(o.getCustomerId()) + " · " + o.totalAmount() + " €",
                    o.totalAmount().longValue(), "SALES"));
        }
        return toOrderView(o);
    }

    /** Feedback from the folder workflow: the order's folder was signed → approved. */
    public void markApproved(String orderId, String approverKey) {
        orders.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == OrderStatus.PENDING_APPROVAL) {
                o.decide(approverKey, true, LocalDate.now(), "Folder signed");
            }
        });
    }

    /** eFreigabe decision: only by a person authorized to sign (ACMEhr approval limit). */
    public OrderView decideOrder(String id, String approverId, boolean approve, String comment) {
        SalesOrder o = requireOrder(id);
        if (o.getStatus() != OrderStatus.PENDING_APPROVAL) {
            throw unprocessable("Order " + id + " is " + o.getStatus() + " (not PENDING_APPROVAL)");
        }
        if (!approvalAuthority.canApprove(approverId, o.totalAmount(), CURRENCY, LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    approverId + " is not authorized to sign for " + o.totalAmount() + " " + CURRENCY);
        }
        o.decide(approverId, approve, LocalDate.now(), comment);
        return toOrderView(o);
    }

    public OrderView fulfillOrder(String id) {
        SalesOrder o = requireOrder(id);
        if (o.getStatus() != OrderStatus.APPROVED) {
            throw unprocessable("Order " + id + " is " + o.getStatus() + " (not APPROVED)");
        }
        o.getLines().forEach(l -> l.produce(l.remaining()));
        o.changeStatus(OrderStatus.FULFILLED);
        return toOrderView(o);
    }

    /**
     * Record partial production (daily production cycle): {@code producedByLine[i]} = units produced today
     * for the i-th line. A fully fulfilled order → FULFILLED, otherwise it stays APPROVED (backlog).
     */
    public void produce(String orderId, int[] producedByLine) {
        SalesOrder o = requireOrder(orderId);
        if (o.getStatus() != OrderStatus.APPROVED) {
            return;
        }
        List<OrderLine> lines = o.getLines();
        for (int i = 0; i < lines.size() && i < producedByLine.length; i++) {
            lines.get(i).produce(producedByLine[i]);
        }
        if (lines.stream().allMatch(l -> l.remaining() == 0)) {
            o.changeStatus(OrderStatus.FULFILLED);
        }
    }

    public OrderView cancelOrder(String id) {
        SalesOrder o = requireOrder(id);
        if (o.getStatus() == OrderStatus.FULFILLED || o.getStatus() == OrderStatus.CANCELLED) {
            throw unprocessable("Order " + id + " is " + o.getStatus());
        }
        o.changeStatus(OrderStatus.CANCELLED);
        return toOrderView(o);
    }

    // ── helpers ──
    private SalesOrder newOrder(String customerId, String quoteId, List<OrderLine> lines, String note) {
        BigDecimal total = lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesOrder("ord-" + shortId(), customerId, quoteId, LocalDate.now(),
                new Money(total, CURRENCY), note, lines);
    }

    private List<OrderLine> resolveLines(String customerId, List<LineInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw unprocessable("at least one line item is required");
        }
        return inputs.stream().map(in -> {
            if (!products.existsById(in.productId())) {
                throw notFound("Product " + in.productId() + " unknown");
            }
            BigDecimal price = in.unitPrice() != null ? in.unitPrice()
                    : crm.resolvePrice(customerId, in.productId(), in.quantity()).unitPrice();
            return new OrderLine(in.productId(), in.quantity(), price, in.discountPercent());
        }).toList();
    }

    private List<LineView> lineViews(List<OrderLine> lines) {
        Map<String, String> names = products.findAllById(
                        lines.stream().map(OrderLine::getProductId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        return lines.stream().map(l -> new LineView(l.getProductId(), names.get(l.getProductId()),
                l.getQuantity(), l.getUnitPrice(), l.getDiscountPercent(), l.lineTotal(), l.remaining())).toList();
    }

    private String customerName(String id) {
        return customers.findById(id).map(Customer::getName).orElse(id);
    }

    private String customerCountry(String id) {
        return customers.findById(id).map(Customer::getCountry).orElse(null);
    }

    private QuoteView toQuoteView(Quote q) {
        return new QuoteView(q.getId(), q.getCustomerId(), customerName(q.getCustomerId()), q.getStatus().name(),
                q.getCurrency(), q.getValidUntil(), lineViews(q.getLines()), q.netTotal(), q.getCreatedOn());
    }

    private OrderView toOrderView(SalesOrder o) {
        ApprovalView ap = new ApprovalView(o.isApprovalRequired(), o.getApproverId(), o.getApprovalDecision(),
                o.getApprovalDecidedOn(), o.getApprovalComment());
        return new OrderView(o.getId(), o.getCustomerId(), customerName(o.getCustomerId()),
                customerCountry(o.getCustomerId()), o.getQuoteId(),
                o.getStatus().name(), o.getOrderDate(), lineViews(o.getLines()), MoneyView.of(o.getTotal()),
                ap, o.getNote(), o.getShippingMode());
    }

    private Customer requireCustomer(String id) {
        return customers.findById(id).orElseThrow(() -> notFound("Customer " + id + " unknown"));
    }

    private SalesOrder requireOrder(String id) {
        return orders.findById(id).orElseThrow(() -> notFound("Order " + id + " unknown"));
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException unprocessable(String msg) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
    }
}
