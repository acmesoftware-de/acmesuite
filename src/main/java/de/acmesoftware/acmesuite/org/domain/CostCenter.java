package de.acmesoftware.acmesuite.org.domain;

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
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/**
 * Cost center. Attached to an organizational unit and has an annual budget; the
 * responsible person usually signs off on expenses charged to this cost center.
 */
@Entity
@Table(name = "cost_center")
@Audited
@SQLRestriction("deleted_at is null")
public class CostCenter extends AuditedEntity {

    /** Cost center code, e.g. "CC-2000". */
    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_unit_id", nullable = false)
    private OrgUnit orgUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_person_id")
    private Person responsible;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "budget_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "budget_currency", length = 3))
    })
    private Money annualBudget;

    protected CostCenter() {
    }

    public CostCenter(String id, String name, OrgUnit orgUnit, Person responsible, Money annualBudget) {
        this.id = id;
        this.name = name;
        this.orgUnit = orgUnit;
        this.responsible = responsible;
        this.annualBudget = annualBudget;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    public Person getResponsible() {
        return responsible;
    }

    public Money getAnnualBudget() {
        return annualBudget;
    }
}
