package de.acmesoftware.acmesuite.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** A line item (quote/order): product, quantity, unit price, discount. */
@Embeddable
public class OrderLine {

    @Column(name = "product_id", length = 48, nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent")
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /** Quantity already produced/delivered (partial delivery over several days possible). */
    @Column(name = "fulfilled_quantity", nullable = false)
    private int fulfilledQuantity = 0;

    protected OrderLine() {
    }

    public OrderLine(String productId, int quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
    }

    /** Quantity of this line still outstanding. */
    public int remaining() {
        return quantity - fulfilledQuantity;
    }

    /** Produces {@code q} units of this line (capped at the outstanding quantity). */
    public void produce(int q) {
        fulfilledQuantity += Math.max(0, Math.min(q, remaining()));
    }

    public int getFulfilledQuantity() {
        return fulfilledQuantity;
    }

    /** Line total = quantity × unit price × (1 − discount%/100). */
    public BigDecimal lineTotal() {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.movePointLeft(2));
        return gross.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
}
