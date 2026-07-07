package de.acmesoftware.acmesuite.shared;

/**
 * Event: an applicant was hired → consumers should add the person so that they are visible
 * from now on.
 */
public record PersonHired(String personId, String fullName, String unitKey) {
}
