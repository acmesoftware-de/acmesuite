package de.acmesoftware.acmesuite.build;

import de.acmesoftware.acmesuite.build.BuildViews.ProductBomView;
import de.acmesoftware.acmesuite.build.domain.BomLine;
import de.acmesoftware.acmesuite.build.domain.ProductBom;
import de.acmesoftware.acmesuite.build.domain.ProductBomRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** ACMEprod application logic: read/maintain bills of materials. Provides the basis for the daily production cycle. */
@Service
@Transactional
public class BuildService {

    private final ProductBomRepository boms;

    public BuildService(ProductBomRepository boms) {
        this.boms = boms;
    }

    @Transactional(readOnly = true)
    public List<ProductBomView> listBoms() {
        return boms.findAll().stream()
                .sorted(Comparator.comparing(ProductBom::getProductId))
                .map(ProductBomView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductBomView> getBom(String productId) {
        return boms.findById(productId).map(ProductBomView::of);
    }

    public ProductBomView upsertBom(String productId, BigDecimal laborUnits, BigDecimal energyUnits,
                                    List<BomLine> lines) {
        if (laborUnits == null || energyUnits == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "laborUnits and energyUnits are required");
        }
        ProductBom bom = boms.findById(productId)
                .map(b -> {
                    b.update(laborUnits, energyUnits, lines);
                    return b;
                })
                .orElseGet(() -> new ProductBom(productId, laborUnits, energyUnits, lines));
        return ProductBomView.of(boms.save(bom));
    }
}
