package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbsenceRepository extends JpaRepository<Absence, String> {

    List<Absence> findByPerson_Id(String personId);
}
