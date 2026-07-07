package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, String> {
    List<Quote> findByCustomerId(String customerId);
}
