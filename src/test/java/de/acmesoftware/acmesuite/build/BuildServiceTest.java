package de.acmesoftware.acmesuite.build;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.build.BuildViews.BomLineView;
import de.acmesoftware.acmesuite.build.BuildViews.ProductBomView;
import de.acmesoftware.acmesuite.build.domain.BomLine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** ACMEprod: bills of materials of the products (basis for the later production cycle). */
@SpringBootTest
@Import(TestcontainersConfig.class)
class BuildServiceTest {

    @Autowired
    BuildService prod;

    @Test
    void seedsBomsForAllProducts() {
        assertThat(prod.listBoms()).extracting(ProductBomView::productId)
                .contains("prod-radio", "prod-turntable", "prod-typewriter");
        assertThat(prod.getBom("prod-radio")).get().satisfies(b -> {
            assertThat(b.laborUnits()).isEqualByComparingTo("3");
            assertThat(b.energyUnits()).isEqualByComparingTo("4");
            assertThat(b.lines()).extracting(BomLineView::materialId)
                    .contains("mat-elektronik", "mat-holz", "mat-stahl");
        });
    }

    @Test
    void upsertsBom() {
        var v = prod.upsertBom("prod-test", new BigDecimal("2"), new BigDecimal("3"),
                List.of(new BomLine("mat-stahl", new BigDecimal("1.0"))));
        assertThat(v.lines()).hasSize(1);
        assertThat(prod.getBom("prod-test")).isPresent();
    }
}
