package de.acmesoftware.acmesuite.process;

/**
 * A required approval in the contract circulation. Either bound to a unit (whose management
 * signs → resolved later via {@code OrgDirectory.leadOf}) or to a fixed person
 * (e.g. a specific managing director).
 */
public record ApprovalStep(String label, String unitKey, String fixedPersonKey) {

    public static ApprovalStep unit(String label, String unitKey) {
        return new ApprovalStep(label, unitKey, null);
    }

    public static ApprovalStep person(String label, String personKey) {
        return new ApprovalStep(label, null, personKey);
    }
}
