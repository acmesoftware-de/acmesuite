package de.acmesoftware.acmesuite.supply.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;

/**
 * Stock level per raw material (ACMEsupply warehouse). Production draws it down; goods receipt (received
 * procurement) replenishes it. Energy materials are NOT stocked (they flow), only raw materials.
 */
@Entity
@Table(name = "material_stock")
@Audited
@SQLRestriction("deleted_at is null")
public class MaterialStock extends AuditedEntity {

    @Id
    @Column(name = "material_id", length = 48)
    private String materialId;

    @Column(name = "quantity", nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Reorder level: below it, procurement demand is signaled (purchasing plans manually). */
    @Column(name = "reorder_level", nullable = false, precision = 16, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    protected MaterialStock() {
    }

    public MaterialStock(String materialId, BigDecimal quantity, BigDecimal reorderLevel) {
        this.materialId = materialId;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    /** Withdraws up to {@code want}; returns the quantity actually withdrawn (never more than stock). */
    public BigDecimal consume(BigDecimal want) {
        BigDecimal taken = want.min(quantity).max(BigDecimal.ZERO);
        quantity = quantity.subtract(taken);
        return taken;
    }

    public void receive(BigDecimal amount) {
        quantity = quantity.add(amount.max(BigDecimal.ZERO));
    }

    /** Set the stock directly (e.g. reset to the initial state). */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public boolean belowReorder() {
        return quantity.compareTo(reorderLevel) < 0;
    }

    public String getMaterialId() {
        return materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }
}
