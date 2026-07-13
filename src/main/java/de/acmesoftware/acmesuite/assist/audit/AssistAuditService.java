package de.acmesoftware.acmesuite.assist.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Appends one hash-chained audit row per turn (ADR-0008 G2). {@code synchronized} serializes the
 * read-previous → compute-hash → save so the chain stays consistent on a single instance (a
 * distributed deployment would need a DB-side lock/sequence — follow-up).
 */
@Service
public class AssistAuditService {

    private static final String PROMPT_VERSION = "customer-360/v1";

    private final AssistAuditRepository repository;
    private final Environment env;

    public AssistAuditService(AssistAuditRepository repository, Environment env) {
        this.repository = repository;
        this.env = env;
    }

    public synchronized void record(String conversationId, String userId, String userRole,
            String agent, String provider, List<String> tools, boolean failed) {
        String prevHash = repository.findTopByOrderByIdDesc().map(AssistAudit::getHash).orElse("");
        String model = modelFor(provider);
        String toolsCsv = String.join(",", tools);
        String outcome = failed ? "error" : "ok";
        Instant now = Instant.now();

        String canonical = String.join("|", nz(conversationId), nz(userId), nz(userRole), nz(agent),
                nz(provider), nz(model), toolsCsv, outcome, PROMPT_VERSION, now.toString());
        String hash = sha256(prevHash + "\n" + canonical);

        repository.save(new AssistAudit(conversationId, userId, userRole, agent, provider, model,
                toolsCsv, outcome, PROMPT_VERSION, now, prevHash, hash));
    }

    /** Best-effort resolution of the active model id for G2 (from the Spring AI config). */
    private String modelFor(String provider) {
        return switch (provider) {
            case "stub" -> "stub";
            case "ollama" -> env.getProperty("spring.ai.ollama.chat.options.model", "ollama");
            case "openai" -> env.getProperty("spring.ai.openai.chat.options.model", "openai");
            case "claude", "anthropic" -> env.getProperty("spring.ai.anthropic.chat.options.model", "anthropic");
            case "google", "gemini" -> env.getProperty("spring.ai.google.genai.chat.options.model", "google");
            default -> provider;
        };
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
