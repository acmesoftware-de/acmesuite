package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceListRepository extends JpaRepository<PriceList, String> {
    List<PriceList> findByKind(PriceListKind kind);
}
