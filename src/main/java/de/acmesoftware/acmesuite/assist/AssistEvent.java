package de.acmesoftware.acmesuite.assist;

import java.util.List;

/**
 * An event streamed back to the client during a turn (over SSE). {@link #type()} is the SSE event
 * name. The sequence for a read-only turn is: zero or more {@link ToolCall}s, a run of
 * {@link Delta}s (the streamed answer), one terminal {@link Message}, then {@link Done}.
 */
public sealed interface AssistEvent {

    /** SSE event name. */
    String type();

    /** The assistant invoked a (read) tool. */
    record ToolCall(String tool, String detail) implements AssistEvent {
        @Override
        public String type() {
            return "tool";
        }
    }

    /** A chunk of the streamed answer. */
    record Delta(String text) implements AssistEvent {
        @Override
        public String type() {
            return "delta";
        }
    }

    /** The final assistant message, with the source records behind it. */
    record Message(String text, List<String> sources) implements AssistEvent {
        @Override
        public String type() {
            return "message";
        }
    }

    /** End of the turn. */
    record Done(String conversationId) implements AssistEvent {
        @Override
        public String type() {
            return "done";
        }
    }
}
