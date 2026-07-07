package de.acmesoftware.acmesuite.shared;

/**
 * Import/export restriction of a country (maintainable in the operations cockpit). Drives the approval
 * path in the process engine.
 *
 * <ul>
 *   <li>{@link #NONE} — no restriction (normal trade)</li>
 *   <li>{@link #DESPOTIUM} — Despotium-related goods restricted (sourcing/sale of strategic line)</li>
 *   <li>{@link #DUAL_USE} — dual-use goods may not be sold there</li>
 *   <li>{@link #GENERAL} — general embargo (any trade requires approval up to management)</li>
 *   <li>{@link #IMPORT_TARIFF} — penalty/import tariff due (levy + both managing directors)</li>
 * </ul>
 */
public enum CountryRestriction {
    NONE, DESPOTIUM, DUAL_USE, GENERAL, IMPORT_TARIFF
}
