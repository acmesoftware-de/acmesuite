package de.acmesoftware.acmesuite.assist.springai;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.tools.AssistTool;
import de.acmesoftware.acmesuite.assist.tools.AuthenticatedApiDispatcher;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

/**
 * A Spring AI {@link ToolCallback} that runs a Customer-360 read tool <em>as the signed-in user</em>
 * via {@link AuthenticatedApiDispatcher}. The per-request {@link CallerContext}, the event sink and
 * the source list travel in the Spring AI {@link ToolContext} (set on the ChatClient call), so the
 * execute-as-the-user guarantee (ADR-0008 Decision 2) survives Spring AI's native tool loop.
 */
class DispatchingToolCallback implements ToolCallback {

    private static final JsonParser JSON = JsonParserFactory.getJsonParser();

    private final AssistTool tool;
    private final AuthenticatedApiDispatcher dispatcher;

    DispatchingToolCallback(AssistTool tool, AuthenticatedApiDispatcher dispatcher) {
        this.tool = tool;
        this.dispatcher = dispatcher;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(tool.inputSchema())
                .build();
    }

    @Override
    public String call(String toolInput) {
        return execute(toolInput, null, null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String call(String toolInput, ToolContext toolContext) {
        Map<String, Object> ctx = toolContext == null ? Map.of() : toolContext.getContext();
        return execute(toolInput,
                (CallerContext) ctx.get("caller"),
                (Consumer<AssistEvent>) ctx.get("sink"),
                (List<String>) ctx.get("sources"));
    }

    private String execute(String toolInput, CallerContext caller, Consumer<AssistEvent> sink,
            List<String> sources) {
        if (caller == null) {
            return "";
        }
        Map<String, Object> args = toolInput == null || toolInput.isBlank()
                ? Map.of() : JSON.parseMap(toolInput);
        String path = pathFor(tool, args);
        if (sink != null) {
            sink.accept(new AssistEvent.ToolCall(tool.name(), String.valueOf(args)));
        }
        AuthenticatedApiDispatcher.Result result = dispatcher.get(caller, path);
        if (sources != null) {
            sources.add(tool.name() + " " + path + " -> " + result.status());
        }
        return result.body() == null ? "" : result.body();
    }

    private static String pathFor(AssistTool tool, Map<String, Object> args) {
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
}
