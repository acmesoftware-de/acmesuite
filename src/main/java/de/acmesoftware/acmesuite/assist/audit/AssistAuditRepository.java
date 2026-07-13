package de.acmesoftware.acmesuite.assist.audit;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistAuditRepository extends JpaRepository<AssistAudit, Long> {

    /** The most recent row — the head of the hash chain. */
    Optional<AssistAudit> findTopByOrderByIdDesc();
}
