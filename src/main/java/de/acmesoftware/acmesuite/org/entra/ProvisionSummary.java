package de.acmesoftware.acmesuite.org.entra;

import java.util.List;

/**
 * Result of a provisioning run.
 *
 * @param dryRun           was it a pure planning run (no writes)?
 * @param eligible         provisionable persons (active, non-applicants, with email)
 * @param created          newly created in Entra
 * @param updated          existing ones updated
 * @param skippedApplicants skipped applicants
 * @param managersLinked   manager relationships set
 * @param errors           errors per person (empty = clean)
 */
public record ProvisionSummary(boolean dryRun, int eligible, int created, int updated,
                               int skippedApplicants, int managersLinked, List<String> errors) {
}
