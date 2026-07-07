package de.acmesoftware.acmesuite.supply.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Line item of a procurement: material, quantity, unit price. */
@Embeddable
public class SupplyLine {

    @Column(name = "material_id", length = 48, nullable = false)
    private String materialId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected SupplyLine() {
    }

    public SupplyLine(String materialId, int quantity, BigDecimal unitPrice) {
        this.materialId = materialId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getMaterialId() {
        return materialId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
