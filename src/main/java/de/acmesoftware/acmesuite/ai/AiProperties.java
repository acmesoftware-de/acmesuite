package de.acmesoftware.acmesuite.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the AI layer. {@code provider=stub} (default) uses a deterministic
 * fake implementation; {@code provider=ollama} (later) wires in a local LLM via Ollama
 * (tool calling + structured JSON output).
 */
@ConfigurationProperties(prefix = "acme.ai")
public record AiProperties(
        String provider,
        Ollama ollama) {

    public record Ollama(String baseUrl, String model) {
    }
}
