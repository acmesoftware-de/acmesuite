package de.acmesoftware.acmesuite.process;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The approval rules of "ACME offline" — who must sign off on a transaction depending on value and
 * restrictions. Pure logic (no org resolution, no random numbers); returns ordered
 * {@link ApprovalStep}s. Empty list = no approval needed (direct purchase / online order).
 *
 * <p><b>Purchasing</b> (order): specialist department (+ division management depending on amount) → purchasing →
 * head of purchasing → [controlling → CFO → executive management]. <b>Sales</b>: sales → specialist department →
 * head of sales → [controlling → CFO → executive management]. The later stations are added depending on
 * threshold/restrictions.
 *
 * <p>Product lines: Demokratium→Widget→business unit A (ou-fb-a), Despotium→Fidget→business unit B
 * (ou-fb-b); the remaining materials go through business unit B.
 */
@Component
public class ApprovalRouting {

    // General approval limit (configurable; from here on additionally controlling/CFO).
    static final long GENERAL_LIMIT_EUR = 10_000;
    // Flat role-based approval limits (mirror of the seeded ApprovalLimits).
    static final long LIMIT_TEAM_LEAD_EUR = 50_000;      // from here on additionally division management
    static final long LIMIT_CONTROLLING_LEAD_EUR = 250_000; // above this additionally the CFO

    /** Purchasing: key/restricted materials require approvals, auxiliary materials do not. */
    public List<ApprovalStep> forPurchase(Material m, long valueEur, Restrictions r, boolean controllingSpotCheck) {
        if (m.easilyAvailable()) {
            return List.of(); // easily available auxiliary material → direct purchase, no circulation folder
        }
        List<ApprovalStep> steps = new ArrayList<>();
        // Specialist department (+ division management depending on amount)
        steps.add(ApprovalStep.unit("Specialist Department", abteilungOf(m)));
        if (valueEur >= LIMIT_TEAM_LEAD_EUR) {
            steps.add(ApprovalStep.unit("Division Management", fachbereichOf(m)));
        }
        steps.add(ApprovalStep.person("Procurement", "u-einkauf-1"));
        steps.add(ApprovalStep.person("Head of Procurement", "u-einkauf-lead"));

        if (r.any()) {
            // Variant 3 — restrictions: up to executive management.
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
            if (valueEur > LIMIT_CONTROLLING_LEAD_EUR) {
                steps.add(ApprovalStep.person("CFO", "u-finance-cfo"));
            }
            steps.add(ApprovalStep.person("Executive Management", "u-gf-1"));
            if (r.tariffsOrLevies()) {
                steps.add(ApprovalStep.person("Executive Management II", "u-gf-2")); // punitive tariffs/levies → both
            }
        } else if (valueEur >= GENERAL_LIMIT_EUR) {
            // Variant 2 — above the threshold: controlling → CFO.
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
            steps.add(ApprovalStep.person("CFO", "u-finance-cfo"));
        } else if (controllingSpotCheck) {
            // Variant 1 — below the threshold: spot check by controlling on 2 % of orders.
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
        }
        return steps;
    }

    /** Sales: by value and restrictions; small products without restriction/below the threshold stay online. */
    public List<ApprovalStep> forSales(Product p, long valueEur, Restrictions r, boolean controllingSpotCheck) {
        if (p.productClass() == ProductClass.SMALL && !r.any() && valueEur < GENERAL_LIMIT_EUR) {
            return List.of(); // standard online order → no contract
        }
        List<ApprovalStep> steps = new ArrayList<>();
        steps.add(ApprovalStep.person("Sales", "u-vertrieb-1"));
        steps.add(ApprovalStep.unit("Specialist Department", abteilungOf(p)));
        steps.add(ApprovalStep.person("Head of Sales", "u-vertrieb-lead"));

        if (r.any()) {
            // Variant 3 — restrictions: controlling → CFO → executive management (always both).
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
            steps.add(ApprovalStep.person("CFO", "u-finance-cfo"));
            steps.add(ApprovalStep.person("Executive Management I", "u-gf-1"));
            steps.add(ApprovalStep.person("Executive Management II", "u-gf-2"));
        } else if (valueEur >= GENERAL_LIMIT_EUR) {
            // Variant 2 — above the threshold: controlling → CFO (if above the controlling limit).
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
            if (valueEur > LIMIT_CONTROLLING_LEAD_EUR) {
                steps.add(ApprovalStep.person("CFO", "u-finance-cfo"));
            }
        } else if (controllingSpotCheck) {
            // Variant 1 — below the threshold: spot check by controlling at 2 %.
            steps.add(ApprovalStep.unit("Controlling", "ou-controlling"));
        }
        return steps;
    }

    /** HR: hiring is lightweight, termination involves legal + executive management. */
    public List<ApprovalStep> forHr(ContractType type) {
        return switch (type) {
            case HR_HIRE -> List.of(
                    ApprovalStep.person("HR Associate", "u-hr-1"),
                    ApprovalStep.unit("Head of HR", "ou-hr"),
                    ApprovalStep.person("Executive Management", "u-gf-1"));
            case HR_TERMINATION -> List.of(
                    ApprovalStep.unit("Head of HR", "ou-hr"),
                    ApprovalStep.unit("Legal Department", "ou-legal"),
                    ApprovalStep.person("Executive Management", "u-gf-1"));
            default -> throw new IllegalArgumentException("Not an HR contract type: " + type);
        };
    }

    // ── Product-line assignment ──
    private static String abteilungOf(Material m) {
        return m == Material.DEMOKRATIUM ? "ou-abt-a1" : "ou-abt-b1";
    }

    private static String fachbereichOf(Material m) {
        return m == Material.DEMOKRATIUM ? "ou-fb-a" : "ou-fb-b";
    }

    private static String abteilungOf(Product p) {
        return p == Product.WIDGET ? "ou-abt-a1" : "ou-abt-b1";
    }
}
