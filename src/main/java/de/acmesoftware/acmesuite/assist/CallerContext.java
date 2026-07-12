package de.acmesoftware.acmesuite.assist;

/**
 * Who is asking, and how to reach the API as them (ADR-0008 Decision 2). Captured on the request
 * thread and threaded into the engine so every tool call can be dispatched <em>as the user</em>.
 *
 * @param user          the caller's identity (for audit/logging)
 * @param authorization the caller's raw {@code Authorization} header, forwarded verbatim on
 *                      loopback calls; {@code null} when auth is disabled (local dev)
 * @param baseUrl       the server's own base URL (scheme://host:port) for the in-process loopback
 */
public record CallerContext(String user, String authorization, String baseUrl) {
}
