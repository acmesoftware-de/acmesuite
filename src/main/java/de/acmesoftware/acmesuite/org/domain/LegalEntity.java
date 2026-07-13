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
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/**
 * Legal entity of the group (e.g. the holding or an operating GmbH/AG).
 *
 * <p>The group forms a tree via {@link #parent} (holding → subsidiaries). Each legal entity
 * can be mapped to a tenant on an external contract-management platform ({@link #platformTenantKey}) — this is how the
 * entity later becomes a real tenant on the external platform.
 */
@Entity
@Table(name = "legal_entity")
@Audited
@SQLRestriction("deleted_at is null")
public class LegalEntity extends AuditedEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private LegalEntityType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LegalEntity parent;

    @Column(name = "country", length = 2)
    private String country;

    /** Commercial register number (e.g. "HRB 12345"). */
    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    /** Key of the external platform tenant, if this legal entity is set up on the external platform. */
    @Column(name = "platform_tenant_key", length = 64)
    private String platformTenantKey;

    protected LegalEntity() {
    }

    public LegalEntity(String id, String legalName, LegalEntityType type, LegalEntity parent,
                       String country, String registrationNumber) {
        this.id = id;
        this.legalName = legalName;
        this.type = type;
        this.parent = parent;
        this.country = country;
        this.registrationNumber = registrationNumber;
    }

    public String getId() {
        return id;
    }

    public String getLegalName() {
        return legalName;
    }

    public LegalEntityType getType() {
        return type;
    }

    public LegalEntity getParent() {
        return parent;
    }

    public String getCountry() {
        return country;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getPlatformTenantKey() {
        return platformTenantKey;
    }

    public void mapToPlatformTenant(String tenantKey) {
        this.platformTenantKey = tenantKey;
    }
}
