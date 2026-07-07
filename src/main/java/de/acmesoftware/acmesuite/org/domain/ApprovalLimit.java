package de.acmesoftware.acmesuite.org.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Explicitly maintained approval limit of a person (ACMEhr). Overrides the limit
 * derived from powers of attorney; basis for eFreigabe.
 */
@Entity
@Table(name = "approval_limit")
public class ApprovalLimit {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Optionally scoped to a single legal entity (null = global). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_entity_id")
    private LegalEntity legalEntity;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "max_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "max_currency", length = 3))
    })
    private Money maxAmount;

    @Embedded
    private DateRange validity;

    protected ApprovalLimit() {
    }

    public ApprovalLimit(String id, Person person, LegalEntity legalEntity, Money maxAmount, DateRange validity) {
        this.id = id;
        this.person = person;
        this.legalEntity = legalEntity;
        this.maxAmount = maxAmount;
        this.validity = validity;
    }

    public void update(Money maxAmount, DateRange validity) {
        this.maxAmount = maxAmount;
        this.validity = validity;
    }

    public String getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public Money getMaxAmount() {
        return maxAmount;
    }

    public DateRange getValidity() {
        return validity;
    }
}
