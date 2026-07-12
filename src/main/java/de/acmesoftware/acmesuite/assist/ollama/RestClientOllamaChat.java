package de.acmesoftware.acmesuite.assist.ollama;

import de.acmesoftware.acmesuite.assist.AssistProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to a self-hosted Ollama server ({@code POST {baseUrl}/api/chat}, non-streaming) and maps
 * the tool-calling request/response. Only created when {@code acme.assist.provider=ollama}, so the
 * default (stub) context never reaches for Ollama.
 *
 * <p><b>Interim, unverified against a live server</b> (phase-1 plan spike #3): the wire mapping
 * follows the Ollama spec; true token streaming and arg schemas are follow-ups.
 */
@Component
@ConditionalOnProperty(prefix = "acme.assist", name = "provider", havingValue = "ollama")
public class RestClientOllamaChat implements OllamaChatClient {

    private final RestClient client;

    public RestClientOllamaChat(AssistProperties props) {
        String baseUrl = props.ollama() == null || props.ollama().baseUrl() == null
                ? "http://localhost:11434"
                : props.ollama().baseUrl();
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Reply chat(ChatCall call) {
        Map<String, Object> body = Map.of(
                "model", call.model(),
                "stream", false,
                "messages", call.messages().stream()
                        .map(message -> Map.of("role", message.role(), "content", message.content()))
                        .toList(),
                "tools", toolSchemas(call.tools()),
                "options", call.numCtx() == null ? Map.of() : Map.of("num_ctx", call.numCtx()));

        OllamaResponse response = client.post().uri("/api/chat").body(body).retrieve().body(OllamaResponse.class);
        if (response == null || response.message() == null) {
            return new Reply("", List.of());
        }
        List<ToolCall> calls = new ArrayList<>();
        if (response.message().tool_calls() != null) {
            for (OllamaToolCall tc : response.message().tool_calls()) {
                if (tc.function() != null) {
                    Map<String, Object> args = tc.function().arguments() == null ? Map.of() : tc.function().arguments();
                    calls.add(new ToolCall(tc.function().name(), args));
                }
            }
        }
        String content = response.message().content() == null ? "" : response.message().content();
        return new Reply(content, calls);
    }

    private static List<Map<String, Object>> toolSchemas(List<ToolSpec> tools) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ToolSpec tool : tools) {
            out.add(Map.of("type", "function", "function", Map.of(
                    "name", tool.name(),
                    "description", tool.description(),
                    "parameters", tool.parameters() == null ? Map.of("type", "object") : tool.parameters())));
        }
        return out;
    }

    record OllamaResponse(OllamaMessage message, boolean done) {
    }

    record OllamaMessage(String role, String content, List<OllamaToolCall> tool_calls) {
    }

    record OllamaToolCall(OllamaFunction function) {
    }

    record OllamaFunction(String name, Map<String, Object> arguments) {
    }
}
