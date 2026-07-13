package de.acmesoftware.acmesuite.supply.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/** Supplier of ACME analog Inc. (ACMEsupply SoR). */
@Entity
@Table(name = "supplier")
@Audited
@SQLRestriction("deleted_at is null")
public class Supplier extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SupplierStatus status = SupplierStatus.PROSPECT;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "country", length = 3)
    private String country;

    protected Supplier() {
    }

    public Supplier(String id, String name, SupplierStatus status, String email, String country) {
        this.id = id;
        this.name = name;
        this.status = status == null ? SupplierStatus.PROSPECT : status;
        this.email = email;
        this.country = country;
    }

    public void update(String name, SupplierStatus status, String email, String country) {
        if (name != null) {
            this.name = name;
        }
        if (status != null) {
            this.status = status;
        }
        if (email != null) {
            this.email = email;
        }
        if (country != null) {
            this.country = country;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }
}
