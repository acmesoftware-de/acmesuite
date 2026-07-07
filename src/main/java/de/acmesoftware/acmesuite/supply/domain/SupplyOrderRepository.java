package de.acmesoftware.acmesuite.supply.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, String> {
    List<SupplyOrder> findBySupplierId(String supplierId);

    List<SupplyOrder> findByStatus(SupplyOrderStatus status);
}
