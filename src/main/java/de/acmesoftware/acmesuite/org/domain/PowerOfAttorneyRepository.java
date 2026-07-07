package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PowerOfAttorneyRepository extends JpaRepository<PowerOfAttorney, String> {

    List<PowerOfAttorney> findByHolder_Id(String personId);

    List<PowerOfAttorney> findByLegalEntity_Id(String legalEntityId);
}
