package de.acmesoftware.acmesuite.supply.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplyContractRepository extends JpaRepository<SupplyContract, String> {
    List<SupplyContract> findBySupplierId(String supplierId);

    List<SupplyContract> findByMaterialId(String materialId);

    Optional<SupplyContract> findFirstBySupplierIdAndMaterialId(String supplierId, String materialId);
}
