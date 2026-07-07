package de.acmesoftware.acmesuite.org.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Power of attorney: who may sign for which company up to what amount — and solely or
 * only jointly? A central quantity for the later contract/approval logic: whether a person
 * may approve a contract depends on the type of power of attorney, the limit, and the signature rule.
 */
@Entity
@Table(name = "power_of_attorney")
public class PowerOfAttorney {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person holder;

    /** Company for which the power of attorney applies. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PowerOfAttorneyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_rule", nullable = false, length = 16)
    private SignatureRule signatureRule;

    /** Amount limit; {@link Money#isUnlimited()} = no limit. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "limit_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "limit_currency", length = 3))
    })
    private Money limit;

    @Column(name = "scope", length = 512)
    private String scope;

    @Embedded
    private DateRange validity;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    protected PowerOfAttorney() {
    }

    public PowerOfAttorney(String id, Person holder, LegalEntity legalEntity, PowerOfAttorneyType type,
                           SignatureRule signatureRule, Money limit, String scope, DateRange validity) {
        this.id = id;
        this.holder = holder;
        this.legalEntity = legalEntity;
        this.type = type;
        this.signatureRule = signatureRule;
        this.limit = limit;
        this.scope = scope;
        this.validity = validity;
        this.revoked = false;
    }

    /**
     * May this power of attorney cover an amount of the given size on the cutoff date?
     * (The SOLE/JOINT signature rule is not checked here — that is the responsibility of the approval logic.)
     */
    public boolean covers(Money amount, LocalDate on) {
        if (revoked || !validity.isActiveOn(on)) {
            return false;
        }
        if (limit == null || limit.isUnlimited()) {
            return true;
        }
        if (amount == null || amount.isUnlimited()) {
            return false;
        }
        boolean sameCurrency = limit.currency().equals(amount.currency());
        return sameCurrency && limit.amount().compareTo(amount.amount()) >= 0;
    }

    public void revoke() {
        this.revoked = true;
    }

    public String getId() {
        return id;
    }

    public Person getHolder() {
        return holder;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public PowerOfAttorneyType getType() {
        return type;
    }

    public SignatureRule getSignatureRule() {
        return signatureRule;
    }

    public Money getLimit() {
        return limit;
    }

    public String getScope() {
        return scope;
    }

    public DateRange getValidity() {
        return validity;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
