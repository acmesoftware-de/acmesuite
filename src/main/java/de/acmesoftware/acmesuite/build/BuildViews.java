package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.domain.BomLine;
import de.acmesoftware.acmesuite.build.domain.ProductBom;
import java.math.BigDecimal;
import java.util.List;

/** Serializable ACMEprod views (bills of materials). */
public final class BuildViews {

    private BuildViews() {
    }

    public record BomLineView(String materialId, BigDecimal quantity) {
        public static BomLineView of(BomLine l) {
            return new BomLineView(l.getMaterialId(), l.getQuantity());
        }
    }

    public record ProductBomView(String productId, BigDecimal laborUnits, BigDecimal energyUnits,
                                 List<BomLineView> lines) {
        public static ProductBomView of(ProductBom b) {
            return new ProductBomView(b.getProductId(), b.getLaborUnits(), b.getEnergyUnits(),
                    b.getLines().stream().map(BomLineView::of).toList());
        }
    }
}
