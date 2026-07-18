package de.acmesoftware.acmesuite.build.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftPlanRowRepository extends JpaRepository<ShiftPlanRow, ShiftId> {

    List<ShiftPlanRow> findAllByOrderByOrd();
}
