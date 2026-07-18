package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.ShopfloorViews.MachineView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ProductionOrderView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ShiftPlanView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ShiftRowView;
import de.acmesoftware.acmesuite.build.domain.MachineRepository;
import de.acmesoftware.acmesuite.build.domain.OrderStage;
import de.acmesoftware.acmesuite.build.domain.ProductionOrder;
import de.acmesoftware.acmesuite.build.domain.ProductionOrderRepository;
import de.acmesoftware.acmesuite.build.domain.ShiftCell;
import de.acmesoftware.acmesuite.build.domain.ShiftId;
import de.acmesoftware.acmesuite.build.domain.ShiftPlanRow;
import de.acmesoftware.acmesuite.build.domain.ShiftPlanRowRepository;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ACMEbuild shop-floor logic: the production planning board, the weekly shift plan and the live
 * machine monitor (contract v0.2.0). Kept separate from {@link BuildService} (BOM master data)
 * as this is operational execution state.
 */
@Service
@Transactional
public class ShopfloorService {

    /** Canonical shift metadata (order, label, time window) — the source of truth for the plan shape. */
    public record ShiftMeta(ShiftId id, int ord, String label, String time) {
    }

    public static final List<ShiftMeta> SHIFTS = List.of(
            new ShiftMeta(ShiftId.EARLY, 0, "Frühschicht", "06–14"),
            new ShiftMeta(ShiftId.LATE, 1, "Spätschicht", "14–22"),
            new ShiftMeta(ShiftId.NIGHT, 2, "Nachtschicht", "22–06"));

    private final ProductionOrderRepository orders;
    private final ShiftPlanRowRepository shifts;
    private final MachineRepository machines;

    public ShopfloorService(ProductionOrderRepository orders, ShiftPlanRowRepository shifts,
                            MachineRepository machines) {
        this.orders = orders;
        this.shifts = shifts;
        this.machines = machines;
    }

    // ── Production orders ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProductionOrderView> listOrders(OrderStage stage) {
        List<ProductionOrder> rows = stage == null
                ? orders.findAllByOrderByOrderNo()
                : orders.findByStageOrderByOrderNo(stage);
        return rows.stream().map(ProductionOrderView::of).toList();
    }

    public ProductionOrderView createOrder(String productId, Integer quantity, String machine,
                                           String ownerInitials, OrderStage stage, LocalDate dueDate) {
        if (productId == null || productId.isBlank() || quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "productId and a quantity >= 1 are required");
        }
        ProductionOrder o = new ProductionOrder("fa-" + shortId(), nextOrderNo(), productId, null,
                quantity, blankToNull(machine), upper(ownerInitials),
                stage == null ? OrderStage.GEPLANT : stage, dueDate);
        return ProductionOrderView.of(orders.save(o));
    }

    public Optional<ProductionOrderView> updateOrder(String id, OrderStage stage, String machine,
                                                     String ownerInitials) {
        return orders.findById(id).map(o -> {
            o.apply(stage, blankToNull(machine), upper(ownerInitials));
            return ProductionOrderView.of(orders.save(o));
        });
    }

    private String nextOrderNo() {
        int max = orders.findAll().stream()
                .map(ProductionOrder::getOrderNo)
                .filter(n -> n != null && n.startsWith("FA-"))
                .mapToInt(n -> {
                    try {
                        return Integer.parseInt(n.substring(3));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max().orElse(1050);
        return "FA-" + (max + 1);
    }

    // ── Shift plan ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ShiftPlanView getShiftPlan() {
        List<ShiftRowView> rows = shifts.findAllByOrderByOrd().stream().map(ShiftRowView::of).toList();
        return new ShiftPlanView(currentWeekLabel(), rows);
    }

    /** Replaces the staffing of each given shift; missing rows are seeded from the canonical shape. */
    public ShiftPlanView putShiftPlan(List<ShiftInput> input) {
        for (ShiftMeta meta : SHIFTS) {
            List<ShiftCell> cells = input.stream()
                    .filter(i -> i.shift() == meta.id())
                    .map(ShiftInput::cells)
                    .findFirst()
                    .orElse(null);
            if (cells == null) {
                continue;
            }
            ShiftPlanRow row = shifts.findById(meta.id())
                    .map(r -> {
                        r.setCells(cells);
                        return r;
                    })
                    .orElseGet(() -> new ShiftPlanRow(meta.id(), meta.ord(), meta.label(), meta.time(), cells));
            shifts.save(row);
        }
        return getShiftPlan();
    }

    public record ShiftInput(ShiftId shift, List<ShiftCell> cells) {
    }

    private static String currentWeekLabel() {
        return "KW " + LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear());
    }

    // ── Machines ───────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<MachineView> listMachines() {
        return machines.findAllByOrderByName().stream().map(MachineView::of).toList();
    }

    // ── helpers ────────────────────────────────────────────────────────────
    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String upper(String s) {
        String v = blankToNull(s);
        return v == null ? null : v.toUpperCase();
    }
}
