package de.acmesoftware.acmesuite.crm.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

/** Quote (ACMEcrm): draft → sent → accepted; acceptance creates an order. */
@Entity
@Table(name = "quote")
public class Quote {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "customer_id", length = 48, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_on")
    private LocalDate createdOn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quote_line", joinColumns = @JoinColumn(name = "quote_id"))
    private List<OrderLine> lines = new ArrayList<>();

    protected Quote() {
    }

    public Quote(String id, String customerId, String currency, LocalDate validUntil, LocalDate createdOn,
                 List<OrderLine> lines) {
        this.id = id;
        this.customerId = customerId;
        this.currency = currency;
        this.validUntil = validUntil;
        this.createdOn = createdOn;
        this.lines = new ArrayList<>(lines);
    }

    public BigDecimal netTotal() {
        return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void changeStatus(QuoteStatus status) {
        this.status = status;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public LocalDate getCreatedOn() {
        return createdOn;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
