package de.acmesoftware.acmesuite.org.feed;

import java.time.Instant;
import java.util.List;

/**
 * Connector feed of the ACME org: serializes the org isomorphically to the external platform's {@code SourceSnapshot}
 * (ADR-016/041), so that a future {@code AcmeOrgConnector} on the external platform is a thin HTTP mapper.
 *
 * <p>Two layers kept separate (SoR split):
 * <ul>
 *   <li><b>projected</b> ({@link SnapshotFeed}) — units, hierarchy, persons
 *       (displayName/subjectRef/enabled), primary + secondary membership, absences;</li>
 *   <li><b>curated</b> ({@link OverlayPerson}) — title, managerKey, delegateKeys, assistantKeys.</li>
 * </ul>
 *
 * <p>{@code subjectRef} = the person's Entra object id (oid) once they have been provisioned to Entra —
 * the stable SSO anchor against which the login token is mapped to the org person on the external platform; fallback is
 * e-mail as long as no oid is available (reference catalog, applicants). {@code absenceId} = the business
 * reason key (e.g. "urlaub-1"). Validity is left open
 * ({@code null}) where not time-bounded — the external reconciler then sets {@code [observedAt, ∞)}.
 */
public final class OrgFeed {

    private OrgFeed() {
    }

    public record SnapshotFeed(boolean complete, Instant observedAt,
                               List<SourcePerson> persons, List<SourceUnit> units,
                               List<ParentEdge> parentEdges, List<MembershipEdge> memberships,
                               List<MembershipEdge> secondaryMemberships, List<AbsenceFact> absences) {
    }

    public record SourcePerson(String key, String displayName, String subjectRef, boolean enabled) {
    }

    public record SourceUnit(String key, String name) {
    }

    /** {@code parentUnitKey == null} ⇒ root. */
    public record ParentEdge(String childUnitKey, String parentUnitKey) {
    }

    public record MembershipEdge(String personKey, String orgUnitKey) {
    }

    /** Window {@code [from, to)} of a time-bounded absence. */
    public record ValidPeriodView(Instant from, Instant to) {
    }

    public record AbsenceFact(String personKey, String absenceId, String deputyKey, ValidPeriodView validity) {
    }

    public record OverlayPerson(String key, String title, String managerKey,
                                List<String> delegateKeys, List<String> assistantKeys) {
    }
}
