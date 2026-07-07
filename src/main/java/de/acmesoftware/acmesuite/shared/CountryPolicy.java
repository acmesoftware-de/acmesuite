package de.acmesoftware.acmesuite.shared;

import java.util.List;

/**
 * Port (hexagonal): the maintainable country restrictions, so that the process engine can use them
 * without accessing the {@code thesim} domain model (module boundaries). The adapter lives in {@code thesim}.
 */
public interface CountryPolicy {

    /** Restriction of the country (NONE if unknown / without restriction). */
    CountryRestriction restrictionFor(String country);

    /** Countries that can be sourced from. */
    List<String> sourcingCountries();

    /** Countries that can be sold to. */
    List<String> marketCountries();
}
