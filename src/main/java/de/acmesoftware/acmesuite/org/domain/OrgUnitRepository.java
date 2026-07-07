package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, String> {

    List<OrgUnit> findByLegalEntity_Id(String legalEntityId);

    List<OrgUnit> findByParent_Id(String parentId);
}
