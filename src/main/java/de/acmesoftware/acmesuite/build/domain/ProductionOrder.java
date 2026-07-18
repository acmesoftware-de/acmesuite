package de.acmesoftware.acmesuite.build.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A production order on the ACMEbuild planning board. Operational data — it moves between board
 * stages (drag &amp; drop) and is not Envers-audited. {@code productId} is a free reference (the
 * product master lives in CRM); {@code productName} is denormalised for display.
 */
@Entity
@Table(name = "production_order")
public class ProductionOrder {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "order_no", length = 24, nullable = false)
    private String orderNo;

    @Column(name = "product_id", length = 48)
    private String productId;

    @Column(name = "product_name", length = 120)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "machine", length = 48)
    private String machine;

    @Column(name = "owner_initials", length = 8)
    private String ownerInitials;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 16, nullable = false)
    private OrderStage stage;

    @Column(name = "due_date")
    private LocalDate dueDate;

    protected ProductionOrder() {
    }

    public ProductionOrder(String id, String orderNo, String productId, String productName, int quantity,
                           String machine, String ownerInitials, OrderStage stage, LocalDate dueDate) {
        this.id = id;
        this.orderNo = orderNo;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.machine = machine;
        this.ownerInitials = ownerInitials;
        this.stage = stage;
        this.dueDate = dueDate;
    }

    /** Applies the non-null fields of a partial update (board move / machine reassignment). */
    public void apply(OrderStage stage, String machine, String ownerInitials) {
        if (stage != null) {
            this.stage = stage;
        }
        if (machine != null) {
            this.machine = machine;
        }
        if (ownerInitials != null) {
            this.ownerInitials = ownerInitials;
        }
    }

    public String getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getMachine() {
        return machine;
    }

    public String getOwnerInitials() {
        return ownerInitials;
    }

    public OrderStage getStage() {
        return stage;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}
