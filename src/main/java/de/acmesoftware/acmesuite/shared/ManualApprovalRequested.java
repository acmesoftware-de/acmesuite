package de.acmesoftware.acmesuite.shared;

/**
 * Event: a transaction needs a manual approval (ACME analog → signature folder in circulation).
 * {@code ref} = external key (e.g. ACMEcrm order), {@code valueEur} = amount for the routing tier,
 * {@code kind} = "SALES" (sale, yellow) or "PURCHASE" (purchase, blue).
 */
public record ManualApprovalRequested(String ref, String subject, long valueEur, String kind) {
}
