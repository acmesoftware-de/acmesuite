package de.acmesoftware.acmesuite.process;

/** Sales class of a product — determines channel and approval depth. */
public enum ProductClass {
    /** Main product (Fidget/Widget): sales contract via phone/email. */
    MAIN,
    /** Small product (Nexus/Pulse/Vector): online shop; contract only for B2B special conditions. */
    SMALL,
    /** Dual-use (Orbit/Quantum/Vertex): regulated, compliance + four-eyes across all departments. */
    DUAL_USE
}
