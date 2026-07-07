package de.acmesoftware.acmesuite.base;

/**
 * Access roles of the ACMEsuite (part of <em>ACMEbase</em>). Deliberately kept separate from the
 * business job roles in {@code org} (CFO, buyer …): this is solely about read/write permissions
 * on the suite APIs.
 *
 * <ul>
 *   <li>{@link #WATCH} — read only (GET).</li>
 *   <li>{@link #WORK} — read and write (operational transactions: inquiries, quotes, contracts,
 *       orders).</li>
 *   <li>{@link #ADMIN} — additionally create/modify master data (e.g. new products, price lists,
 *       suppliers).</li>
 * </ul>
 *
 * <p>Hierarchy: {@code ADMIN > WORK > WATCH} (see {@code BaseSecurityConfig}).
 */
public enum AccessRole {

    WATCH,
    WORK,
    ADMIN;

    /** Spring authority name including the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
