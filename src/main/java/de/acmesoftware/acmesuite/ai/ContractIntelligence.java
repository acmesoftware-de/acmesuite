package de.acmesoftware.acmesuite.ai;

import java.util.List;

/**
 * AI layer for the contract domain: analysis, risk detection, summarization, Q&A and
 * form pre-fill. Deliberately modeled as a port so that a later Ollama/RAG integration can
 * replace the stubbed implementation ({@link StubContractIntelligence}) without changes to the caller.
 */
public interface ContractIntelligence {

    /** Concise summary of a contract/document text. */
    String summarize(String documentText);

    /** Risk detection: returns detected clause risks with severity. */
    List<RiskFinding> assessRisks(String documentText);

    /** Q&A over a document text (RAG-backed later). */
    String ask(String documentText, String question);

    enum Severity {LOW, MEDIUM, HIGH}

    record RiskFinding(String clause, Severity severity, String rationale) {
    }
}
