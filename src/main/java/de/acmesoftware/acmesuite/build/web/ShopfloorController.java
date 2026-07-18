package de.acmesoftware.acmesuite.build.web;

import de.acmesoftware.acmesuite.build.ShopfloorService;
import de.acmesoftware.acmesuite.build.ShopfloorService.ShiftInput;
import de.acmesoftware.acmesuite.build.ShopfloorViews.MachineView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ProductionOrderView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ShiftPlanView;
import de.acmesoftware.acmesuite.build.domain.OrderStage;
import de.acmesoftware.acmesuite.build.domain.ShiftCell;
import de.acmesoftware.acmesuite.build.domain.ShiftId;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ACMEbuild shop-floor HTTP API (contract v0.2.0): the production planning board, the weekly
 * shift plan and the machine monitor. Reads need WATCH, writes WORK — enforced centrally by the
 * path rules in Base's security config, so no per-method guards are required here.
 */
@RestController
@RequestMapping("/api/build")
public class ShopfloorController {

    private final ShopfloorService shopfloor;

    public ShopfloorController(ShopfloorService shopfloor) {
        this.shopfloor = shopfloor;
    }

    // ── Production orders ──
    @GetMapping("/production-orders")
    public List<ProductionOrderView> productionOrders(@RequestParam(required = false) OrderStage stage) {
        return shopfloor.listOrders(stage);
    }

    @PostMapping("/production-orders")
    public ResponseEntity<ProductionOrderView> createOrder(@RequestBody ProductionOrderCreate req) {
        ProductionOrderView v = shopfloor.createOrder(req.productId(), req.quantity(), req.machine(),
                req.ownerInitials(), req.stage(), req.dueDate());
        return ResponseEntity.created(URI.create("/api/build/production-orders/" + v.id())).body(v);
    }

    @PatchMapping("/production-orders/{id}")
    public ResponseEntity<ProductionOrderView> updateOrder(@PathVariable String id,
                                                           @RequestBody ProductionOrderUpdate req) {
        return ResponseEntity.of(shopfloor.updateOrder(id, req.stage(), req.machine(), req.ownerInitials()));
    }

    // ── Shift plan ──
    @GetMapping("/shift-plan")
    public ShiftPlanView shiftPlan() {
        return shopfloor.getShiftPlan();
    }

    @PutMapping("/shift-plan")
    public ShiftPlanView putShiftPlan(@RequestBody ShiftPlanWrite req) {
        List<ShiftInput> rows = req.rows() == null ? List.of() : req.rows().stream()
                .map(r -> new ShiftInput(r.shift(), r.cells() == null ? List.of() : r.cells()))
                .toList();
        return shopfloor.putShiftPlan(rows);
    }

    // ── Machines ──
    @GetMapping("/machines")
    public List<MachineView> machines() {
        return shopfloor.listMachines();
    }

    // ── request bodies ──
    public record ProductionOrderCreate(String productId, Integer quantity, String machine,
                                        String ownerInitials, OrderStage stage, LocalDate dueDate) {
    }

    public record ProductionOrderUpdate(OrderStage stage, String machine, String ownerInitials) {
    }

    public record ShiftRowWrite(ShiftId shift, List<ShiftCell> cells) {
    }

    public record ShiftPlanWrite(List<ShiftRowWrite> rows) {
    }
}
