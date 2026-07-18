package de.acmesoftware.acmesuite.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailThreadRepository extends JpaRepository<MailThread, String> {
    List<MailThread> findByCustomerId(String customerId);

    List<MailThread> findByContactId(String contactId);
}
