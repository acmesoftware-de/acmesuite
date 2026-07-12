package de.acmesoftware.acmesuite.assist.ollama;

import java.util.List;
import java.util.Map;

/**
 * Minimal seam over Ollama's {@code /api/chat} (tool-calling). Kept as an interface so the engine
 * is unit-testable without a live model.
 *
 * <p><b>Interim (ADR-0008 phase-1 plan §0).</b> This talks to Ollama directly because Spring AI
 * does not yet target Spring Boot 4.1. The {@code AssistantEngine} port keeps the later swap to
 * Spring AI + langgraph4j a drop-in.
 */
public interface OllamaChatClient {

    Reply chat(ChatCall call);

    record ChatCall(String model, List<ChatMessage> messages, List<ToolSpec> tools, Integer numCtx) {
    }

    record ChatMessage(String role, String content) {

        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }

        public static ChatMessage tool(String content) {
            return new ChatMessage("tool", content);
        }
    }

    record ToolSpec(String name, String description, Map<String, Object> parameters) {
    }

    record ToolCall(String name, Map<String, Object> arguments) {
    }

    record Reply(String content, List<ToolCall> toolCalls) {

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
