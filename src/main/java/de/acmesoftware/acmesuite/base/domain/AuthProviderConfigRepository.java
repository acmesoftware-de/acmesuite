package de.acmesoftware.acmesuite.base.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProviderConfigRepository extends JpaRepository<AuthProviderConfig, String> {

    Optional<AuthProviderConfig> findByProviderId(String providerId);
}
