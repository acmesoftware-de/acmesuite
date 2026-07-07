package de.acmesoftware.acmesuite.supply.domain;

import de.acmesoftware.acmesuite.shared.DateRange;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Supply contract: supplier × material, tiered prices + lead time. */
@Entity
@Table(name = "supply_contract")
public class SupplyContract {

    @Id
    @Column(name = "id", length = 48)
    private String id;

    @Column(name = "supplier_id", length = 48, nullable = false)
    private String supplierId;

    @Column(name = "material_id", length = 48, nullable = false)
    private String materialId;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Embedded
    private DateRange validity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supply_contract_tier", joinColumns = @JoinColumn(name = "contract_id"))
    private List<Tier> tiers = new ArrayList<>();

    protected SupplyContract() {
    }

    public SupplyContract(String id, String supplierId, String materialId, String currency, int leadTimeDays,
                          DateRange validity, List<Tier> tiers) {
        this.id = id;
        this.supplierId = supplierId;
        this.materialId = materialId;
        this.currency = currency;
        this.leadTimeDays = leadTimeDays;
        this.validity = validity;
        this.tiers = new ArrayList<>(tiers == null ? List.of() : tiers);
    }

    public String getId() {
        return id;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getCurrency() {
        return currency;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public DateRange getValidity() {
        return validity;
    }

    public List<Tier> getTiers() {
        return tiers;
    }
}
