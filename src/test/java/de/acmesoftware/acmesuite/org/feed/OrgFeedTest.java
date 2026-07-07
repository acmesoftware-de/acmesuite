package de.acmesoftware.acmesuite.org.feed;

import static org.assertj.core.api.Assertions.assertThat;

import de.acmesoftware.acmesuite.TestcontainersConfig;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.SnapshotFeed;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks the org source-connector feed: complete, shape-equal to the consumer's SourceSnapshot,
 * and with the canonical facts (root, matrix, absence with substitute, overlay).
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class OrgFeedTest {

    @Autowired
    OrgFeedService feed;

    @Autowired
    PersonRepository persons;

    @Test
    void snapshotIsCompleteAndCoversTheWholeCompany() {
        SnapshotFeed snap = feed.snapshot(Instant.parse("2026-06-22T00:00:00Z"));

        assertThat(snap.complete()).isTrue();
        assertThat(snap.persons()).hasSize(99);
        // Root has no parent edge.
        assertThat(snap.parentEdges())
                .filteredOn(e -> e.childUnitKey().equals("ou-acme"))
                .singleElement()
                .satisfies(e -> assertThat(e.parentUnitKey()).isNull());
        // Without an Entra oid, subjectRef falls back to the email (the demo catalog is not provisioned).
        assertThat(snap.persons())
                .filteredOn(p -> p.key().equals("u-finance-cfo"))
                .singleElement()
                .satisfies(p -> assertThat(p.subjectRef()).isEqualTo("f.finance@acme-group.io"));
    }

    /**
     * As soon as a person carries an Entra Object-Id (oid), THAT is the subjectRef — the stable
     * SSO anchor against which the login token maps to the org person. @Transactional rolls back
     * the oid assignment after the test so that the fallback assertion above stays independent.
     */
    @Test
    @Transactional
    void subjectRefIsTheEntraObjectIdOncePersonIsProvisioned() {
        Person cfo = persons.findById("u-finance-cfo").orElseThrow();
        cfo.assignEntraObjectId("00000000-1111-2222-3333-444444444444");
        persons.save(cfo);

        SnapshotFeed snap = feed.snapshot(Instant.parse("2026-06-22T00:00:00Z"));

        assertThat(snap.persons())
                .filteredOn(p -> p.key().equals("u-finance-cfo"))
                .singleElement()
                .satisfies(p -> assertThat(p.subjectRef()).isEqualTo("00000000-1111-2222-3333-444444444444"));
    }

    @Test
    void snapshotCarriesMatrixAndTimeBoundedAbsence() {
        SnapshotFeed snap = feed.snapshot(Instant.parse("2026-06-22T00:00:00Z"));

        assertThat(snap.secondaryMemberships())
                .anySatisfy(m -> {
                    assertThat(m.personKey()).isEqualTo("u-compliance-1");
                    assertThat(m.orgUnitKey()).isEqualTo("ou-gremium");
                });

        // Vacation of department lead B1 with a substitute and a concrete window.
        assertThat(snap.absences())
                .filteredOn(a -> a.absenceId().equals("urlaub-1"))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.personKey()).isEqualTo("u-abt-b1-lead");
                    assertThat(a.deputyKey()).isEqualTo("u-b1-1-1");
                    assertThat(a.validity().from()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
                    // until=15.07. is inclusive → half-open [from,to) ends at the start of the next day 16.07.
                    assertThat(a.validity().to()).isEqualTo(Instant.parse("2026-07-16T00:00:00Z"));
                });
    }

    @Test
    void overlayCarriesReportingAndDelegation() {
        assertThat(feed.overlay())
                .filteredOn(o -> o.key().equals("u-abt-b1-lead"))
                .singleElement()
                .satisfies(o -> {
                    assertThat(o.title()).isEqualTo("Abteilungsleitung");
                    assertThat(o.managerKey()).isEqualTo("u-fb-b-lead");
                    assertThat(o.delegateKeys()).hasSize(2).contains("u-b1-1-1", "u-b1-2-8");
                    assertThat(o.assistantKeys()).containsExactly("u-fb-b-asst");
                });
    }
}
