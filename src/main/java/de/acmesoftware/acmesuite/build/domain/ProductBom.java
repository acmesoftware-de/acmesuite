package de.acmesoftware.acmesuite.build.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bill of materials of a product (ACMEprod): which raw materials + how many labor and
 * energy units (kWh) are needed per unit. Key = CRM product ID; links sales ↔ procurement
 * ↔ factory and is the basis for the daily production cycle.
 */
@Entity
@Table(name = "product_bom")
public class ProductBom {

    @Id
    @Column(name = "product_id", length = 48)
    private String productId;

    @Column(name = "labor_units", nullable = false)
    private BigDecimal laborUnits;

    @Column(name = "energy_units", nullable = false)
    private BigDecimal energyUnits;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_bom_line", joinColumns = @JoinColumn(name = "product_id"))
    private List<BomLine> lines = new ArrayList<>();

    protected ProductBom() {
    }

    public ProductBom(String productId, BigDecimal laborUnits, BigDecimal energyUnits, List<BomLine> lines) {
        this.productId = productId;
        this.laborUnits = laborUnits;
        this.energyUnits = energyUnits;
        this.lines = new ArrayList<>(lines == null ? List.of() : lines);
    }

    public void update(BigDecimal laborUnits, BigDecimal energyUnits, List<BomLine> lines) {
        if (laborUnits != null) {
            this.laborUnits = laborUnits;
        }
        if (energyUnits != null) {
            this.energyUnits = energyUnits;
        }
        if (lines != null) {
            this.lines = new ArrayList<>(lines);
        }
    }

    public String getProductId() {
        return productId;
    }

    public BigDecimal getLaborUnits() {
        return laborUnits;
    }

    public BigDecimal getEnergyUnits() {
        return energyUnits;
    }

    public List<BomLine> getLines() {
        return lines;
    }
}
