package de.acmesoftware.acmesuite.crm;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.crm.domain.Product;
import de.acmesoftware.acmesuite.crm.domain.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * ADR-0010 on a business entity (crm/supply/build retrofit): every change is versioned by Envers
 * and deletion tombstones (hidden from ordinary reads, retained in history). Not {@code @Transactional}
 * — Envers persists audit rows at commit, so each save must run in its own transaction.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class CrmEntityAuditTest {

    @Autowired
    ProductRepository products;
    @Autowired
    EntityManagerFactory emf;

    @Test
    void productChangesAreVersionedAndDeleteTombstones() {
        String id = "prod-audit-" + System.nanoTime();
        products.save(new Product(id, "SKU-" + id, "Widget", "tools", "pcs", true, null));

        // A change -> a new version.
        Product p = products.findById(id).orElseThrow();
        p.update(null, "Widget Pro", null, null, null, null);
        products.save(p);

        // Delete -> tombstone (never a hard delete).
        Product live = products.findById(id).orElseThrow();
        live.tombstone("tester", Instant.now());
        products.save(live);

        // Hidden from ordinary reads (the @SQLRestriction).
        assertThat(products.findById(id)).isEmpty();

        // Retained in Envers history: multiple revisions, latest is the tombstone, actor stamped.
        EntityManager em = emf.createEntityManager();
        try {
            AuditReader reader = AuditReaderFactory.get(em);
            List<Number> revisions = reader.getRevisions(Product.class, id);
            assertThat(revisions).hasSizeGreaterThanOrEqualTo(3);

            Product latest = reader.find(Product.class, id, revisions.get(revisions.size() - 1));
            assertThat(latest.isDeleted()).isTrue();
            assertThat(latest.getDeletedBy()).isEqualTo("tester");
            assertThat(latest.getName()).isEqualTo("Widget Pro");
        } finally {
            em.close();
        }
    }
}
