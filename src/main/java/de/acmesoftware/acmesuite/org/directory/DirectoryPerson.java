package de.acmesoftware.acmesuite.org.directory;

/**
 * A person as the directory needs to see them — expressed in HR terms, free of any directory's
 * vocabulary (ADR-0011 §2). The adapter maps these fields onto its protocol; the port and the
 * orchestration never mention Graph, UPNs or object ids beyond what is unavoidable.
 *
 * @param loginName    the work address that doubles as the sign-in name (the idempotency key,
 *                     ADR-0011 §4). NOT a mailbox claim on its own — see {@code mail}.
 * @param displayName  full name for display
 * @param givenName    first name
 * @param surname      last name
 * @param mail         the deliverable work e-mail. Set as a real directory attribute, not merely
 *                     as the login name: the downstream org projection reads exactly this to make
 *                     a person reachable ("who must approve", "with whom does it sit").
 * @param jobTitle     job title, or {@code null}
 * @param department   org unit name, or {@code null}
 * @param employeeId   the HR id, carried so the directory can point back at the record
 * @param active       whether the account should be enabled
 */
public record DirectoryPerson(
        String loginName,
        String displayName,
        String givenName,
        String surname,
        String mail,
        String jobTitle,
        String department,
        String employeeId,
        boolean active) {
}
