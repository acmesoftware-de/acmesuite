package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.domain.Machine;
import de.acmesoftware.acmesuite.build.domain.MachineRepository;
import de.acmesoftware.acmesuite.build.domain.MachineStatus;
import de.acmesoftware.acmesuite.build.domain.OrderStage;
import de.acmesoftware.acmesuite.build.domain.ProductionOrder;
import de.acmesoftware.acmesuite.build.domain.ProductionOrderRepository;
import de.acmesoftware.acmesuite.build.domain.ShiftCell;
import de.acmesoftware.acmesuite.build.domain.ShiftId;
import de.acmesoftware.acmesuite.build.domain.ShiftPlanRow;
import de.acmesoftware.acmesuite.build.domain.ShiftPlanRowRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the ACMEbuild shop floor (contract v0.2.0): the production planning board, the default
 * weekly shift plan and the machine monitor — the representative data behind the "Fertigung"
 * views. Each collection is seeded independently and only when empty.
 */
@Component
@Order(3)
public class ShopfloorSeeder implements ApplicationRunner {

    private final ProductionOrderRepository orders;
    private final ShiftPlanRowRepository shifts;
    private final MachineRepository machines;

    public ShopfloorSeeder(ProductionOrderRepository orders, ShiftPlanRowRepository shifts,
                           MachineRepository machines) {
        this.orders = orders;
        this.shifts = shifts;
        this.machines = machines;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedOrders();
        seedShiftPlan();
        seedMachines();
    }

    private void seedOrders() {
        if (orders.count() > 0) {
            return;
        }
        order("fa-1042", "FA-1042", "Gehäuse A2", 250, "CNC-03", "MW", OrderStage.IN_ARBEIT);
        order("fa-1043", "FA-1043", "Welle 12 mm", 1000, "Dreh-01", "JS", OrderStage.GEPLANT);
        order("fa-1044", "FA-1044", "Deckel B", 500, "Press-02", "AL", OrderStage.RUESTEN);
        order("fa-1045", "FA-1045", "Rahmen XL", 80, "Schweiß-01", "MW", OrderStage.IN_ARBEIT);
        order("fa-1046", "FA-1046", "Halter C3", 320, "CNC-01", "JS", OrderStage.PRUEFUNG);
        order("fa-1047", "FA-1047", "Blende A", 640, "Laser-01", "AL", OrderStage.GEPLANT);
        order("fa-1041", "FA-1041", "Gehäuse A1", 250, "CNC-03", "MW", OrderStage.FERTIG);
        order("fa-1048", "FA-1048", "Adapter M8", 2000, "Dreh-02", "JS", OrderStage.GEPLANT);
        order("fa-1040", "FA-1040", "Grundplatte", 150, "Fräs-01", "AL", OrderStage.FERTIG);
        order("fa-1049", "FA-1049", "Klemme K2", 900, "Press-02", "MW", OrderStage.IN_ARBEIT);
    }

    private void seedShiftPlan() {
        if (shifts.count() > 0) {
            return;
        }
        ShiftCell o = ShiftCell.FULL;
        ShiftCell t = ShiftCell.PARTIAL;
        ShiftCell f = ShiftCell.FREE;
        for (ShopfloorService.ShiftMeta m : ShopfloorService.SHIFTS) {
            List<ShiftCell> cells = switch (m.id()) {
                case EARLY -> List.of(o, o, o, o, o, f);
                case LATE -> List.of(o, o, o, o, t, f);
                case NIGHT -> List.of(f, f, o, o, f, f);
            };
            shifts.save(new ShiftPlanRow(m.id(), m.ord(), m.label(), m.time(), cells));
        }
    }

    private void seedMachines() {
        if (machines.count() > 0) {
            return;
        }
        machine("m-cnc-01", "CNC-01", MachineStatus.RUNNING, 91, 96, 95, 99, 64, "FA-1046 · Halter C3");
        machine("m-cnc-03", "CNC-03", MachineStatus.RUNNING, 87, 93, 92, 98, 38, "FA-1042 · Gehäuse A2");
        machine("m-dreh-01", "Dreh-01", MachineStatus.SETUP, 78, 82, 96, 99, 0, "FA-1043 · Welle 12 mm");
        machine("m-dreh-02", "Dreh-02", MachineStatus.IDLE, 0, 0, 0, 0, 0, "— kein Auftrag");
        machine("m-press-02", "Press-02", MachineStatus.RUNNING, 84, 90, 94, 99, 72, "FA-1049 · Klemme K2");
        machine("m-laser-01", "Laser-01", MachineStatus.RUNNING, 88, 94, 93, 100, 21, "FA-1047 · Blende A");
        machine("m-schweiss-01", "Schweiß-01", MachineStatus.FAULT, 62, 68, 92, 98, 45, "FA-1045 · Rahmen XL");
        machine("m-fraes-01", "Fräs-01", MachineStatus.MAINTENANCE, 0, 0, 0, 0, 0, "Wartung bis 14:00");
    }

    private void order(String id, String orderNo, String productName, int qty, String machine,
                       String owner, OrderStage stage) {
        orders.save(new ProductionOrder(id, orderNo, null, productName, qty, machine, owner, stage, null));
    }

    private void machine(String id, String name, MachineStatus status, int oee, int av, int pe, int qu,
                         int prog, String order) {
        machines.save(new Machine(id, name, status, oee, av, pe, qu, prog, order));
    }
}
