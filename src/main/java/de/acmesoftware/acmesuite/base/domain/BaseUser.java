package de.acmesoftware.acmesuite.base.domain;

import de.acmesoftware.acmesuite.base.AccessRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An ACMEbase user account: an identity that can access the suite APIs, plus the locally
 * assigned access {@link AccessRole}.
 *
 * <p>Identity is either <b>local</b> ({@code authProvider = "local"}, {@link #passwordHash} set)
 * or <b>federated</b> (a configured provider id, {@link #externalSubject} set). The role is always
 * local — never taken from an IdP claim. Deliberately separate from {@code org.person} (business
 * employees): this is solely about API access.
 */
@Entity
@Table(name = "base_user")
public class BaseUser {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    /** Local login name; {@code null} for purely federated users. */
    @Column(name = "username", length = 160)
    private String username;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private AccessRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserStatus status;

    /** {@code "local"} or a configured provider id. */
    @Column(name = "auth_provider", nullable = false, length = 64)
    private String authProvider;

    /** Stable external subject (oid/sub) for federated users; {@code null} for local. */
    @Column(name = "external_subject", length = 200)
    private String externalSubject;

    /** BCrypt hash for local users; {@code null} for federated. */
    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Column(name = "must_set_password", nullable = false)
    private boolean mustSetPassword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BaseUser() {
        // JPA
    }

    public BaseUser(String id, String username, String email, String displayName, AccessRole role,
            UserStatus status, String authProvider, String externalSubject, String passwordHash,
            boolean mustSetPassword, Instant now) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.authProvider = authProvider;
        this.externalSubject = externalSubject;
        this.passwordHash = passwordHash;
        this.mustSetPassword = mustSetPassword;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AccessRole getRole() {
        return role;
    }

    public void setRole(AccessRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMustSetPassword() {
        return mustSetPassword;
    }

    public void setMustSetPassword(boolean mustSetPassword) {
        this.mustSetPassword = mustSetPassword;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
