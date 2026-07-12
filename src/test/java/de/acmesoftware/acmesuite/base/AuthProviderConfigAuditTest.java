package de.acmesoftware.acmesuite.base;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.base.auth.ProviderConfigService;
import de.acmesoftware.acmesuite.base.domain.AuthProviderConfig;
import de.acmesoftware.acmesuite.base.domain.AuthProviderConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * ADR-0010 on the reference entity: deletion tombstones (hidden from ordinary reads, retained in
 * history) and every change is versioned by Envers with the acting user. Not {@code @Transactional}
 * — Envers persists audit rows at commit, so each service call must run in its own transaction.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class AuthProviderConfigAuditTest {

    @Autowired
    ProviderConfigService configs;
    @Autowired
    AuthProviderConfigRepository repo;
    @Autowired
    EntityManagerFactory emf;

    @Test
    void deleteTombstonesButHistoryIsRetained() {
        configs.upsert("oidc", true, Map.of("issuerUri", "https://idp.test/", "clientId", "c",
                "clientSecret", "s", "redirectUri", "https://app/cb"));
        String id = repo.findByProviderId("oidc").orElseThrow().getId();

        // A change -> a new version.
        configs.upsert("oidc", false, Map.of());

        // Delete -> tombstone.
        configs.delete("oidc");

        // Hidden from ordinary reads (the @SQLRestriction).
        assertThat(repo.findByProviderId("oidc")).isEmpty();

        // Retained in Envers history: multiple revisions, latest is the tombstone, actor stamped.
        EntityManager em = emf.createEntityManager();
        try {
            AuditReader reader = AuditReaderFactory.get(em);
            List<Number> revisions = reader.getRevisions(AuthProviderConfig.class, id);
            assertThat(revisions).hasSizeGreaterThanOrEqualTo(2);

            AuthProviderConfig latest = reader.find(AuthProviderConfig.class, id,
                    revisions.get(revisions.size() - 1));
            assertThat(latest.isDeleted()).isTrue();
            assertThat(latest.getDeletedBy()).isEqualTo("system");
        } finally {
            em.close();
        }
    }
}
