package de.acmesoftware.acmesuite.process;

/**
 * Raw materials/components that ACME purchases. The two key materials drive the main products
 * (Despotium→Fidget, Demokratium→Widget); in addition, concrete goods with purchasing restrictions
 * (special CO₂ levy, air freight, possible unethical labor conditions). Auxiliary materials are easily
 * available and purchased directly.
 *
 * <p>Restriction attributes (drive the approval chain, {@link ApprovalRouting}):
 * <ul>
 *   <li>{@code co2} — production/transport causes CO₂ → special levy</li>
 *   <li>{@code airFreight} — flown in by airplane (electronics, strategic Despotium)</li>
 *   <li>{@code unethicalLabor} — possibly produced under unethical labor conditions</li>
 * </ul>
 * The sanction situation depends on the country of origin (not on the material) and is checked in the routing logic.
 */
public enum Material {

    DESPOTIUM("Despotium", false, Product.FIDGET, true, true, false),
    DEMOKRATIUM("Demokratium", false, Product.WIDGET, false, false, false),
    STAHLBLECH("Sheet Steel", false, null, true, false, true),
    HOLZTEILE("Wood Parts", false, null, false, false, true),
    ELEKTRONIK("Electronic Components", false, null, false, true, false),
    FIGETIAXOS("Figetiaxos", true, null, false, false, false),
    WIDGETIX("Widgetix", true, null, false, false, false);

    private final String displayName;
    private final boolean easilyAvailable;
    private final Product producedProduct;
    private final boolean co2;
    private final boolean airFreight;
    private final boolean unethicalLabor;

    Material(String displayName, boolean easilyAvailable, Product producedProduct,
             boolean co2, boolean airFreight, boolean unethicalLabor) {
        this.displayName = displayName;
        this.easilyAvailable = easilyAvailable;
        this.producedProduct = producedProduct;
        this.co2 = co2;
        this.airFreight = airFreight;
        this.unethicalLabor = unethicalLabor;
    }

    public String displayName() {
        return displayName;
    }

    public boolean easilyAvailable() {
        return easilyAvailable;
    }

    public Product producedProduct() {
        return producedProduct;
    }

    public boolean co2() {
        return co2;
    }

    public boolean airFreight() {
        return airFreight;
    }

    public boolean unethicalLabor() {
        return unethicalLabor;
    }
}
