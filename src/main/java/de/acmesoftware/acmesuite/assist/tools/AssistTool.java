package de.acmesoftware.acmesuite.assist.tools;

/**
 * A read tool an agent may call: a stable {@code name} + a model-facing {@code description} +
 * an {@code inputSchema} (JSON Schema of the arguments), mapped to a GET operation of the REST
 * contract ({@code pathTemplate}, may contain {@code {placeholders}}). Phase 1 is reads only.
 */
public record AssistTool(String name, String description, String inputSchema, String pathTemplate) {

    /** Fill one {@code {param}} placeholder — enough for the phase-1 Customer-360 tools. */
    public String resolve(String param, String value) {
        return pathTemplate.replace("{" + param + "}", value);
    }
}
