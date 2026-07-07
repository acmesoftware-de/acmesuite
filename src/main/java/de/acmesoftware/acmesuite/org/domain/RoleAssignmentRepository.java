package de.acmesoftware.acmesuite.org.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, String> {

    List<RoleAssignment> findByPerson_Id(String personId);

    List<RoleAssignment> findByRole_Id(String roleId);
}
