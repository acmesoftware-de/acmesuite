package de.acmesoftware.acmesuite.crm.domain;

import de.acmesoftware.acmesuite.shared.AuditedEntity;
import de.acmesoftware.acmesuite.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/**
 * A pipeline deal — the CRM sales overlay (see api/acme-crm.yaml, Pipeline). Source of truth for
 * the settable stage (board drag-drop / inline edit); may be a bare lead or reference its backing
 * quote/order as it progresses.
 */
@Entity
@Table(name = "deal")
@Audited
@SQLRestriction("deleted_at is null")
public class Deal extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private DealSource source = DealSource.LEAD;

    @Column(name = "customer_id", length = 48)
    private String customerId;

    @Column(name = "contact_id", length = 48)
    private String contactId;

    /** Denormalized company display name (customer name at capture time / for bare leads). */
    @Column(name = "company", nullable = false)
    private String company;

    /** Human contact label, e.g. "Sara Mena · CTO". */
    @Column(name = "contact", length = 200)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 16)
    private PipelineStage stage = PipelineStage.NEU;

    @Column(name = "owner_initials", length = 3)
    private String ownerInitials;

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "value_amount"))
    @AttributeOverride(name = "currency", column = @Column(name = "value_currency", length = 3))
    private Money value;

    @Column(name = "quote_id", length = 48)
    private String quoteId;

    @Column(name = "order_id", length = 48)
    private String orderId;

    @Column(name = "last_activity", length = 200)
    private String lastActivity;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    /** Date the deal entered its current stage — drives ageDays. */
    @Column(name = "stage_since", nullable = false)
    private LocalDate stageSince;

    protected Deal() {
    }

    public Deal(String id, DealSource source, String customerId, String contactId, String company, String contact,
                PipelineStage stage, String ownerInitials, String ownerName, Money value, String quoteId,
                String orderId, String lastActivity, Instant lastActivityAt, LocalDate stageSince) {
        this.id = id;
        this.source = source == null ? DealSource.LEAD : source;
        this.customerId = customerId;
        this.contactId = contactId;
        this.company = company;
        this.contact = contact;
        this.stage = stage == null ? PipelineStage.NEU : stage;
        this.ownerInitials = ownerInitials;
        this.ownerName = ownerName;
        this.value = value;
        this.quoteId = quoteId;
        this.orderId = orderId;
        this.lastActivity = lastActivity;
        this.lastActivityAt = lastActivityAt;
        this.stageSince = stageSince == null ? LocalDate.now() : stageSince;
    }

    /** Partial update of the sales-overlay attributes. Stage change resets {@code stageSince} to today. */
    public void update(PipelineStage stage, String company, String contact, Money value, String ownerInitials,
                       String contactId, LocalDate today) {
        if (stage != null && stage != this.stage) {
            this.stage = stage;
            this.stageSince = today;
        }
        if (company != null) {
            this.company = company;
        }
        if (contact != null) {
            this.contact = contact;
        }
        if (value != null) {
            this.value = value;
        }
        if (ownerInitials != null) {
            this.ownerInitials = ownerInitials;
        }
        if (contactId != null) {
            this.contactId = contactId;
        }
    }

    public String getId() {
        return id;
    }

    public DealSource getSource() {
        return source;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getContactId() {
        return contactId;
    }

    public String getCompany() {
        return company;
    }

    public String getContact() {
        return contact;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public String getOwnerInitials() {
        return ownerInitials;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Money getValue() {
        return value;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public LocalDate getStageSince() {
        return stageSince;
    }
}
