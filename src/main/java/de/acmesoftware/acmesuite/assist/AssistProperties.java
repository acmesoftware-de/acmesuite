package de.acmesoftware.acmesuite.assist;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the ACMEassist co-pilot (ADR-0008). {@code provider=stub} (default) uses the
 * deterministic {@link StubAssistantEngine}; {@code provider=ollama} (later) wires the Spring AI +
 * langgraph4j engine over a self-hosted model. Registered via {@code @ConfigurationPropertiesScan}
 * on the application, mirroring {@code ai.AiProperties}.
 */
@ConfigurationProperties(prefix = "acme.assist")
public record AssistProperties(
        Boolean enabled,
        String provider,
        Ollama ollama,
        Budget budget,
        String localeDefault) {

    /** Two-tier CPU routing (ADR-0008): a small model for easy turns, the main model on demand. */
    public record Ollama(String baseUrl, String modelFast, String modelMain, Integer numCtx) {
    }

    public record Budget(Integer maxToolIterations, Integer perUserDailyTokens) {
    }

    /** Global kill switch (governance G4). Absent ⇒ enabled. */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    /** Provider id; {@code stub} when unset — a deterministic default, as with {@code acme.ai}. */
    public String providerOrDefault() {
        return provider == null || provider.isBlank() ? "stub" : provider;
    }
}
