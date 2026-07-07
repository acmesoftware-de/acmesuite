package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLimitRepository extends JpaRepository<ApprovalLimit, String> {

    List<ApprovalLimit> findByPerson_Id(String personId);

    Optional<ApprovalLimit> findByPerson_IdAndLegalEntity_Id(String personId, String legalEntityId);

    List<ApprovalLimit> findByPerson_IdAndLegalEntityIsNull(String personId);
}
