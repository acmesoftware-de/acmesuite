package de.acmesoftware.acmesuite.process;

import de.acmesoftware.acmesuite.shared.CountryRestriction;
import java.util.ArrayList;
import java.util.List;

/**
 * Detected restrictions of a purchasing/sales transaction. Drives the long approval path (up to
 * executive management) and — for punitive tariffs/levies — both managing directors.
 *
 * <p>Purchasing: sanction/embargo (country), import tariff (country), CO₂ (material), air freight (material),
 * unethical labor (material). Sales: export ban (country×product), dual-use (product), air shipping.
 * The country restriction ({@link CountryRestriction}) is maintainable (operations cockpit).
 */
public record Restrictions(
        boolean sanctioned, boolean co2, boolean airFreight, boolean unethicalLabor, boolean tariff, // purchasing
        boolean exportBanned, boolean dualUse, boolean airShipping) {                                 // sales

    public static Restrictions none() {
        return new Restrictions(false, false, false, false, false, false, false, false);
    }

    /** Purchasing restrictions from material + restriction of the country of origin. */
    public static Restrictions forPurchase(Material m, CountryRestriction cr) {
        boolean sanctioned = cr == CountryRestriction.GENERAL || cr == CountryRestriction.DESPOTIUM;
        boolean tariff = cr == CountryRestriction.IMPORT_TARIFF;
        return new Restrictions(sanctioned, m.co2(), m.airFreight(), m.unethicalLabor(), tariff,
                false, false, false);
    }

    /** Sales restrictions from product, restriction of the destination country and shipping method. */
    public static Restrictions forSales(Product p, CountryRestriction cr, boolean byAir) {
        boolean banned = cr == CountryRestriction.GENERAL
                || (cr == CountryRestriction.DUAL_USE && p.dualUse())
                || (cr == CountryRestriction.DESPOTIUM && p == Product.FIDGET);
        boolean tariff = cr == CountryRestriction.IMPORT_TARIFF;
        return new Restrictions(false, false, false, false, tariff, banned, p.dualUse(), byAir);
    }

    /** Is there any restriction at all → long path up to executive management. */
    public boolean any() {
        return sanctioned || co2 || airFreight || unethicalLabor || tariff
                || exportBanned || dualUse || airShipping;
    }

    /** Punitive tariffs (sanction/import tariff) or special levies (CO₂) due → both managing directors for purchasing. */
    public boolean tariffsOrLevies() {
        return sanctioned || co2 || tariff;
    }

    /** Short labels for display (e.g. "Sanction · CO₂"). */
    public List<String> labels() {
        List<String> l = new ArrayList<>();
        if (sanctioned) {
            l.add("Sanction");
        }
        if (tariff) {
            l.add("Import Tariff");
        }
        if (co2) {
            l.add("CO₂");
        }
        if (airFreight) {
            l.add("Air Freight");
        }
        if (unethicalLabor) {
            l.add("Labor Conditions");
        }
        if (exportBanned) {
            l.add("Export Ban");
        }
        if (dualUse) {
            l.add("Dual-Use");
        }
        if (airShipping) {
            l.add("Air Shipping");
        }
        return l;
    }
}
