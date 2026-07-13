package de.acmesoftware.acmesuite.org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/**
 * Business role (e.g. "Head of Procurement", "Legal Counsel", "Managing Director").
 * Assigned to persons via {@link RoleAssignment}.
 */
@Entity
@Table(name = "role")
@Audited
@SQLRestriction("deleted_at is null")
public class Role extends AuditedEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private RoleKind kind;

    @Column(name = "description", length = 512)
    private String description;

    protected Role() {
    }

    public Role(String id, String title, RoleKind kind, String description) {
        this.id = id;
        this.title = title;
        this.kind = kind;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public RoleKind getKind() {
        return kind;
    }

    public String getDescription() {
        return description;
    }
}
