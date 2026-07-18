package de.acmesoftware.acmesuite.crm.domain;

import de.acmesoftware.acmesuite.shared.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/** A person at a customer (company) — ACMEcrm contact (see api/acme-crm.yaml, Contacts). */
@Entity
@Table(name = "contact")
@Audited
@SQLRestriction("deleted_at is null")
public class Contact extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    /** The company this contact belongs to (nullable). */
    @Column(name = "customer_id", length = 48)
    private String customerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role", length = 120)
    private String role;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "phone", length = 48)
    private String phone;

    /** Primary contact of the customer. ("primary" is a SQL keyword → column is_primary.) */
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    /** Newsletter opt-in (set by web-form newsletter actions). */
    @Column(name = "newsletter", nullable = false)
    private boolean newsletter;

    protected Contact() {
    }

    public Contact(String id, String customerId, String name, String role, String email, String phone,
                   boolean primary, boolean newsletter) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.primary = primary;
        this.newsletter = newsletter;
    }

    /** Partial update: null leaves a field unchanged; booleans are always applied. */
    public void update(String customerId, String name, String role, String email, String phone,
                       Boolean primary, Boolean newsletter) {
        if (customerId != null) {
            this.customerId = customerId;
        }
        if (name != null) {
            this.name = name;
        }
        if (role != null) {
            this.role = role;
        }
        if (email != null) {
            this.email = email;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (primary != null) {
            this.primary = primary;
        }
        if (newsletter != null) {
            this.newsletter = newsletter;
        }
    }

    public void setNewsletter(boolean newsletter) {
        this.newsletter = newsletter;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isNewsletter() {
        return newsletter;
    }
}
