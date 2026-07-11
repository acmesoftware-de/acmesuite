package de.acmesoftware.acmesuite.base.domain;

import de.acmesoftware.acmesuite.base.AccessRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseUserRepository extends JpaRepository<BaseUser, String> {

    Optional<BaseUser> findByUsername(String username);

    Optional<BaseUser> findByAuthProviderAndExternalSubject(String authProvider, String externalSubject);

    boolean existsByRole(AccessRole role);

    List<BaseUser> findAllByOrderByCreatedAtAsc();
}
