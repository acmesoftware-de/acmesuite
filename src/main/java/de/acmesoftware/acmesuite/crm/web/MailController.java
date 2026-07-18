package de.acmesoftware.acmesuite.crm.web;

import de.acmesoftware.acmesuite.crm.CrmViews.MailThreadView;
import de.acmesoftware.acmesuite.crm.MailService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ACMEcrm mail threads (see api/acme-crm.yaml, Mail) — read-only correspondence. */
@RestController
@RequestMapping("/api/crm")
public class MailController {

    private final MailService mail;

    public MailController(MailService mail) {
        this.mail = mail;
    }

    @GetMapping("/threads")
    public List<MailThreadView> threads(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String contactId,
            @RequestParam(required = false) String q) {
        return mail.listThreads(customerId, contactId);
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<MailThreadView> thread(@PathVariable String id) {
        return ResponseEntity.of(mail.getThread(id));
    }
}
