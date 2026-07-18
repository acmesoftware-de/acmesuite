package de.acmesoftware.acmesuite.build;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.build.ShopfloorService.ShiftInput;
import de.acmesoftware.acmesuite.build.ShopfloorViews.MachineView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ProductionOrderView;
import de.acmesoftware.acmesuite.build.ShopfloorViews.ShiftRowView;
import de.acmesoftware.acmesuite.build.domain.MachineStatus;
import de.acmesoftware.acmesuite.build.domain.OrderStage;
import de.acmesoftware.acmesuite.build.domain.ShiftCell;
import de.acmesoftware.acmesuite.build.domain.ShiftId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** ACMEbuild shop floor: production board, weekly shift plan and machine monitor (contract v0.2.0). */
@SpringBootTest
@Import(TestcontainersConfig.class)
class ShopfloorServiceTest {

    @Autowired
    ShopfloorService shopfloor;

    @Test
    void seedsProductionBoardAndFiltersByStage() {
        assertThat(shopfloor.listOrders(null)).extracting(ProductionOrderView::orderNo)
                .contains("FA-1042", "FA-1045", "FA-1049");
        assertThat(shopfloor.listOrders(OrderStage.FERTIG)).extracting(ProductionOrderView::orderNo)
                .containsExactlyInAnyOrder("FA-1040", "FA-1041");
    }

    @Test
    void createsAndAdvancesAnOrder() {
        ProductionOrderView created = shopfloor.createOrder("p1", 120, "CNC-01", "mw", null, null);
        assertThat(created.stage()).isEqualTo(OrderStage.GEPLANT);
        assertThat(created.orderNo()).startsWith("FA-");
        assertThat(created.ownerInitials()).isEqualTo("MW");

        var moved = shopfloor.updateOrder(created.id(), OrderStage.IN_ARBEIT, null, null);
        assertThat(moved).get().extracting(ProductionOrderView::stage).isEqualTo(OrderStage.IN_ARBEIT);
        assertThat(shopfloor.updateOrder("missing", OrderStage.FERTIG, null, null)).isEmpty();
    }

    @Test
    void seedsShiftPlanAndPersistsAnEdit() {
        var plan = shopfloor.getShiftPlan();
        assertThat(plan.rows()).extracting(ShiftRowView::shift)
                .containsExactly(ShiftId.EARLY, ShiftId.LATE, ShiftId.NIGHT);
        assertThat(plan.rows().get(0).cells()).hasSize(6);

        var full = List.of(ShiftCell.FULL, ShiftCell.FULL, ShiftCell.FULL, ShiftCell.FULL, ShiftCell.FULL,
                ShiftCell.FULL);
        var saved = shopfloor.putShiftPlan(List.of(new ShiftInput(ShiftId.NIGHT, full)));
        assertThat(saved.rows()).filteredOn(r -> r.shift() == ShiftId.NIGHT)
                .singleElement().extracting(ShiftRowView::cells).isEqualTo(full);
    }

    @Test
    void seedsMachineMonitor() {
        assertThat(shopfloor.listMachines()).extracting(MachineView::name)
                .contains("CNC-01", "Schweiß-01", "Fräs-01");
        assertThat(shopfloor.listMachines()).filteredOn(m -> m.name().equals("CNC-01"))
                .singleElement().satisfies(m -> {
                    assertThat(m.status()).isEqualTo(MachineStatus.RUNNING);
                    assertThat(m.oee()).isEqualTo(91);
                });
    }
}
