package de.acmesoftware.acmesuite.org.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Assignment of a {@link Role} to a {@link Person}, optionally within a specific
 * {@link OrgUnit} and with a business validity period ({@link DateRange}).
 */
@Entity
@Table(name = "role_assignment")
public class RoleAssignment {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_unit_id")
    private OrgUnit orgUnit;

    @Embedded
    private DateRange validity;

    protected RoleAssignment() {
    }

    public RoleAssignment(String id, Person person, Role role, OrgUnit orgUnit, DateRange validity) {
        this.id = id;
        this.person = person;
        this.role = role;
        this.orgUnit = orgUnit;
        this.validity = validity;
    }

    public String getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public Role getRole() {
        return role;
    }

    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    public DateRange getValidity() {
        return validity;
    }
}
