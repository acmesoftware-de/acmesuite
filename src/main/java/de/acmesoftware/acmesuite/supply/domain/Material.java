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

/** Raw material or energy in the procurement catalog. */
@Entity
@Table(name = "material")
@Audited
@SQLRestriction("deleted_at is null")
public class Material extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "code", nullable = false, length = 48)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private MaterialKind kind;

    @Column(name = "unit", length = 16)
    private String unit;

    protected Material() {
    }

    public Material(String id, String code, String name, MaterialKind kind, String unit) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.kind = kind;
        this.unit = unit;
    }

    public void update(String code, String name, MaterialKind kind, String unit) {
        if (code != null) {
            this.code = code;
        }
        if (name != null) {
            this.name = name;
        }
        if (kind != null) {
            this.kind = kind;
        }
        if (unit != null) {
            this.unit = unit;
        }
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public MaterialKind getKind() {
        return kind;
    }

    public String getUnit() {
        return unit;
    }
}
