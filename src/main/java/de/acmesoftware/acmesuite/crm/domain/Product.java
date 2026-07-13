package de.acmesoftware.acmesuite.crm.domain;

import de.acmesoftware.acmesuite.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/** Product in the ACME catalog (analog goods made by the factory). */
@Entity
@Table(name = "product")
@Audited
@SQLRestriction("deleted_at is null")
public class Product extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "unit", length = 24)
    private String unit = "pcs";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "list_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "list_currency", length = 3))
    })
    private Money listPrice;

    protected Product() {
    }

    public Product(String id, String sku, String name, String category, String unit, boolean active,
                   Money listPrice) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unit = unit == null ? "pcs" : unit;
        this.active = active;
        this.listPrice = listPrice;
    }

    public void update(String sku, String name, String category, String unit, Boolean active, Money listPrice) {
        if (sku != null) {
            this.sku = sku;
        }
        if (name != null) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (unit != null) {
            this.unit = unit;
        }
        if (active != null) {
            this.active = active;
        }
        if (listPrice != null) {
            this.listPrice = listPrice;
        }
    }

    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isActive() {
        return active;
    }

    public Money getListPrice() {
        return listPrice;
    }
}
