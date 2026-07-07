package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.domain.BomLine;
import de.acmesoftware.acmesuite.build.domain.ProductBom;
import de.acmesoftware.acmesuite.build.domain.ProductBomRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Bills of materials for the six analog ACME products (raw materials + labor/energy per unit). */
@Component
@Order(3)
public class BuildSeeder implements ApplicationRunner {

    private final ProductBomRepository boms;

    public BuildSeeder(ProductBomRepository boms) {
        this.boms = boms;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (boms.count() > 0) {
            return;
        }
        bom("prod-radio", "3", "4", line("mat-elektronik", "2"), line("mat-holz", "1"), line("mat-stahl", "0.5"));
        // Premium devices require Despotium (→ sensitive to sanctions).
        bom("prod-turntable", "5", "6", line("mat-elektronik", "3"), line("mat-holz", "1"), line("mat-stahl", "1.5"),
                line("mat-despotium", "1"));
        bom("prod-tape", "4", "5", line("mat-elektronik", "2"), line("mat-stahl", "0.8"));
        bom("prod-super8", "6", "7", line("mat-elektronik", "4"), line("mat-stahl", "1.0"),
                line("mat-despotium", "2"));
        bom("prod-phone", "2", "2", line("mat-elektronik", "1"), line("mat-stahl", "0.3"));
        bom("prod-typewriter", "5", "3", line("mat-stahl", "5.0"), line("mat-holz", "1"));
    }

    private void bom(String productId, String labor, String energy, BomLine... lines) {
        boms.save(new ProductBom(productId, new BigDecimal(labor), new BigDecimal(energy), List.of(lines)));
    }

    private static BomLine line(String materialId, String qty) {
        return new BomLine(materialId, new BigDecimal(qty));
    }
}
