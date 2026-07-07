package de.acmesoftware.acmesuite.ai;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic placeholder implementation of the AI layer (active while
 * {@code acme.ai.provider=stub}, the default). Returns reproducible, fixed answers — so the
 * dependent processes/tests already run today, before Ollama/RAG is in place.
 */
@Component
@ConditionalOnProperty(prefix = "acme.ai", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubContractIntelligence implements ContractIntelligence {

    @Override
    public String summarize(String documentText) {
        int words = documentText == null ? 0 : documentText.trim().split("\\s+").length;
        return "[stub-summary] Document with ~" + words + " words. Real summary follows with Ollama/RAG.";
    }

    @Override
    public List<RiskFinding> assessRisks(String documentText) {
        return List.of(new RiskFinding(
                "(stub) no real clause analysis",
                Severity.LOW,
                "Placeholder — risk detection arrives with the real AI layer."));
    }

    @Override
    public String ask(String documentText, String question) {
        return "[stub-answer] Question recognized: \"" + question + "\". Real Q&A follows with Ollama/RAG.";
    }
}
