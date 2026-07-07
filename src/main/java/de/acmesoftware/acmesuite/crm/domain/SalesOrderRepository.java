package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, String> {
    List<SalesOrder> findByCustomerId(String customerId);

    List<SalesOrder> findByStatus(OrderStatus status);
}
