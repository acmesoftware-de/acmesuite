package de.acmesoftware.acmesuite.supply;

import de.acmesoftware.acmesuite.shared.ApprovalAuthority;
import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.ManualApprovalRequested;
import de.acmesoftware.acmesuite.shared.Money;
import de.acmesoftware.acmesuite.supply.SupplyViews.ApprovalView;
import de.acmesoftware.acmesuite.supply.SupplyViews.LineView;
import de.acmesoftware.acmesuite.supply.SupplyViews.MaterialView;
import de.acmesoftware.acmesuite.supply.SupplyViews.MoneyView;
import de.acmesoftware.acmesuite.supply.SupplyViews.ResolvedSupplyPriceView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplierView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplyContractView;
import de.acmesoftware.acmesuite.supply.SupplyViews.StockView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplyOrderView;
import de.acmesoftware.acmesuite.supply.SupplyViews.TierView;
import de.acmesoftware.acmesuite.supply.domain.Material;
import de.acmesoftware.acmesuite.supply.domain.MaterialKind;
import de.acmesoftware.acmesuite.supply.domain.MaterialRepository;
import de.acmesoftware.acmesuite.supply.domain.MaterialStock;
import de.acmesoftware.acmesuite.supply.domain.MaterialStockRepository;
import de.acmesoftware.acmesuite.supply.domain.Supplier;
import de.acmesoftware.acmesuite.supply.domain.SupplierRepository;
import de.acmesoftware.acmesuite.supply.domain.SupplierStatus;
import de.acmesoftware.acmesuite.supply.domain.SupplyContract;
import de.acmesoftware.acmesuite.supply.domain.SupplyContractRepository;
import de.acmesoftware.acmesuite.supply.domain.SupplyLine;
import de.acmesoftware.acmesuite.supply.domain.SupplyOrder;
import de.acmesoftware.acmesuite.supply.domain.SupplyOrderRepository;
import de.acmesoftware.acmesuite.supply.domain.SupplyOrderStatus;
import de.acmesoftware.acmesuite.supply.domain.Tier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ACMEsupply application logic: suppliers, materials, supply contracts + procurements incl. approval. */
@Service
@Transactional
public class SupplyService {

    public record LineInput(String materialId, int quantity, BigDecimal unitPrice) {
    }

    private static final String CURRENCY = "EUR";

    private final SupplierRepository suppliers;
    private final MaterialRepository materials;
    private final SupplyContractRepository contracts;
    private final SupplyOrderRepository orders;
    private final MaterialStockRepository stock;
    private final ApprovalAuthority approvalAuthority;
    private final ApplicationEventPublisher events;
    private final BigDecimal threshold;

    public SupplyService(SupplierRepository suppliers, MaterialRepository materials,
                         SupplyContractRepository contracts, SupplyOrderRepository orders,
                         MaterialStockRepository stock, ApprovalAuthority approvalAuthority,
                         ApplicationEventPublisher events,
                         @Value("${acme.supply.approval-threshold-eur:10000}") BigDecimal threshold) {
        this.suppliers = suppliers;
        this.materials = materials;
        this.contracts = contracts;
        this.orders = orders;
        this.stock = stock;
        this.approvalAuthority = approvalAuthority;
        this.events = events;
        this.threshold = threshold;
    }

    // ── Suppliers ──
    @Transactional(readOnly = true)
    public List<SupplierView> listSuppliers(SupplierStatus status, String q) {
        String n = q == null ? null : q.toLowerCase();
        return suppliers.findAll().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> n == null || s.getName().toLowerCase().contains(n))
                .sorted(Comparator.comparing(Supplier::getName))
                .map(SupplierView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<SupplierView> getSupplier(String id) {
        return suppliers.findById(id).map(SupplierView::of);
    }

    public SupplierView createSupplier(String name, SupplierStatus status, String email, String country) {
        if (name == null || name.isBlank()) {
            throw unprocessable("name is required");
        }
        Supplier s = new Supplier("sup-" + sid(), name, status, email, country);
        return SupplierView.of(suppliers.save(s));
    }

    public SupplierView updateSupplier(String id, String name, SupplierStatus status, String email, String country) {
        Supplier s = suppliers.findById(id).orElseThrow(() -> notFound("Supplier " + id + " unknown"));
        s.update(name, status, email, country);
        return SupplierView.of(s);
    }

    // ── Materials ──
    @Transactional(readOnly = true)
    public List<MaterialView> listMaterials(MaterialKind kind, String q) {
        String n = q == null ? null : q.toLowerCase();
        return materials.findAll().stream()
                .filter(m -> kind == null || m.getKind() == kind)
                .filter(m -> n == null || m.getName().toLowerCase().contains(n) || m.getCode().toLowerCase().contains(n))
                .sorted(Comparator.comparing(Material::getName))
                .map(MaterialView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MaterialView> getMaterial(String id) {
        return materials.findById(id).map(MaterialView::of);
    }

    public MaterialView createMaterial(String code, String name, MaterialKind kind, String unit) {
        if (code == null || name == null || kind == null) {
            throw unprocessable("code, name and kind are required");
        }
        Material m = new Material("mat-" + sid(), code, name, kind, unit);
        return MaterialView.of(materials.save(m));
    }

    public MaterialView updateMaterial(String id, String code, String name, MaterialKind kind, String unit) {
        Material m = materials.findById(id).orElseThrow(() -> notFound("Material " + id + " unknown"));
        m.update(code, name, kind, unit);
        return MaterialView.of(m);
    }

    // ── Supply contracts ──
    @Transactional(readOnly = true)
    public List<SupplyContractView> listContracts(String supplierId, String materialId) {
        List<SupplyContract> base = supplierId != null ? contracts.findBySupplierId(supplierId)
                : materialId != null ? contracts.findByMaterialId(materialId) : contracts.findAll();
        return base.stream()
                .filter(c -> materialId == null || materialId.equals(c.getMaterialId()))
                .map(this::toContractView).toList();
    }

    @Transactional(readOnly = true)
    public Optional<SupplyContractView> getContract(String id) {
        return contracts.findById(id).map(this::toContractView);
    }

    public SupplyContractView createContract(String supplierId, String materialId, String currency,
                                             int leadTimeDays, LocalDate from, LocalDate until, List<Tier> tiers) {
        if (!suppliers.existsById(supplierId)) {
            throw notFound("Supplier " + supplierId + " unknown");
        }
        if (!materials.existsById(materialId)) {
            throw notFound("Material " + materialId + " unknown");
        }
        if (tiers == null || tiers.isEmpty()) {
            throw unprocessable("at least one price tier is required");
        }
        SupplyContract c = new SupplyContract("sc-" + sid(), supplierId, materialId,
                currency == null ? CURRENCY : currency, leadTimeDays, new DateRange(from, until), tiers);
        return toContractView(contracts.save(c));
    }

    // ── Price / lead-time resolution ──
    @Transactional(readOnly = true)
    public ResolvedSupplyPriceView resolvePrice(String supplierId, String materialId, int quantity) {
        SupplyContract c = contracts.findFirstBySupplierIdAndMaterialId(supplierId, materialId)
                .orElseThrow(() -> notFound("No supply contract for " + supplierId + "/" + materialId));
        int qty = Math.max(1, quantity);
        BigDecimal price = bestTierPrice(c, qty);
        return new ResolvedSupplyPriceView(supplierId, materialId, qty, price, c.getCurrency(),
                c.getLeadTimeDays(), c.getId());
    }

    private BigDecimal bestTierPrice(SupplyContract c, int qty) {
        return c.getTiers().stream()
                .filter(t -> t.getMinQuantity() <= qty)
                .max(Comparator.comparingInt(Tier::getMinQuantity))
                .map(Tier::getUnitPrice)
                .orElseThrow(() -> unprocessable("No matching price tier for quantity " + qty));
    }

    // ── Supply orders ──
    @Transactional(readOnly = true)
    public List<SupplyOrderView> listOrders(String supplierId, SupplyOrderStatus status) {
        List<SupplyOrder> base = supplierId != null ? orders.findBySupplierId(supplierId)
                : status != null ? orders.findByStatus(status) : orders.findAll();
        return base.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .sorted(Comparator.comparing(SupplyOrder::getId))
                .map(this::toOrderView).toList();
    }

    /**
     * Like {@link #listOrders(String, SupplyOrderStatus)}, but with the status as a String — so that
     * callers outside the module (the integration seam) do not need to know the internal {@code SupplyOrderStatus} enum.
     */
    public List<SupplyOrderView> listOrders(String supplierId, String statusName) {
        return listOrders(supplierId, statusName == null ? null : SupplyOrderStatus.valueOf(statusName));
    }

    @Transactional(readOnly = true)
    public Optional<SupplyOrderView> getOrder(String id) {
        return orders.findById(id).map(this::toOrderView);
    }

    public SupplyOrderView createOrder(String supplierId, List<LineInput> inputs, String note, String deliveryMode) {
        Supplier s = suppliers.findById(supplierId)
                .orElseThrow(() -> notFound("Supplier " + supplierId + " unknown"));
        if (inputs == null || inputs.isEmpty()) {
            throw unprocessable("at least one line item is required");
        }
        boolean air = "AIRPLANE".equals(deliveryMode);
        int maxLead = 0;
        BigDecimal total = BigDecimal.ZERO;
        java.util.List<SupplyLine> lines = new java.util.ArrayList<>();
        for (LineInput in : inputs) {
            if (!materials.existsById(in.materialId())) {
                throw notFound("Material " + in.materialId() + " unknown");
            }
            SupplyContract c = contracts.findFirstBySupplierIdAndMaterialId(s.getId(), in.materialId()).orElse(null);
            BigDecimal price = in.unitPrice() != null ? in.unitPrice()
                    : c != null ? bestTierPrice(c, in.quantity())
                    : unprocessableThrow("No price/contract for material " + in.materialId());
            if (c != null) {
                maxLead = Math.max(maxLead, c.getLeadTimeDays());
            }
            SupplyLine line = new SupplyLine(in.materialId(), in.quantity(), price);
            total = total.add(line.lineTotal());
            lines.add(line);
        }
        // Delivery mode: airplane is significantly faster, but incurs a 30% air-freight surcharge.
        int lead = air ? Math.max(1, maxLead / 3) : maxLead;
        BigDecimal finalTotal = air ? total.multiply(new BigDecimal("1.30")).setScale(2, java.math.RoundingMode.HALF_UP)
                : total;
        LocalDate today = LocalDate.now();
        SupplyOrder o = new SupplyOrder("po-" + sid(), s.getId(), today, today.plusDays(lead),
                new Money(finalTotal, CURRENCY), note, lines);
        o.setDeliveryMode(air ? "AIRPLANE" : "SHIP");
        return toOrderView(orders.save(o));
    }

    /** Submit for approval: above the threshold → PENDING_APPROVAL (ACME analog: blue purchasing folder), otherwise APPROVED. */
    public SupplyOrderView submitOrder(String id) {
        SupplyOrder o = requireOrder(id);
        if (o.getStatus() != SupplyOrderStatus.CREATED) {
            throw unprocessable("Procurement " + id + " is " + o.getStatus() + " (not CREATED)");
        }
        boolean required = o.totalAmount().compareTo(threshold) > 0;
        o.submit(required);
        if (required) {
            events.publishEvent(new ManualApprovalRequested(o.getId(),
                    "Procurement " + supplierName(o.getSupplierId()) + " · " + o.totalAmount() + " €",
                    o.totalAmount().longValue(), "PURCHASE"));
        }
        return toOrderView(o);
    }

    public SupplyOrderView decideOrder(String id, String approverId, boolean approve, String comment) {
        SupplyOrder o = requireOrder(id);
        if (o.getStatus() != SupplyOrderStatus.PENDING_APPROVAL) {
            throw unprocessable("Procurement " + id + " is " + o.getStatus() + " (not PENDING_APPROVAL)");
        }
        if (!approvalAuthority.canApprove(approverId, o.totalAmount(), CURRENCY, LocalDate.now())) {
            throw unprocessable(approverId + " is not authorized to sign for " + o.totalAmount() + " " + CURRENCY);
        }
        o.decide(approverId, approve, LocalDate.now(), comment);
        return toOrderView(o);
    }

    /** Feedback from the bustle: purchasing folder signed → procurement approved. */
    public void markApproved(String orderId, String approverKey) {
        orders.findById(orderId).ifPresent(o -> {
            if (o.getStatus() == SupplyOrderStatus.PENDING_APPROVAL) {
                o.decide(approverKey, true, LocalDate.now(), "Folder signed");
            }
        });
    }

    public SupplyOrderView receiveOrder(String id) {
        SupplyOrder o = requireOrder(id);
        if (o.getStatus() != SupplyOrderStatus.APPROVED) {
            throw unprocessable("Procurement " + id + " is " + o.getStatus() + " (not APPROVED)");
        }
        o.changeStatus(SupplyOrderStatus.RECEIVED);
        // Goods receipt replenishes the warehouse (only stocked raw materials; energy flows).
        for (SupplyLine line : o.getLines()) {
            stock.findById(line.getMaterialId())
                    .ifPresent(s -> s.receive(BigDecimal.valueOf(line.getQuantity())));
        }
        return toOrderView(o);
    }

    // ── Warehouse (stock/consumption) ──

    @Transactional(readOnly = true)
    public List<StockView> stockLevels() {
        return stock.findAll().stream()
                .map(s -> StockView.of(s, materialName(s.getMaterialId())))
                .sorted(Comparator.comparing(StockView::materialName))
                .toList();
    }

    /** Available stock level of a raw material (0 if not stocked). */
    @Transactional(readOnly = true)
    public BigDecimal availableStock(String materialId) {
        return stock.findById(materialId).map(MaterialStock::getQuantity).orElse(BigDecimal.ZERO);
    }

    /** Withdraws from the warehouse; returns the quantity actually withdrawn (≤ stock). */
    public BigDecimal consumeStock(String materialId, BigDecimal want) {
        return stock.findById(materialId).map(s -> s.consume(want)).orElse(BigDecimal.ZERO);
    }

    /** Reset: replenish stock levels to the initial state (cf. SupplySeeder.seedStock). */
    public void resetStock() {
        setStock("mat-stahl", "8000");
        setStock("mat-holz", "6000");
        setStock("mat-elektronik", "12000");
        setStock("mat-despotium", "1500");
    }

    private void setStock(String materialId, String qty) {
        stock.findById(materialId).ifPresent(s -> s.setQuantity(new BigDecimal(qty)));
    }

    public SupplyOrderView cancelOrder(String id) {
        SupplyOrder o = requireOrder(id);
        if (o.getStatus() == SupplyOrderStatus.RECEIVED || o.getStatus() == SupplyOrderStatus.CANCELLED) {
            throw unprocessable("Procurement " + id + " is " + o.getStatus());
        }
        o.changeStatus(SupplyOrderStatus.CANCELLED);
        return toOrderView(o);
    }

    // ── helpers ──
    private SupplyContractView toContractView(SupplyContract c) {
        return new SupplyContractView(c.getId(), c.getSupplierId(), supplierName(c.getSupplierId()),
                c.getMaterialId(), materialName(c.getMaterialId()), c.getCurrency(), c.getLeadTimeDays(),
                c.getValidity(), c.getTiers().stream().map(TierView::of).toList());
    }

    private SupplyOrderView toOrderView(SupplyOrder o) {
        List<LineView> lines = o.getLines().stream()
                .map(l -> new LineView(l.getMaterialId(), materialName(l.getMaterialId()), l.getQuantity(),
                        l.getUnitPrice(), l.lineTotal()))
                .toList();
        ApprovalView ap = new ApprovalView(o.isApprovalRequired(), o.getApproverId(), o.getApprovalDecision(),
                o.getApprovalDecidedOn(), o.getApprovalComment());
        return new SupplyOrderView(o.getId(), o.getSupplierId(), supplierName(o.getSupplierId()),
                o.getStatus().name(), o.getOrderDate(), o.getExpectedDeliveryDate(), lines,
                MoneyView.of(o.getTotal()), ap, o.getNote(), o.getDeliveryMode());
    }

    private String supplierName(String id) {
        return suppliers.findById(id).map(Supplier::getName).orElse(id);
    }

    private String materialName(String id) {
        return materials.findById(id).map(Material::getName).orElse(id);
    }

    private SupplyOrder requireOrder(String id) {
        return orders.findById(id).orElseThrow(() -> notFound("Procurement " + id + " unknown"));
    }

    private static String sid() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private static BigDecimal unprocessableThrow(String msg) {
        throw unprocessable(msg);
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException unprocessable(String msg) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
    }
}
