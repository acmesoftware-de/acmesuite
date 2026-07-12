package de.acmesoftware.acmesuite.base.domain;

import de.acmesoftware.acmesuite.shared.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;

/**
 * Admin-managed configuration of a federated auth provider (Entra, generic OIDC). Non-secret
 * fields are serialized to {@link #configJson}; secret fields (client secrets) to
 * {@link #secretsJson} envelope-encrypted. Exactly one live config per provider id.
 *
 * <p>Versioned + tombstoned (ADR-0010): audit stamps and history come from {@link AuditedEntity};
 * {@code @SQLRestriction} hides tombstoned rows from ordinary reads; {@code @Audited} keeps history.
 */
@Entity
@Audited
@Table(name = "auth_provider_config")
@SQLRestriction("deleted_at is null")
public class AuthProviderConfig extends AuditedEntity {

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

    protected AuthProviderConfig() {
        // JPA
    }

    public AuthProviderConfig(String id, String providerId) {
        this.id = id;
        this.providerId = providerId;
        this.enabled = false;
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
}
