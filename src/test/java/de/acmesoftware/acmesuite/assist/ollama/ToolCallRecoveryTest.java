package de.acmesoftware.acmesuite.assist.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit test of the M3.1 recovery of a tool call the model leaked as text content. */
class ToolCallRecoveryTest {

    private static final Set<String> KNOWN = Set.of("get_customer", "find_customers");

    @Test
    void recoversLeakedCallWithNoisePrefix() {
        assertThat(ToolCallRecovery.fromContent(
                "-toast {\"name\": \"get_customer\", \"arguments\": {\"id\": \"VELA-004\"}}", KNOWN))
                .singleElement()
                .satisfies(call -> {
                    assertThat(call.name()).isEqualTo("get_customer");
                    assertThat(call.arguments()).containsEntry("id", "VELA-004");
                });
    }

    @Test
    void unwrapsFunctionEnvelopeAndParametersKey() {
        assertThat(ToolCallRecovery.fromContent(
                "{\"function\": {\"name\": \"find_customers\", \"parameters\": {\"q\": \"Vela\"}}}", KNOWN))
                .singleElement()
                .satisfies(call -> {
                    assertThat(call.name()).isEqualTo("find_customers");
                    assertThat(call.arguments()).containsEntry("q", "Vela");
                });
    }

    @Test
    void ignoresUnknownToolsPlainProseAndBlanks() {
        assertThat(ToolCallRecovery.fromContent("{\"name\":\"drop_table\",\"arguments\":{}}", KNOWN)).isEmpty();
        assertThat(ToolCallRecovery.fromContent("Vela Robotics ist aktiv.", KNOWN)).isEmpty();
        assertThat(ToolCallRecovery.fromContent("", KNOWN)).isEmpty();
        assertThat(ToolCallRecovery.fromContent(null, KNOWN)).isEmpty();
    }
}
