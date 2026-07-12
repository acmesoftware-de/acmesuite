package de.acmesoftware.acmesuite.assist.ollama;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.AssistProperties;
import de.acmesoftware.acmesuite.assist.AssistRequest;
import de.acmesoftware.acmesuite.assist.AssistantEngine;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.agent.Customer360Agent;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ChatCall;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ChatMessage;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.Reply;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ToolCall;
import de.acmesoftware.acmesuite.assist.ollama.OllamaChatClient.ToolSpec;
import de.acmesoftware.acmesuite.assist.tools.AssistTool;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * The {@code provider=ollama} engine (ADR-0008): a read-only ReAct loop over the Customer-360
 * tools, each executed <em>as the user</em> via {@link AuthenticatedApiDispatcher}. No write tools
 * are registered — this persona cannot mutate. Interim implementation over {@link OllamaChatClient}
 * (direct Ollama); a drop-in swap to Spring AI + langgraph4j once those support Spring Boot 4.1.
 */
@Component
@ConditionalOnProperty(prefix = "acme.assist", name = "provider", havingValue = "ollama")
public class OllamaAssistantEngine implements AssistantEngine {

    private final OllamaChatClient chat;
    private final AuthenticatedApiDispatcher dispatcher;
    private final AssistProperties props;

    public OllamaAssistantEngine(OllamaChatClient chat, AuthenticatedApiDispatcher dispatcher,
            AssistProperties props) {
        this.chat = chat;
        this.dispatcher = dispatcher;
        this.props = props;
    }

    @Override
    public void converse(AssistRequest request, CallerContext caller, Consumer<AssistEvent> sink) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(Customer360Agent.SYSTEM_PROMPT));
        messages.add(ChatMessage.user(request.message()));

        List<ToolSpec> tools = toolSpecs();
        List<String> sources = new ArrayList<>();
        int maxIterations = maxIterations();

        for (int i = 0; i < maxIterations; i++) {
            Reply reply = chat.chat(new ChatCall(model(), messages, tools, numCtx()));
            if (!reply.hasToolCalls()) {
                stream(reply.content(), sink);
                sink.accept(new AssistEvent.Message(reply.content(), List.copyOf(sources)));
                sink.accept(new AssistEvent.Done(conversationId(request)));
                return;
            }
            for (ToolCall call : reply.toolCalls()) {
                sink.accept(new AssistEvent.ToolCall(call.name(), String.valueOf(call.arguments())));
                String path = pathFor(call);
                AuthenticatedApiDispatcher.Result result = dispatcher.get(caller, path);
                sources.add(call.name() + " " + path + " -> " + result.status());
                messages.add(ChatMessage.assistant("Tool " + call.name() + " aufgerufen."));
                messages.add(ChatMessage.tool(result.body() == null ? "" : result.body()));
            }
        }
        sink.accept(new AssistEvent.Message(
                "(Keine abschließende Antwort nach " + maxIterations + " Schritten.)", List.copyOf(sources)));
        sink.accept(new AssistEvent.Done(conversationId(request)));
    }

    private static void stream(String content, Consumer<AssistEvent> sink) {
        if (content == null || content.isBlank()) {
            return;
        }
        for (String word : content.split(" ")) {
            sink.accept(new AssistEvent.Delta(word + " "));
        }
    }

    /** Map a model tool call to its GET path (phase-1 read tools). No write path exists. */
    private String pathFor(ToolCall call) {
        AssistTool tool = Customer360Agent.TOOLS.stream()
                .filter(candidate -> candidate.name().equals(call.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown tool: " + call.name()));
        Map<String, Object> args = call.arguments() == null ? Map.of() : call.arguments();
        return switch (tool.name()) {
            case "get_customer" -> tool.resolve("id", String.valueOf(args.getOrDefault("id", "")));
            case "find_customers" -> tool.pathTemplate()
                    + (args.get("q") == null ? "" : "?q=" + args.get("q"));
            case "resolve_price" -> tool.pathTemplate()
                    + "?customerId=" + args.getOrDefault("customerId", "")
                    + "&productId=" + args.getOrDefault("productId", "")
                    + "&quantity=" + args.getOrDefault("quantity", "1");
            default -> tool.pathTemplate();
        };
    }

    private List<ToolSpec> toolSpecs() {
        List<ToolSpec> specs = new ArrayList<>();
        for (AssistTool tool : Customer360Agent.TOOLS) {
            specs.add(new ToolSpec(tool.name(), tool.description(), Map.of("type", "object")));
        }
        return specs;
    }

    private String model() {
        AssistProperties.Ollama ollama = props.ollama();
        return ollama != null && ollama.modelMain() != null ? ollama.modelMain() : "qwen2.5:7b";
    }

    private Integer numCtx() {
        return props.ollama() == null ? null : props.ollama().numCtx();
    }

    private int maxIterations() {
        AssistProperties.Budget budget = props.budget();
        return budget == null || budget.maxToolIterations() == null ? 5 : budget.maxToolIterations();
    }

    private static String conversationId(AssistRequest request) {
        return request.conversationId() == null ? "conv" : request.conversationId();
    }
}
