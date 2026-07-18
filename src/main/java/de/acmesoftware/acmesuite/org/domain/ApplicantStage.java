package de.acmesoftware.acmesuite.org.domain;

/**
 * Recruiting pipeline stage of an applicant (applicant=true). Ordered from first contact to
 * decision; {@code OFFER} candidates are the ones to hire, {@code REJECTED} the declined ones.
 */
public enum ApplicantStage {
    NEW,
    SCREENING,
    INTERVIEW,
    OFFER,
    REJECTED
}
