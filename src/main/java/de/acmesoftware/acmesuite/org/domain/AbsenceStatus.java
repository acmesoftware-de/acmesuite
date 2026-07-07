package de.acmesoftware.acmesuite.org.domain;

/** Status of an absence: vacation moves through PLANNED→APPROVED, a sick notice starts at APPROVED. */
public enum AbsenceStatus {
    PLANNED,
    APPROVED,
    REJECTED,
    CANCELLED
}
