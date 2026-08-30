package de.acmesoftware.acmesuite.org.directory;

import java.util.Optional;

/**
 * The seam between HRM and a directory (ADR-0011 §2). HRM is the system of record; this port is how
 * it makes an external directory MIRROR the workforce. One-directional — the directory is never
 * read back as truth about who exists, only the object id returns as the sign-in anchor.
 *
 * <p>Expressed in HR terms on purpose: upsert a person, disable a person, set an initial password.
 * Everything a specific directory needs to know (a UPN here, a group's object id there, which
 * attribute carries the mail) lives in the adapter, not here.
 */
public interface DirectoryProvisioner {

    /** The object id the directory holds for this login name, or empty if it has no such account. */
    Optional<String> findObjectId(String loginName);

    /**
     * Creates the account and returns its object id. The create call carries the initial password
     * inline (ADR-0011 §3), because a directory requires one at creation.
     */
    String create(DirectoryPerson person, String initialPassword);

    /**
     * Writes the mutable attributes (display name, mail, job title, department, enabled) of an
     * existing account. The password is NOT part of this call — attributes and the password profile
     * hang on two different permissions, and sending them together lets a missing password
     * permission take the harmless attributes down with it (ADR-0011 §3).
     */
    void updateAttributes(String objectId, DirectoryPerson person);

    /**
     * Sets the initial password in its OWN request — the second of the two permissions. A directory
     * that has not granted it fails only here; the attributes from {@link #updateAttributes} have
     * already landed. Returns false when the permission was refused, true when the password was set.
     */
    boolean setInitialPassword(String objectId, String password);

    /** Blocks the account (a person who left, ADR-0011 §7). Never deletes. */
    void disable(String objectId);

    /** Adds the account to a directory group by the group's configured name. */
    void addToGroup(String objectId, String groupName);
}
