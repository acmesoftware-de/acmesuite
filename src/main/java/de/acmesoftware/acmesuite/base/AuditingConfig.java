package de.acmesoftware.acmesuite.base;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables JPA auditing and supplies the current actor for {@code @CreatedBy}/{@code @LastModifiedBy}. */
@Configuration
@EnableJpaAuditing
class AuditingConfig {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(CurrentActor.current());
    }
}
