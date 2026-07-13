package de.acmesoftware.acmesuite.assist.springai;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.AssistProperties;
import de.acmesoftware.acmesuite.assist.AssistRequest;
import de.acmesoftware.acmesuite.assist.AssistantEngine;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.agent.Customer360Agent;
import de.acmesoftware.acmesuite.assist.tools.AssistTool;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * The engine on Spring AI 2.0 (ADR-0008): a read-only agent whose Customer-360 tools run <em>as the
 * user</em> via {@link AuthenticatedApiDispatcher}. Spring AI's {@link ChatClient} drives the
 * tool-calling loop and streaming natively — no bespoke ReAct loop and no leaked-tool-call guard.
 * Active whenever {@code acme.assist.provider} is not {@code stub}; the provider selects the
 * ChatModel bean (Ollama the self-hosted default; Anthropic/OpenAI opt-in by config + API key).
 */
@Component
@ConditionalOnExpression("'${acme.assist.provider:stub}' != 'stub'")
public class SpringAiAssistantEngine implements AssistantEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiAssistantEngine.class);

    private final ChatClient chatClient;
    private final List<ToolCallback> tools;

    public SpringAiAssistantEngine(Map<String, ChatModel> chatModels,
            AuthenticatedApiDispatcher dispatcher, AssistProperties props) {
        this.chatClient = ChatClient.create(selectModel(chatModels, props.providerOrDefault()));
        this.tools = new ArrayList<>();
        for (AssistTool tool : Customer360Agent.TOOLS) {
            this.tools.add(new DispatchingToolCallback(tool, dispatcher));
        }
    }

    private static ChatModel selectModel(Map<String, ChatModel> chatModels, String provider) {
        String beanName = switch (provider) {
            case "ollama" -> "ollamaChatModel";
            case "claude", "anthropic" -> "anthropicChatModel";
            case "openai" -> "openAiChatModel";
            case "google", "gemini" -> "googleGenAiChatModel";
            default -> throw new IllegalStateException("unknown acme.assist.provider: " + provider);
        };
        ChatModel model = chatModels.get(beanName);
        if (model == null) {
            throw new IllegalStateException("no ChatModel '" + beanName + "' for provider '"
                    + provider + "' — is the matching spring-ai starter present?");
        }
        return model;
    }

    @Override
    public void converse(AssistRequest request, CallerContext caller, Consumer<AssistEvent> sink) {
        List<String> sources = new ArrayList<>();
        Map<String, Object> toolContext = new HashMap<>();
        toolContext.put("caller", caller);
        toolContext.put("sink", sink);
        toolContext.put("sources", sources);

        StringBuilder answer = new StringBuilder();
        try {
            chatClient.prompt()
                    .system(Customer360Agent.SYSTEM_PROMPT)
                    .user(request.message())
                    .toolCallbacks(tools)
                    .toolContext(toolContext)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        answer.append(chunk);
                        sink.accept(new AssistEvent.Delta(chunk));
                    })
                    .blockLast();
            sink.accept(new AssistEvent.Message(answer.toString(), List.copyOf(sources)));
        } catch (RuntimeException e) {
            // A provider/tool failure becomes a graceful error event + done, not an HTTP 500.
            LOG.warn("ACMEassist turn failed", e);
            sink.accept(new AssistEvent.Error(
                    "Der KI-Assistent ist derzeit nicht verfügbar. Bitte später erneut versuchen."));
        }
        sink.accept(new AssistEvent.Done(
                request.conversationId() == null ? "conv" : request.conversationId()));
    }
}
