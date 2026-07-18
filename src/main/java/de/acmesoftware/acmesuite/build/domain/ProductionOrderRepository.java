package de.acmesoftware.acmesuite.build.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, String> {

    List<ProductionOrder> findByStageOrderByOrderNo(OrderStage stage);

    List<ProductionOrder> findAllByOrderByOrderNo();
}
