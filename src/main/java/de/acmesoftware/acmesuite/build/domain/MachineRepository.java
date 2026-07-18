package de.acmesoftware.acmesuite.build.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, String> {

    List<Machine> findAllByOrderByName();
}
