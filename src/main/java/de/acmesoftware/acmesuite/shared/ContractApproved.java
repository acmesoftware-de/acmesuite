package de.acmesoftware.acmesuite.shared;

/** Event: the folder of an external transaction has been fully signed (returned to the originator). */
public record ContractApproved(String ref, String approverKey) {
}
