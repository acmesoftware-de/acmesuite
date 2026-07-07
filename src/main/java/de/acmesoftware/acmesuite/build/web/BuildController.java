package de.acmesoftware.acmesuite.build.web;

import de.acmesoftware.acmesuite.build.BuildService;
import de.acmesoftware.acmesuite.build.BuildViews.ProductBomView;
import de.acmesoftware.acmesuite.build.domain.BomLine;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ACMEprod HTTP API: bills of materials (BOM) per product. */
@RestController
@RequestMapping("/api/build")
public class BuildController {

    private final BuildService prod;

    public BuildController(BuildService prod) {
        this.prod = prod;
    }

    @GetMapping("/boms")
    public List<ProductBomView> boms() {
        return prod.listBoms();
    }

    @GetMapping("/products/{id}/bom")
    public ResponseEntity<ProductBomView> bom(@PathVariable String id) {
        return ResponseEntity.of(prod.getBom(id));
    }

    @PutMapping("/products/{id}/bom")
    public ProductBomView upsertBom(@PathVariable String id, @RequestBody BomWrite req) {
        List<BomLine> lines = req.lines() == null ? List.of() : req.lines().stream()
                .map(l -> new BomLine(l.materialId(), l.quantity())).toList();
        return prod.upsertBom(id, req.laborUnits(), req.energyUnits(), lines);
    }

    public record BomWrite(BigDecimal laborUnits, BigDecimal energyUnits, List<LineWrite> lines) {
    }

    public record LineWrite(String materialId, BigDecimal quantity) {
    }
}
