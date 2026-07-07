package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, String> {

    List<CostCenter> findByOrgUnit_Id(String orgUnitId);
}
