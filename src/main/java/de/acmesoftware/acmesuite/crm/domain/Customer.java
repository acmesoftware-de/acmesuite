package de.acmesoftware.acmesuite.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/** Customer or reseller of ACME analog Inc. (ACMEcrm SoR). */
@Entity
@Table(name = "customer")
@Audited
@SQLRestriction("deleted_at is null")
public class Customer extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private CustomerKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "country", length = 3)
    private String country;

    /** For end customers of a reseller: that reseller's ID. */
    @Column(name = "parent_reseller_id", length = 48)
    private String parentResellerId;

    /** Assigned price list (otherwise resolved by customer kind). */
    @Column(name = "price_list_id", length = 48)
    private String priceListId;

    protected Customer() {
    }

    public Customer(String id, String name, CustomerKind kind, CustomerStatus status, String email,
                    String country, String parentResellerId, String priceListId) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.status = status == null ? CustomerStatus.PROSPECT : status;
        this.email = email;
        this.country = country;
        this.parentResellerId = parentResellerId;
        this.priceListId = priceListId;
    }

    public void update(String name, CustomerKind kind, CustomerStatus status, String email, String country,
                       String parentResellerId, String priceListId) {
        if (name != null) {
            this.name = name;
        }
        if (kind != null) {
            this.kind = kind;
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
        if (parentResellerId != null) {
            this.parentResellerId = parentResellerId;
        }
        if (priceListId != null) {
            this.priceListId = priceListId;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CustomerKind getKind() {
        return kind;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }

    public String getParentResellerId() {
        return parentResellerId;
    }

    public String getPriceListId() {
        return priceListId;
    }
}
