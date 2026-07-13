package de.acmesoftware.acmesuite.org.hr;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
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
 * ADR-0010 on an association-bearing org/HR entity (the whole org graph is audited together):
 * a Person's changes are versioned by Envers and deletion tombstones (hidden from ordinary reads,
 * retained in history). Not {@code @Transactional} — Envers persists audit rows at commit.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class HrEntityAuditTest {

    @Autowired
    PersonRepository persons;
    @Autowired
    EntityManagerFactory emf;

    @Test
    void personChangesAreVersionedAndDeleteTombstones() {
        String id = "person-audit-" + System.nanoTime();
        persons.save(new Person(id, "Alex", "Muster", "alex@acme.test", "Analyst", null));

        // A change -> a new version.
        Person p = persons.findById(id).orElseThrow();
        p.updateJobTitle("Senior Analyst");
        persons.save(p);

        // Delete -> tombstone (never a hard delete).
        Person live = persons.findById(id).orElseThrow();
        live.tombstone("tester", Instant.now());
        persons.save(live);

        // Hidden from ordinary reads (the @SQLRestriction).
        assertThat(persons.findById(id)).isEmpty();

        // Retained in Envers history: multiple revisions, latest is the tombstone, actor stamped.
        EntityManager em = emf.createEntityManager();
        try {
            AuditReader reader = AuditReaderFactory.get(em);
            List<Number> revisions = reader.getRevisions(Person.class, id);
            assertThat(revisions).hasSizeGreaterThanOrEqualTo(3);

            Person latest = reader.find(Person.class, id, revisions.get(revisions.size() - 1));
            assertThat(latest.isDeleted()).isTrue();
            assertThat(latest.getDeletedBy()).isEqualTo("tester");
            assertThat(latest.getJobTitle()).isEqualTo("Senior Analyst");
        } finally {
            em.close();
        }
    }
}
