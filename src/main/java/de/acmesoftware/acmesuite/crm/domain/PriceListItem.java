package de.acmesoftware.acmesuite.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** A price line: product → unit price from a minimum quantity (tier). */
@Embeddable
public class PriceListItem {

    @Column(name = "product_id", length = 48, nullable = false)
    private String productId;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity = 1;

    protected PriceListItem() {
    }

    public PriceListItem(String productId, BigDecimal unitPrice, int minQuantity) {
        this.productId = productId;
        this.unitPrice = unitPrice;
        this.minQuantity = Math.max(1, minQuantity);
    }

    public String getProductId() {
        return productId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getMinQuantity() {
        return minQuantity;
    }
}
