package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.domain.Machine;
import de.acmesoftware.acmesuite.build.domain.MachineStatus;
import de.acmesoftware.acmesuite.build.domain.OrderStage;
import de.acmesoftware.acmesuite.build.domain.ProductionOrder;
import de.acmesoftware.acmesuite.build.domain.ShiftCell;
import de.acmesoftware.acmesuite.build.domain.ShiftId;
import de.acmesoftware.acmesuite.build.domain.ShiftPlanRow;
import java.time.LocalDate;
import java.util.List;

/** Serializable ACMEbuild shop-floor views (production orders, shift plan, machines). */
public final class ShopfloorViews {

    private ShopfloorViews() {
    }

    public record ProductionOrderView(String id, String orderNo, String productId, String productName,
                                      int quantity, String machine, String ownerInitials, OrderStage stage,
                                      LocalDate dueDate) {
        public static ProductionOrderView of(ProductionOrder o) {
            return new ProductionOrderView(o.getId(), o.getOrderNo(), o.getProductId(), o.getProductName(),
                    o.getQuantity(), o.getMachine(), o.getOwnerInitials(), o.getStage(), o.getDueDate());
        }
    }

    public record ShiftRowView(ShiftId shift, String label, String time, List<ShiftCell> cells) {
        public static ShiftRowView of(ShiftPlanRow r) {
            return new ShiftRowView(r.getShift(), r.getLabel(), r.getTimeRange(), r.getCells());
        }
    }

    public record ShiftPlanView(String week, List<ShiftRowView> rows) {
    }

    public record MachineView(String id, String name, MachineStatus status, int oee, int availability,
                              int performance, int quality, int progress, String currentOrder) {
        public static MachineView of(Machine m) {
            return new MachineView(m.getId(), m.getName(), m.getStatus(), m.getOee(), m.getAvailability(),
                    m.getPerformance(), m.getQuality(), m.getProgress(), m.getCurrentOrder());
        }
    }
}
