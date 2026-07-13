package de.acmesoftware.acmesuite.assist.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One append-only audit row per ACMEassist turn (ADR-0008 governance G2). Records who asked, the
 * provider/model, the tools invoked and the outcome. Tamper-evidence: each row's {@link #hash}
 * chains the previous row's hash, so a deleted/edited row breaks the chain.
 */
@Entity
@Table(name = "assist_audit")
public class AssistAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "user_role", length = 16)
    private String userRole;

    @Column(name = "agent", length = 64)
    private String agent;

    @Column(name = "provider", length = 32)
    private String provider;

    @Column(name = "model", length = 96)
    private String model;

    @Column(name = "tools")
    private String tools;

    @Column(name = "outcome", nullable = false, length = 16)
    private String outcome;

    @Column(name = "prompt_version", length = 32)
    private String promptVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    protected AssistAudit() {
    }

    AssistAudit(String conversationId, String userId, String userRole, String agent, String provider,
            String model, String tools, String outcome, String promptVersion, Instant createdAt,
            String prevHash, String hash) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.userRole = userRole;
        this.agent = agent;
        this.provider = provider;
        this.model = model;
        this.tools = tools;
        this.outcome = outcome;
        this.promptVersion = promptVersion;
        this.createdAt = createdAt;
        this.prevHash = prevHash;
        this.hash = hash;
    }

    public Long getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getAgent() {
        return agent;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getTools() {
        return tools;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getHash() {
        return hash;
    }
}
