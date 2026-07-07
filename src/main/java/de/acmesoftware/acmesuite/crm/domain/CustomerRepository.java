package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByKind(CustomerKind kind);

    List<Customer> findByParentResellerId(String parentResellerId);
}
