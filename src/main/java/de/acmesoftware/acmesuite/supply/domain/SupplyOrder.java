package de.acmesoftware.acmesuite.supply.domain;

import de.acmesoftware.acmesuite.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Procurement (ACMEsupply): goes through the approval process; goods receipt feeds warehouse/power plants. */
@Entity
@Table(name = "supply_order")
public class SupplyOrder {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "supplier_id", length = 48, nullable = false)
    private String supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplyOrderStatus status = SupplyOrderStatus.CREATED;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", length = 3))
    })
    private Money total;

    @Column(name = "note", length = 512)
    private String note;

    /** Delivery mode: SHIP (slow, cheap) or AIRPLANE (fast, more expensive + air-freight restriction). */
    @Column(name = "delivery_mode", length = 8, nullable = false)
    private String deliveryMode = "SHIP";

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "approver_id", length = 48)
    private String approverId;

    @Column(name = "approval_decision", length = 16)
    private String approvalDecision;

    @Column(name = "approval_decided_on")
    private LocalDate approvalDecidedOn;

    @Column(name = "approval_comment", length = 512)
    private String approvalComment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supply_order_line", joinColumns = @JoinColumn(name = "order_id"))
    private List<SupplyLine> lines = new ArrayList<>();

    protected SupplyOrder() {
    }

    public SupplyOrder(String id, String supplierId, LocalDate orderDate, LocalDate expectedDeliveryDate,
                       Money total, String note, List<SupplyLine> lines) {
        this.id = id;
        this.supplierId = supplierId;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.total = total;
        this.note = note;
        this.lines = new ArrayList<>(lines);
    }

    public void submit(boolean required) {
        this.approvalRequired = required;
        this.status = required ? SupplyOrderStatus.PENDING_APPROVAL : SupplyOrderStatus.APPROVED;
    }

    public void decide(String approverId, boolean approve, LocalDate on, String comment) {
        this.approverId = approverId;
        this.approvalDecision = approve ? "APPROVE" : "REJECT";
        this.approvalDecidedOn = on;
        this.approvalComment = comment;
        this.status = approve ? SupplyOrderStatus.APPROVED : SupplyOrderStatus.REJECTED;
    }

    public void changeStatus(SupplyOrderStatus status) {
        this.status = status;
    }

    public BigDecimal totalAmount() {
        return total == null ? BigDecimal.ZERO : total.amount();
    }

    public String getId() {
        return id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public SupplyOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = "AIRPLANE".equals(deliveryMode) ? "AIRPLANE" : "SHIP";
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public Money getTotal() {
        return total;
    }

    public String getNote() {
        return note;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }

    public String getApproverId() {
        return approverId;
    }

    public String getApprovalDecision() {
        return approvalDecision;
    }

    public LocalDate getApprovalDecidedOn() {
        return approvalDecidedOn;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public List<SupplyLine> getLines() {
        return lines;
    }
}
