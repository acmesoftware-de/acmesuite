package de.acmesoftware.acmesuite.assist;

/**
 * A user turn sent to the assistant. {@code context} carries what the user is currently looking at
 * (active module + sub-view, and an optional selected entity) so an agent can ground its answer.
 */
public record AssistRequest(String conversationId, String message, Context context) {

    public record Context(String module, String subView, String entityId) {
    }
}
