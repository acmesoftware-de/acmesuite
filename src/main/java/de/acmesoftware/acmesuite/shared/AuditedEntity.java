package de.acmesoftware.acmesuite.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base for every persistent entity (ADR-0010: versioned, tombstoned data).
 *
 * <ul>
 *   <li>Audit stamps ({@code created_*}, {@code updated_*}) are filled automatically by JPA
 *       auditing (who/when of the last change is visible to everyone).</li>
 *   <li>Deletion is a <b>tombstone</b> ({@code deleted_*}), never a hard delete; concrete entities
 *       add {@code @SQLRestriction("deleted_at is null")} so default reads exclude tombstones.</li>
 *   <li>{@code @Audited}: Hibernate Envers retains the full version history (visible only to the
 *       AUDIT role).</li>
 * </ul>
 */
@MappedSuperclass
@Audited
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 64)
    private String deletedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Marks this row as a tombstone (ADR-0010) — no hard delete. */
    public void tombstone(String actor, Instant at) {
        this.deletedAt = at;
        this.deletedBy = actor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }
}
