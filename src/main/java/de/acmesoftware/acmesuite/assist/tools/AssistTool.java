package de.acmesoftware.acmesuite.assist.tools;

/**
 * A read tool an agent may call: a stable {@code name} + a model-facing {@code description},
 * mapped to a GET operation of the REST contract ({@code pathTemplate}, may contain
 * {@code {placeholders}}). Phase 1 is reads only; write tools (with confirmation) arrive in
 * phase 3. The tool surface is generated from / validated against the OpenAPI specs and curated
 * per agent (ADR-0008).
 */
public record AssistTool(String name, String description, String pathTemplate) {

    /** Fill one {@code {param}} placeholder — enough for the phase-1 Customer-360 tools. */
    public String resolve(String param, String value) {
        return pathTemplate.replace("{" + param + "}", value);
    }
}
