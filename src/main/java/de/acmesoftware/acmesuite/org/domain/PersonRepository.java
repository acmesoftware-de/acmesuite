package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, String> {

    List<Person> findByPrimaryOrgUnit_Id(String orgUnitId);
}
