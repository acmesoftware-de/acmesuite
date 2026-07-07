package de.acmesoftware.acmesuite.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Organizational unit (department tree) within a legal entity.
 * Hierarchy via {@link #parent} (Division → Department → Team).
 */
@Entity
@Table(name = "org_unit")
public class OrgUnit {

    @Id
    @Column(name = "id", length = 96)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private OrgUnitType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_entity_id", nullable = false)
    private LegalEntity legalEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private OrgUnit parent;

    protected OrgUnit() {
    }

    public OrgUnit(String id, String name, OrgUnitType type, LegalEntity legalEntity, OrgUnit parent) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.legalEntity = legalEntity;
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public OrgUnitType getType() {
        return type;
    }

    public LegalEntity getLegalEntity() {
        return legalEntity;
    }

    public OrgUnit getParent() {
        return parent;
    }
}
