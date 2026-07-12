package de.acmesoftware.acmesuite.assist.ollama;

import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ToolCall;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

/**
 * Recovers a tool call that a model leaked as <em>text</em> in {@code message.content} instead of a
 * structured {@code tool_calls} object — observed in ~25–30 % of warm {@code qwen2.5:7b} turns
 * during the phase-1 spike (see the plan, spike #3). Without this, the engine would treat the
 * garbage text as the final answer.
 *
 * <p>Conservative on purpose: it only recovers when the embedded JSON names a <em>known</em> tool,
 * so ordinary prose containing braces is not misread as a call. Uses Spring Boot's {@link JsonParser}
 * (Jackson at runtime, no compile-time Jackson dependency).
 */
public final class ToolCallRecovery {

    private static final JsonParser JSON = JsonParserFactory.getJsonParser();

    private ToolCallRecovery() {
    }

    @SuppressWarnings("unchecked")
    public static List<ToolCall> fromContent(String content, Set<String> knownTools) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            Map<String, Object> root = JSON.parseMap(content.substring(start, end + 1));
            Map<String, Object> call = root.get("function") instanceof Map<?, ?> fn
                    ? (Map<String, Object>) fn
                    : root;
            if (!(call.get("name") instanceof String name) || !knownTools.contains(name)) {
                return List.of();
            }
            Object rawArgs = call.containsKey("arguments") ? call.get("arguments") : call.get("parameters");
            Map<String, Object> arguments = rawArgs instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Map.of();
            return List.of(new ToolCall(name, arguments));
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
