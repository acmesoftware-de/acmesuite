package de.acmesoftware.acmesuite.org.directory;

import java.util.List;

/**
 * The outcome of one provisioning run (ADR-0011 §2): legible per counter, and per-person errors
 * collected rather than aborting the whole run. A partially failed run is a list of what went
 * wrong, not a single boolean.
 *
 * @param dryRun          true if nothing was written (planned/logged only)
 * @param eligible        persons considered (members, not applicants)
 * @param created         accounts newly created
 * @param updated         accounts whose attributes were written
 * @param disabled        accounts blocked (a person who left)
 * @param passwordsSet    initial passwords set (a separate permission — may be fewer than created)
 * @param skippedApplicants applicants deliberately left out (ADR-0011 §6)
 * @param errors          "person-id: message" for each per-person failure
 */
public record ProvisioningRun(
        boolean dryRun,
        int eligible,
        int created,
        int updated,
        int disabled,
        int passwordsSet,
        int skippedApplicants,
        List<String> errors) {
}
