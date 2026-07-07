package de.acmesoftware.acmesuite.supply;

import de.acmesoftware.acmesuite.shared.DateRange;
import de.acmesoftware.acmesuite.supply.domain.Material;
import de.acmesoftware.acmesuite.supply.domain.MaterialKind;
import de.acmesoftware.acmesuite.supply.domain.MaterialRepository;
import de.acmesoftware.acmesuite.supply.domain.MaterialStock;
import de.acmesoftware.acmesuite.supply.domain.MaterialStockRepository;
import de.acmesoftware.acmesuite.supply.domain.Supplier;
import de.acmesoftware.acmesuite.supply.domain.SupplierRepository;
import de.acmesoftware.acmesuite.supply.domain.SupplierStatus;
import de.acmesoftware.acmesuite.supply.domain.SupplyContract;
import de.acmesoftware.acmesuite.supply.domain.SupplyContractRepository;
import de.acmesoftware.acmesuite.supply.domain.Tier;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Stable ACMEsupply demo dataset: suppliers, raw materials/energy, supply contracts (tiers + lead times). */
@Component
@Order(2)
public class SupplySeeder implements ApplicationRunner {

    private final SupplierRepository suppliers;
    private final MaterialRepository materials;
    private final SupplyContractRepository contracts;
    private final MaterialStockRepository stock;

    public SupplySeeder(SupplierRepository suppliers, MaterialRepository materials,
                        SupplyContractRepository contracts, MaterialStockRepository stock) {
        this.suppliers = suppliers;
        this.materials = materials;
        this.contracts = contracts;
        this.stock = stock;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedStock(); // independently idempotent: V10 may arrive after the initial seed (stock DB)
        if (suppliers.count() > 0) {
            return;
        }
        suppliers.save(new Supplier("sup-rohstoff", "Raw Materials Trading AG", SupplierStatus.ACTIVE,
                "einkauf@rohstoff-ag.de", "DE"));
        suppliers.save(new Supplier("sup-energie", "Energy North GmbH", SupplierStatus.ACTIVE,
                "vertrieb@energie-nord.de", "DE"));
        suppliers.save(new Supplier("sup-bauteile", "Components Express e.K.", SupplierStatus.ACTIVE,
                "service@bauteile-express.at", "AT"));

        materials.save(new Material("mat-stahl", "STAHL", "Sheet Steel", MaterialKind.RAW_MATERIAL, "kg"));
        materials.save(new Material("mat-holz", "HOLZ", "Wood Housing Blank", MaterialKind.RAW_MATERIAL, "pcs"));
        materials.save(new Material("mat-elektronik", "ELEKTRO", "Electronic Components", MaterialKind.RAW_MATERIAL, "pcs"));
        materials.save(new Material("mat-strom", "STROM", "Electricity", MaterialKind.ENERGY, "kWh"));
        materials.save(new Material("mat-kohle", "KOHLE", "Hard Coal", MaterialKind.ENERGY, "t"));
        // Strategic raw material: only four countries supply Despotium (the market model controls the world market).
        materials.save(new Material("mat-despotium", "DESPOT", "Despotium", MaterialKind.RAW_MATERIAL, "unit"));

        // Four country suppliers for Despotium (the market model controls shares/sanctions).
        suppliers.save(new Supplier("sup-donbasia", "Donbasia Minerals", SupplierStatus.ACTIVE, "ore@donbasia.gov", "DON"));
        suppliers.save(new Supplier("sup-coreanum", "Coreanumdelnorte State Mining", SupplierStatus.ACTIVE, "export@coreanum.gov", "COR"));
        suppliers.save(new Supplier("sup-rumcola", "Rumcola Resources", SupplierStatus.ACTIVE, "sales@rumcola.gov", "RUM"));
        suppliers.save(new Supplier("sup-africanum", "Africanumhornum Extraction", SupplierStatus.ACTIVE, "trade@africanum.gov", "AFR"));

        // supplier, material, currency, leadDays, tiers
        contract("sc-stahl", "sup-rohstoff", "mat-stahl", 14, tier(1, "2.50"), tier(1000, "2.10"));
        contract("sc-holz", "sup-rohstoff", "mat-holz", 10, tier(1, "8.00"), tier(500, "6.50"));
        contract("sc-elektronik", "sup-bauteile", "mat-elektronik", 7, tier(1, "1.20"), tier(5000, "0.90"));
        contract("sc-strom", "sup-energie", "mat-strom", 1, tier(1, "0.30"));
        contract("sc-kohle", "sup-energie", "mat-kohle", 21, tier(1, "120.00"), tier(100, "98.00"));
        // Despotium base price per country (the market model adds the scarcity index on top).
        contract("sc-despo-don", "sup-donbasia", "mat-despotium", 30, tier(1, "30.00"));
        contract("sc-despo-cor", "sup-coreanum", "mat-despotium", 25, tier(1, "28.00"));
        contract("sc-despo-rum", "sup-rumcola", "mat-despotium", 28, tier(1, "32.00"));
        contract("sc-despo-afr", "sup-africanum", "mat-despotium", 35, tier(1, "31.00"));
    }

    /**
     * Initial raw-material stock levels (energy is not stocked). Generous, so that operations keep
     * running; when the reorder level is undershot, the system signals procurement demand. Has its own
     * idempotency guard, so that an existing DB is also populated after the V10 migration.
     */
    private void seedStock() {
        if (stock.count() > 0) {
            return;
        }
        stockOf("mat-stahl", "8000", "1500");
        stockOf("mat-holz", "6000", "1000");
        stockOf("mat-elektronik", "12000", "2500");
        stockOf("mat-despotium", "1500", "400");
    }

    private void stockOf(String materialId, String qty, String reorder) {
        stock.save(new MaterialStock(materialId, new BigDecimal(qty), new BigDecimal(reorder)));
    }

    private void contract(String id, String supplier, String material, int leadDays, Tier... tiers) {
        contracts.save(new SupplyContract(id, supplier, material, "EUR", leadDays,
                DateRange.openFrom(null), List.of(tiers)));
    }

    private static Tier tier(int minQty, String price) {
        return new Tier(minQty, new BigDecimal(price));
    }
}
