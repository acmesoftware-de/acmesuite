package de.acmesoftware.acmesuite.supply.web;

import de.acmesoftware.acmesuite.supply.SupplyService;
import de.acmesoftware.acmesuite.supply.SupplyService.LineInput;
import de.acmesoftware.acmesuite.supply.SupplyViews.MaterialView;
import de.acmesoftware.acmesuite.supply.SupplyViews.ResolvedSupplyPriceView;
import de.acmesoftware.acmesuite.supply.SupplyViews.StockView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplierView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplyContractView;
import de.acmesoftware.acmesuite.supply.SupplyViews.SupplyOrderView;
import de.acmesoftware.acmesuite.supply.domain.MaterialKind;
import de.acmesoftware.acmesuite.supply.domain.SupplierStatus;
import de.acmesoftware.acmesuite.supply.domain.SupplyOrderStatus;
import de.acmesoftware.acmesuite.supply.domain.Tier;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** ACMEsupply HTTP API: suppliers, materials, supply contracts + procurements incl. approval. */
@RestController
@RequestMapping("/api/supply")
public class SupplyController {

    private final SupplyService supply;

    public SupplyController(SupplyService supply) {
        this.supply = supply;
    }

    // ── Suppliers ──
    @GetMapping("/suppliers")
    public List<SupplierView> suppliers(@RequestParam(required = false) SupplierStatus status,
                                        @RequestParam(required = false) String q) {
        return supply.listSuppliers(status, q);
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierView> supplier(@PathVariable String id) {
        return ResponseEntity.of(supply.getSupplier(id));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierView> createSupplier(@RequestBody SupplierWrite req) {
        SupplierView v = supply.createSupplier(req.name(), req.status(), req.email(), req.country());
        return ResponseEntity.created(URI.create("/api/supply/suppliers/" + v.id())).body(v);
    }

    @PatchMapping("/suppliers/{id}")
    public SupplierView updateSupplier(@PathVariable String id, @RequestBody SupplierWrite req) {
        return supply.updateSupplier(id, req.name(), req.status(), req.email(), req.country());
    }

    // ── Materials ──
    @GetMapping("/materials")
    public List<MaterialView> materials(@RequestParam(required = false) MaterialKind kind,
                                        @RequestParam(required = false) String q) {
        return supply.listMaterials(kind, q);
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<MaterialView> material(@PathVariable String id) {
        return ResponseEntity.of(supply.getMaterial(id));
    }

    @GetMapping("/stock")
    public List<StockView> stock() {
        return supply.stockLevels();
    }

    @PostMapping("/materials")
    public ResponseEntity<MaterialView> createMaterial(@RequestBody MaterialWrite req) {
        MaterialView v = supply.createMaterial(req.code(), req.name(), req.kind(), req.unit());
        return ResponseEntity.created(URI.create("/api/supply/materials/" + v.id())).body(v);
    }

    @PatchMapping("/materials/{id}")
    public MaterialView updateMaterial(@PathVariable String id, @RequestBody MaterialWrite req) {
        return supply.updateMaterial(id, req.code(), req.name(), req.kind(), req.unit());
    }

    // ── Stock ──
    @GetMapping("/materials/{id}/stock")
    public MaterialStockResponse stockOf(@PathVariable String id) {
        requireMaterial(id);
        return new MaterialStockResponse(id, supply.availableStock(id));
    }

    @PostMapping("/materials/{id}/stock/consumption")
    public MaterialStockResponse consume(@PathVariable String id, @RequestBody StockConsumptionReq req) {
        requireMaterial(id);
        supply.consumeStock(id, req.quantity());
        return new MaterialStockResponse(id, supply.availableStock(id));
    }

    @PostMapping("/stock/reset")
    public ResponseEntity<Void> resetStock() {
        supply.resetStock();
        return ResponseEntity.noContent().build();
    }

    private void requireMaterial(String id) {
        if (supply.getMaterial(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material not found");
        }
    }

    // ── Supply contracts ──
    @GetMapping("/supply-contracts")
    public List<SupplyContractView> contracts(@RequestParam(required = false) String supplierId,
                                              @RequestParam(required = false) String materialId) {
        return supply.listContracts(supplierId, materialId);
    }

    @GetMapping("/supply-contracts/{id}")
    public ResponseEntity<SupplyContractView> contract(@PathVariable String id) {
        return ResponseEntity.of(supply.getContract(id));
    }

    @PostMapping("/supply-contracts")
    public ResponseEntity<SupplyContractView> createContract(@RequestBody SupplyContractWrite req) {
        List<Tier> tiers = req.tiers() == null ? List.of() : req.tiers().stream()
                .map(t -> new Tier(t.minQuantity() == null ? 1 : t.minQuantity(), t.unitPrice())).toList();
        SupplyContractView v = supply.createContract(req.supplierId(), req.materialId(), req.currency(),
                req.leadTimeDays() == null ? 0 : req.leadTimeDays(), req.validFrom(), req.validUntil(), tiers);
        return ResponseEntity.created(URI.create("/api/supply/supply-contracts/" + v.id())).body(v);
    }

    @GetMapping("/procurement-price")
    public ResolvedSupplyPriceView price(@RequestParam String supplierId, @RequestParam String materialId,
                                         @RequestParam(defaultValue = "1") int quantity) {
        return supply.resolvePrice(supplierId, materialId, quantity);
    }

    // ── Supply orders ──
    @GetMapping("/supply-orders")
    public List<SupplyOrderView> orders(@RequestParam(required = false) String supplierId,
                                        @RequestParam(required = false) SupplyOrderStatus status) {
        return supply.listOrders(supplierId, status);
    }

    @GetMapping("/supply-orders/{id}")
    public ResponseEntity<SupplyOrderView> order(@PathVariable String id) {
        return ResponseEntity.of(supply.getOrder(id));
    }

    @PostMapping("/supply-orders")
    public ResponseEntity<SupplyOrderView> createOrder(@RequestBody SupplyOrderWrite req) {
        List<LineInput> lines = req.lines() == null ? List.of() : req.lines().stream()
                .map(l -> new LineInput(l.materialId(), l.quantity() == null ? 1 : l.quantity(), l.unitPrice()))
                .toList();
        SupplyOrderView v = supply.createOrder(req.supplierId(), lines, req.note(), req.deliveryMode());
        return ResponseEntity.created(URI.create("/api/supply/supply-orders/" + v.id())).body(v);
    }

    @PostMapping("/supply-orders/{id}/submission")
    public SupplyOrderView submit(@PathVariable String id) {
        return supply.submitOrder(id);
    }

    @PostMapping("/supply-orders/{id}/decision")
    public SupplyOrderView decide(@PathVariable String id, @RequestBody DecisionReq req) {
        boolean approve = "APPROVE".equalsIgnoreCase(req.decision());
        if (!approve && !"REJECT".equalsIgnoreCase(req.decision())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "decision must be APPROVE or REJECT");
        }
        return supply.decideOrder(id, req.approverId(), approve, req.comment());
    }

    @PostMapping("/supply-orders/{id}/receipt")
    public SupplyOrderView receive(@PathVariable String id) {
        return supply.receiveOrder(id);
    }

    @PostMapping("/supply-orders/{id}/cancellation")
    public SupplyOrderView cancel(@PathVariable String id) {
        return supply.cancelOrder(id);
    }

    public record SupplierWrite(String name, SupplierStatus status, String email, String country) {
    }

    public record MaterialWrite(String code, String name, MaterialKind kind, String unit) {
    }

    public record TierWrite(Integer minQuantity, BigDecimal unitPrice) {
    }

    public record SupplyContractWrite(String supplierId, String materialId, String currency, Integer leadTimeDays,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                                      List<TierWrite> tiers) {
    }

    public record LineWrite(String materialId, Integer quantity, BigDecimal unitPrice) {
    }

    public record SupplyOrderWrite(String supplierId, List<LineWrite> lines, String note, String deliveryMode) {
    }

    public record DecisionReq(String approverId, String decision, String comment) {
    }

    public record StockConsumptionReq(BigDecimal quantity) {
    }

    public record MaterialStockResponse(String materialId, BigDecimal available) {
    }
}
