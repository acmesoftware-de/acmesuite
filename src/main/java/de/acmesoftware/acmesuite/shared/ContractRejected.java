package de.acmesoftware.acmesuite.shared;

/**
 * Event: a contract with an external reference was rejected during circulation (instead of signed).
 * {@code ref} = external key (e.g. applicant ID), {@code day} = day index of the rejection.
 */
public record ContractRejected(String ref, long day) {
}
