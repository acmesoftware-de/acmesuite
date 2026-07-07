package de.acmesoftware.acmesuite.org.feed;

import de.acmesoftware.acmesuite.org.domain.AbsenceRepository;
import de.acmesoftware.acmesuite.org.domain.OrgUnitRepository;
import de.acmesoftware.acmesuite.org.domain.Person;
import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.AbsenceFact;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.MembershipEdge;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.OverlayPerson;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.ParentEdge;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.SnapshotFeed;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.SourcePerson;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.SourceUnit;
import de.acmesoftware.acmesuite.org.feed.OrgFeed.ValidPeriodView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the source-neutral org feed from the org model. Sorted deterministically by key
 * (the external reconciler diffs by key anyway, and a stable order makes diffs/tests easier).
 */
@Service
@Transactional(readOnly = true)
class OrgFeedService {

    private final PersonRepository persons;
    private final OrgUnitRepository units;
    private final AbsenceRepository absences;

    OrgFeedService(PersonRepository persons, OrgUnitRepository units, AbsenceRepository absences) {
        this.persons = persons;
        this.units = units;
        this.absences = absences;
    }

    SnapshotFeed snapshot(Instant observedAt) {
        List<Person> allPersons = persons.findAll();

        List<SourcePerson> sourcePersons = allPersons.stream()
                .map(p -> new SourcePerson(p.getId(), p.fullName(), subjectRef(p), p.isActive()))
                .sorted(Comparator.comparing(SourcePerson::key))
                .toList();

        List<SourceUnit> sourceUnits = units.findAll().stream()
                .map(u -> new SourceUnit(u.getId(), u.getName()))
                .sorted(Comparator.comparing(SourceUnit::key))
                .toList();

        List<ParentEdge> parentEdges = units.findAll().stream()
                .map(u -> new ParentEdge(u.getId(), u.getParent() == null ? null : u.getParent().getId()))
                .sorted(Comparator.comparing(ParentEdge::childUnitKey))
                .toList();

        List<MembershipEdge> memberships = allPersons.stream()
                .filter(p -> p.getPrimaryOrgUnit() != null)
                .map(p -> new MembershipEdge(p.getId(), p.getPrimaryOrgUnit().getId()))
                .sorted(Comparator.comparing(MembershipEdge::personKey))
                .toList();

        List<MembershipEdge> secondary = allPersons.stream()
                .flatMap(p -> p.getSecondaryUnitIds().stream().map(uid -> new MembershipEdge(p.getId(), uid)))
                .sorted(Comparator.comparing(MembershipEdge::personKey).thenComparing(MembershipEdge::orgUnitKey))
                .toList();

        List<AbsenceFact> absenceFacts = absences.findAll().stream()
                .map(a -> new AbsenceFact(
                        a.getPerson().getId(),
                        a.getReasonKey(),
                        a.getSubstitute() == null ? null : a.getSubstitute().getId(),
                        new ValidPeriodView(
                                toInstant(a.getPeriod() == null ? null : a.getPeriod().from()),
                                toExclusiveEnd(a.getPeriod() == null ? null : a.getPeriod().until()))))
                .sorted(Comparator.comparing(AbsenceFact::personKey).thenComparing(AbsenceFact::absenceId))
                .toList();

        return new SnapshotFeed(true, observedAt, sourcePersons, sourceUnits, parentEdges,
                memberships, secondary, absenceFacts);
    }

    List<OverlayPerson> overlay() {
        return persons.findAll().stream()
                .map(p -> new OverlayPerson(p.getId(), p.getJobTitle(),
                        p.getManager() == null ? null : p.getManager().getId(),
                        List.copyOf(p.getDelegateIds()), List.copyOf(p.getAssistantIds())))
                .sorted(Comparator.comparing(OverlayPerson::key))
                .toList();
    }

    /**
     * SSO subject: the Entra object id (oid) is the stable anchor against which the login token can
     * later be mapped (the org projection on the external platform carries the same value as subjectRef).
     * Fallback is e-mail as long as a person has not yet been provisioned to Entra (reference catalog,
     * applicants without an oid) — then the old linkage applies as in the reference company catalog.
     */
    private static String subjectRef(Person p) {
        String oid = p.getEntraObjectId();
        return (oid == null || oid.isBlank()) ? p.getEmail() : oid;
    }

    private static Instant toInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /**
     * {@link de.acmesoftware.acmesuite.shared.DateRange#until()} is <b>inclusive</b>, whereas the external platform's {@code ValidPeriod}
     * is half-open {@code [from, to)}. The end date is therefore mapped to the start of the following day,
     * so that an absence "until 15.07." still covers the 15th — and a single-day absence does not
     * arrive as an empty/invalid interval.
     */
    private static Instant toExclusiveEnd(LocalDate inclusiveUntil) {
        return inclusiveUntil == null ? null : inclusiveUntil.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
