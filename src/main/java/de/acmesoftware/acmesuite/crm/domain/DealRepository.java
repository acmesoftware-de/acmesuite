package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DealRepository extends JpaRepository<Deal, String> {
    List<Deal> findByCustomerId(String customerId);

    List<Deal> findByContactId(String contactId);

    List<Deal> findByStage(PipelineStage stage);
}
