package de.acmesoftware.acmesuite.org.domain;

/** Signature rule of a power of attorney: sole vs. joint representation. */
public enum SignatureRule {
    /** Sole representation — may sign alone. */
    SOLE,
    /** Joint representation — only together with (at least) one further authorized signatory. */
    JOINT
}
