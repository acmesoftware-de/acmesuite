package de.acmesoftware.acmesuite.crm.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
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
import java.util.ArrayList;
import java.util.List;

/** Price list (list/reseller/customer-specific) with tiered line items. */
@Entity
@Table(name = "price_list")
public class PriceList {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private PriceListKind kind;

    @Embedded
    private DateRange validity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "price_list_item", joinColumns = @JoinColumn(name = "price_list_id"))
    private List<PriceListItem> items = new ArrayList<>();

    protected PriceList() {
    }

    public PriceList(String id, String name, String currency, PriceListKind kind, DateRange validity,
                     List<PriceListItem> items) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.kind = kind;
        this.validity = validity;
        this.items = new ArrayList<>(items == null ? List.of() : items);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public PriceListKind getKind() {
        return kind;
    }

    public DateRange getValidity() {
        return validity;
    }

    public List<PriceListItem> getItems() {
        return items;
    }
}
