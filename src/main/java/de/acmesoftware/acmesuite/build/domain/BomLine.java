package de.acmesoftware.acmesuite.build.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** A bill-of-materials line: raw material (ACMEsupply material) × quantity per product unit. */
@Embeddable
public class BomLine {

    @Column(name = "material_id", length = 48, nullable = false)
    private String materialId;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    protected BomLine() {
    }

    public BomLine(String materialId, BigDecimal quantity) {
        this.materialId = materialId;
        this.quantity = quantity;
    }

    public String getMaterialId() {
        return materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
