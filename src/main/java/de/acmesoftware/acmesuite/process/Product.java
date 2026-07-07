package de.acmesoftware.acmesuite.process;

/**
 * ACME's product portfolio in three classes with different sales/approval paths:
 * main products (sales contract), small products (online shop) and dual-use goods (strictly regulated,
 * e.g. typewriter, rotary-dial telephone — can also be used for military purposes).
 */
public enum Product {

    FIDGET("Fidget", ProductClass.MAIN),
    WIDGET("Widget", ProductClass.MAIN),

    NEXUS("Nexus", ProductClass.SMALL),
    PULSE("Pulse", ProductClass.SMALL),
    VECTOR("Vector", ProductClass.SMALL),

    TYPEWRITER("Typewriter", ProductClass.DUAL_USE),
    ROTARY_PHONE("Rotary Dial Telephone", ProductClass.DUAL_USE);

    private final String displayName;
    private final ProductClass productClass;

    Product(String displayName, ProductClass productClass) {
        this.displayName = displayName;
        this.productClass = productClass;
    }

    public String displayName() {
        return displayName;
    }

    public ProductClass productClass() {
        return productClass;
    }

    public boolean dualUse() {
        return productClass == ProductClass.DUAL_USE;
    }
}
