package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.DbSchemaService;
import de.acmesoftware.acmesuite.base.DbSchemaService.RowPage;
import de.acmesoftware.acmesuite.base.DbSchemaService.TableInfo;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ACMEbase DB schema browser (ADMIN only; guarded by URL rule in security config). */
@RestController
@RequestMapping("/api/base/db")
public class DbSchemaController {

    private final DbSchemaService schema;

    public DbSchemaController(DbSchemaService schema) {
        this.schema = schema;
    }

    @GetMapping("/schema")
    public List<TableInfo> schema() {
        return schema.introspect();
    }

    @GetMapping("/tables/{table}/rows")
    public ResponseEntity<RowPage> rows(@PathVariable String table,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return schema.rows(table, page, size)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
