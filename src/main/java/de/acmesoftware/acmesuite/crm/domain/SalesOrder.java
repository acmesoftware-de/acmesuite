package de.acmesoftware.acmesuite.crm.domain;

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
import de.acmesoftware.acmesuite.shared.AuditedEntity;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Order (ACMEcrm). Passes through the eFreigabe; confirmed orders drive factory/port/treasury. */
@Entity
@Table(name = "sales_order")
@Audited
@SQLRestriction("deleted_at is null")
public class SalesOrder extends AuditedEntity {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "customer_id", length = 48, nullable = false)
    private String customerId;

    @Column(name = "quote_id", length = 48)
    private String quoteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency", length = 3))
    })
    private Money total;

    @Column(name = "note", length = 512)
    private String note;

    /** Shipping mode: SHIP (slow, cheap) or AIR (fast, more expensive + air-freight restriction). */
    @Column(name = "shipping_mode", length = 8, nullable = false)
    private String shippingMode = "SHIP";

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

    @NotAudited
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> lines = new ArrayList<>();

    protected SalesOrder() {
    }

    public SalesOrder(String id, String customerId, String quoteId, LocalDate orderDate, Money total,
                      String note, List<OrderLine> lines) {
        this.id = id;
        this.customerId = customerId;
        this.quoteId = quoteId;
        this.orderDate = orderDate;
        this.total = total;
        this.note = note;
        this.lines = new ArrayList<>(lines);
    }

    public void submit(boolean required) {
        this.approvalRequired = required;
        this.status = required ? OrderStatus.PENDING_APPROVAL : OrderStatus.APPROVED;
    }

    public void decide(String approverId, boolean approve, LocalDate on, String comment) {
        this.approverId = approverId;
        this.approvalDecision = approve ? "APPROVE" : "REJECT";
        this.approvalDecidedOn = on;
        this.approvalComment = comment;
        this.status = approve ? OrderStatus.APPROVED : OrderStatus.REJECTED;
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal totalAmount() {
        return total == null ? BigDecimal.ZERO : total.amount();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public Money getTotal() {
        return total;
    }

    public String getNote() {
        return note;
    }

    public String getShippingMode() {
        return shippingMode;
    }

    public void setShippingMode(String shippingMode) {
        this.shippingMode = "AIR".equals(shippingMode) ? "AIR" : "SHIP";
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

    public List<OrderLine> getLines() {
        return lines;
    }
}
