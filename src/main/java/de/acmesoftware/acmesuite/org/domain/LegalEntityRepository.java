package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalEntityRepository extends JpaRepository<LegalEntity, String> {

    List<LegalEntity> findByParentIsNull();

    List<LegalEntity> findByParent_Id(String parentId);
}
