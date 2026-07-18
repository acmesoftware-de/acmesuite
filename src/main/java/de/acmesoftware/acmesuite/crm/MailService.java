package de.acmesoftware.acmesuite.crm;

import de.acmesoftware.acmesuite.crm.CrmViews.MailThreadView;
import de.acmesoftware.acmesuite.crm.domain.MailThread;
import de.acmesoftware.acmesuite.crm.domain.MailThreadRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ACMEcrm mail-thread overlay (see api/acme-crm.yaml, Mail). Read-only correspondence view. */
@Service
@Transactional(readOnly = true)
public class MailService {

    private final MailThreadRepository threads;

    public MailService(MailThreadRepository threads) {
        this.threads = threads;
    }

    public List<MailThreadView> listThreads(String customerId, String contactId) {
        List<MailThread> base = customerId != null ? threads.findByCustomerId(customerId)
                : contactId != null ? threads.findByContactId(contactId)
                : threads.findAll();
        return base.stream()
                .sorted(Comparator.comparing(MailService::lastAt).reversed())
                .map(MailThreadView::of)
                .toList();
    }

    public Optional<MailThreadView> getThread(String id) {
        return threads.findById(id).map(MailThreadView::of);
    }

    private static Instant lastAt(MailThread t) {
        return t.getLastMessageAt() == null ? Instant.EPOCH : t.getLastMessageAt();
    }
}
