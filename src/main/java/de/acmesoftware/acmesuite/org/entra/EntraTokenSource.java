package de.acmesoftware.acmesuite.org.entra;

/** Provides a Bearer token for the Graph API (decouples the Graph client from token acquisition — testable). */
interface EntraTokenSource {

    /** Valid access token (without the {@code Bearer } prefix). */
    String token();
}
