package de.acmesoftware.acmesuite.org.domain;

/**
 * Type of power of attorney (modeled on German commercial law).
 *
 * <ul>
 *   <li>{@code PROKURA} — comprehensive commercial-law representation (§ 48 HGB)</li>
 *   <li>{@code HANDLUNGSVOLLMACHT} — limited to the ordinary course of business (§ 54 HGB)</li>
 *   <li>{@code GENERAL} — general power of attorney, usually without an amount limit</li>
 *   <li>{@code SPECIAL} — special power of attorney for a delimited purpose</li>
 * </ul>
 */
public enum PowerOfAttorneyType {
    PROKURA,
    HANDLUNGSVOLLMACHT,
    GENERAL,
    SPECIAL
}
