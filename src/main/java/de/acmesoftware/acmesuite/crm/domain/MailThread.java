package de.acmesoftware.acmesuite.crm.domain;

import de.acmesoftware.acmesuite.shared.AuditedEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/** A correspondence thread attached to a customer/contact (see api/acme-crm.yaml, Mail). */
@Entity
@Table(name = "mail_thread")
@Audited
@SQLRestriction("deleted_at is null")
public class MailThread extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "customer_id", length = 48)
    private String customerId;

    @Column(name = "contact_id", length = 48)
    private String contactId;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mail_thread_participant", joinColumns = @JoinColumn(name = "thread_id"))
    @Column(name = "participant", length = 160)
    private List<String> participants = new ArrayList<>();

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mail_message", joinColumns = @JoinColumn(name = "thread_id"))
    @OrderColumn(name = "position")
    private List<MailMessage> messages = new ArrayList<>();

    protected MailThread() {
    }

    public MailThread(String id, String customerId, String contactId, String subject, Instant lastMessageAt,
                      List<String> participants, List<MailMessage> messages) {
        this.id = id;
        this.customerId = customerId;
        this.contactId = contactId;
        this.subject = subject;
        this.lastMessageAt = lastMessageAt;
        this.participants = new ArrayList<>(participants == null ? List.of() : participants);
        this.messages = new ArrayList<>(messages == null ? List.of() : messages);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getContactId() {
        return contactId;
    }

    public String getSubject() {
        return subject;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public List<MailMessage> getMessages() {
        return messages;
    }
}
