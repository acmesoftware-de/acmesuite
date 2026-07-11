package de.acmesoftware.acmesuite.base.domain;

/**
 * Lifecycle of an ACMEbase user account.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — may sign in.</li>
 *   <li>{@link #PENDING} — a federated identity signed in but has no role yet; awaits an admin's
 *       role assignment. No API access until activated.</li>
 *   <li>{@link #DISABLED} — sign-in blocked.</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE,
    PENDING,
    DISABLED
}
