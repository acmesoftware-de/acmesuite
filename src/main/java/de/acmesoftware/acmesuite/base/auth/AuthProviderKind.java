package de.acmesoftware.acmesuite.base.auth;

/** How an {@link AuthProvider} authenticates an identity. */
public enum AuthProviderKind {
    /** Username + password against the local Base directory. */
    LOCAL,
    /** OpenID Connect redirect flow against an external IdP (Entra, generic OIDC). */
    OIDC
}
