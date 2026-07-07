package de.acmesoftware.acmesuite.supply.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Tier line of a supply contract: unit price from a minimum quantity onward. */
@Embeddable
public class Tier {

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity = 1;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected Tier() {
    }

    public Tier(int minQuantity, BigDecimal unitPrice) {
        this.minQuantity = Math.max(1, minQuantity);
        this.unitPrice = unitPrice;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
