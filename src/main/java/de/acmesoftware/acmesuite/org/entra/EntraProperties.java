package de.acmesoftware.acmesuite.org.entra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the HR-&gt;Entra provisioning ({@code acme.entra.*}, ADR-0005).
 *
 * <p>{@code clientSecret} is supplied via relaxed binding from the ENV variable
 * {@code ACME_ENTRA_CLIENT_SECRET} (acme-group keeps secrets in the environment, not in the repo).
 * The default is {@code enabled=false} and {@code dryRun=true} — nothing happens without explicitly
 * enabling it, and even when enabled the run is initially only planned (no writes to Entra).
 *
 * @param enabled      provisioning beans active? (otherwise no client/endpoint)
 * @param tenantId     Entra Directory (tenant) ID
 * @param clientId     app registration (Application/client ID)
 * @param clientSecret client secret (ENV {@code ACME_ENTRA_CLIENT_SECRET})
 * @param domain       verified UPN domain (e.g. {@code acme-group.io})
 * @param dryRun       only plan/log instead of writing to Entra
 */
@ConfigurationProperties("acme.entra")
public record EntraProperties(
        @DefaultValue("false") boolean enabled,
        String tenantId,
        String clientId,
        String clientSecret,
        String domain,
        @DefaultValue("true") boolean dryRun) {
}
