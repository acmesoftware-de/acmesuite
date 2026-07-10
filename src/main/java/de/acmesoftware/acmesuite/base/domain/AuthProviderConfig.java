package de.acmesoftware.acmesuite.base.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Admin-managed configuration of a federated auth provider (Entra, generic OIDC). Non-secret
 * fields are serialized to {@link #configJson}; secret fields (client secrets) to
 * {@link #secretsJson} envelope-encrypted. Exactly one config per provider id.
 */
@Entity
@Table(name = "auth_provider_config")
public class AuthProviderConfig {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "display_name", length = 160)
    private String displayName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** JSON object of non-secret field values. */
    @Column(name = "config_json")
    private String configJson;

    /** JSON object of secret field values, each envelope-encrypted. */
    @Column(name = "secrets_json")
    private String secretsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    protected AuthProviderConfig() {
        // JPA
    }

    public AuthProviderConfig(String id, String providerId, Instant now) {
        this.id = id;
        this.providerId = providerId;
        this.enabled = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getSecretsJson() {
        return secretsJson;
    }

    public void setSecretsJson(String secretsJson) {
        this.secretsJson = secretsJson;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
