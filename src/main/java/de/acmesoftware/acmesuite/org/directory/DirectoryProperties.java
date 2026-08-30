package de.acmesoftware.acmesuite.org.directory;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of directory provisioning ({@code acme.directory.*}, ADR-0011 §8).
 *
 * <p>Inert without configuration: {@code enabled=false} by default, so no client, no orchestration
 * and no endpoint exist. {@code clientSecret} arrives via relaxed binding from the environment
 * ({@code ACME_DIRECTORY_CLIENT_SECRET}) or a secret store — never the repository, which is public.
 * Even when enabled, {@code dryRun=true} keeps the first run a plan.
 *
 * <p>{@code groupMappings} is customer vocabulary (ADR-0011 §5): local role or power-of-attorney key
 * to the directory group's name. EMPTY BY DEFAULT — and an empty mapping means no group is touched
 * at all, so a fresh installation cannot rewrite a customer's group memberships by accident.
 *
 * @param enabled       provisioning beans active?
 * @param tenantId      directory (tenant) id
 * @param clientId      app registration (application/client id)
 * @param clientSecret  client secret (ENV {@code ACME_DIRECTORY_CLIENT_SECRET})
 * @param domain        verified login-name domain (e.g. {@code acme-group.io})
 * @param dryRun        only plan/log instead of writing
 * @param groupMappings local-key to directory-group-name; empty means groups are left untouched
 */
@ConfigurationProperties("acme.directory")
public record DirectoryProperties(
        @DefaultValue("false") boolean enabled,
        String tenantId,
        String clientId,
        String clientSecret,
        String domain,
        @DefaultValue("true") boolean dryRun,
        Map<String, String> groupMappings) {

    // A Map cannot carry @DefaultValue (its empty default is a String the binder cannot convert).
    // Null-coalesce here instead, so an omitted mapping is an empty one -- and an empty mapping
    // means no group is ever touched (ADR-0011 §5).
    public DirectoryProperties {
        groupMappings = groupMappings == null ? Map.of() : groupMappings;
    }
}
