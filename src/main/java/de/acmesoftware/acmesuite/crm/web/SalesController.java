package de.acmesoftware.acmesuite.crm.web;

import de.acmesoftware.acmesuite.crm.CrmViews.LineView;
import de.acmesoftware.acmesuite.crm.CrmViews.OrderView;
import de.acmesoftware.acmesuite.crm.CrmViews.QuoteView;
import de.acmesoftware.acmesuite.crm.SalesService;
import de.acmesoftware.acmesuite.crm.SalesService.LineInput;
import de.acmesoftware.acmesuite.crm.domain.OrderStatus;
import de.acmesoftware.acmesuite.crm.domain.QuoteStatus;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** ACMEcrm HTTP API (Slice 2): quotes + orders incl. eFreigabe. */
@RestController
@RequestMapping("/api/crm")
public class SalesController {

    private final SalesService sales;

    public SalesController(SalesService sales) {
        this.sales = sales;
    }

    // ── Quotes ──
    @GetMapping("/quotes")
    public List<QuoteView> quotes(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) QuoteStatus status) {
        return sales.listQuotes(customerId, status);
    }

    @GetMapping("/quotes/{id}")
    public ResponseEntity<QuoteView> quote(@PathVariable String id) {
        return ResponseEntity.of(sales.getQuote(id));
    }

    @PostMapping("/quotes")
    public ResponseEntity<QuoteView> createQuote(@RequestBody QuoteWrite req) {
        QuoteView v = sales.createQuote(req.customerId(), req.validUntil(), lines(req.lines()));
        return ResponseEntity.created(URI.create("/api/crm/quotes/" + v.id())).body(v);
    }

    @PatchMapping("/quotes/{id}")
    public QuoteView updateQuote(@PathVariable String id, @RequestBody QuoteUpdate req) {
        return sales.updateQuote(id, req.status(), req.validUntil());
    }

    @PostMapping("/quotes/{id}/order")
    public ResponseEntity<OrderView> convert(@PathVariable String id) {
        OrderView v = sales.convertQuoteToOrder(id);
        return ResponseEntity.created(URI.create("/api/crm/orders/" + v.id())).body(v);
    }

    // ── Orders ──
    @GetMapping("/orders")
    public List<OrderView> orders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status) {
        return sales.listOrders(customerId, status);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderView> order(@PathVariable String id) {
        return ResponseEntity.of(sales.getOrder(id));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderView> createOrder(@RequestBody OrderWrite req) {
        OrderView v = sales.createOrder(req.customerId(), req.quoteId(), lines(req.lines()), req.note(),
                req.shippingMode());
        return ResponseEntity.created(URI.create("/api/crm/orders/" + v.id())).body(v);
    }

    @PostMapping("/orders/{id}/submission")
    public OrderView submit(@PathVariable String id) {
        return sales.submitOrder(id);
    }

    @PostMapping("/orders/{id}/decision")
    public OrderView decide(@PathVariable String id, @RequestBody DecisionReq req) {
        boolean approve = "APPROVE".equalsIgnoreCase(req.decision());
        if (!approve && !"REJECT".equalsIgnoreCase(req.decision())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "decision must be APPROVE or REJECT");
        }
        return sales.decideOrder(id, req.approverId(), approve, req.comment());
    }

    @PostMapping("/orders/{id}/fulfillment")
    public OrderView fulfill(@PathVariable String id) {
        return sales.fulfillOrder(id);
    }

    @PostMapping("/orders/{id}/cancellation")
    public OrderView cancel(@PathVariable String id) {
        return sales.cancelOrder(id);
    }

    /**
     * Record produced quantities per product (partial fulfillment). Maps the product-based
     * {@link ProductionReport} to the line-based {@code produce} operation (assumption: one order
     * line per product).
     */
    @PostMapping("/orders/{id}/production")
    public OrderView recordProduction(@PathVariable String id, @RequestBody ProductionReport req) {
        OrderView order = sales.getOrder(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        Map<String, Integer> byProduct = new HashMap<>();
        if (req.lines() != null) {
            for (ProducedLine l : req.lines()) {
                byProduct.merge(l.productId(), l.producedQuantity() == null ? 0 : l.producedQuantity(), Integer::sum);
            }
        }
        List<LineView> lines = order.lines();
        int[] produced = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            produced[i] = byProduct.getOrDefault(lines.get(i).productId(), 0);
        }
        sales.produce(id, produced);
        return sales.getOrder(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    /** Delete all orders (economy reset). */
    @DeleteMapping("/orders")
    public ResponseEntity<Void> deleteAllOrders() {
        sales.deleteAllOrders();
        return ResponseEntity.noContent().build();
    }

    private static List<LineInput> lines(List<LineWrite> ls) {
        return ls == null ? List.of() : ls.stream()
                .map(l -> new LineInput(l.productId(), l.quantity() == null ? 1 : l.quantity(),
                        l.unitPrice(), l.discountPercent()))
                .toList();
    }

    public record LineWrite(String productId, Integer quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
    }

    public record QuoteWrite(String customerId,
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                             List<LineWrite> lines) {
    }

    public record QuoteUpdate(QuoteStatus status,
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil) {
    }

    public record OrderWrite(String customerId, String quoteId, List<LineWrite> lines, String note,
                             String shippingMode) {
    }

    public record DecisionReq(String approverId, String decision, String comment) {
    }

    public record ProductionReport(List<ProducedLine> lines) {
    }

    public record ProducedLine(String productId, Integer producedQuantity) {
    }
}
