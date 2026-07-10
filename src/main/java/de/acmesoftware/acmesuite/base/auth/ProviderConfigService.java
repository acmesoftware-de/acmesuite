package de.acmesoftware.acmesuite.base.auth;

import de.acmesoftware.acmesuite.base.crypto.SecretCipher;
import de.acmesoftware.acmesuite.base.domain.AuthProviderConfig;
import de.acmesoftware.acmesuite.base.domain.AuthProviderConfigRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admin management of federated auth-provider configuration. Only providers that declare a config
 * schema (Entra, generic OIDC) are configurable. Secret fields are stored envelope-encrypted and
 * never returned in clear — the UI only learns which secrets are set, not their values.
 */
@Service
public class ProviderConfigService {

    private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private final List<AuthProvider> providers;
    private final AuthProviderConfigRepository repo;
    private final SecretCipher cipher;
    private final ObjectMapper json = JsonMapper.builder().build();

    public ProviderConfigService(List<AuthProvider> providers, AuthProviderConfigRepository repo,
            SecretCipher cipher) {
        this.providers = providers;
        this.repo = repo;
        this.cipher = cipher;
    }

    /** Providers with a config schema (i.e. the ones an admin configures). */
    private List<AuthProvider> configurable() {
        return providers.stream().filter(p -> !p.configSchema().isEmpty()).toList();
    }

    private Optional<AuthProvider> configurable(String providerId) {
        return configurable().stream().filter(p -> p.id().equals(providerId)).findFirst();
    }

    public List<State> list() {
        return configurable().stream()
                .map(p -> toState(p, repo.findByProviderId(p.id()).orElse(null)))
                .toList();
    }

    @Transactional
    public State upsert(String providerId, boolean enabled, Map<String, String> incoming, String actor) {
        AuthProvider provider = configurable(providerId)
                .orElseThrow(() -> new IllegalArgumentException("unknown provider: " + providerId));
        Instant now = Instant.now();
        AuthProviderConfig cfg = repo.findByProviderId(providerId)
                .orElseGet(() -> new AuthProviderConfig(newId(), providerId, now));

        Map<String, String> nonSecret = readMap(cfg.getConfigJson());
        Map<String, String> secrets = readMap(cfg.getSecretsJson());
        Set<String> secretKeys = provider.configSchema().stream()
                .filter(f -> f.type() == ConfigField.Type.SECRET)
                .map(ConfigField::key)
                .collect(Collectors.toSet());

        if (incoming != null) {
            incoming.forEach((key, value) -> {
                if (secretKeys.contains(key)) {
                    // Blank secret = keep the stored one (the UI never echoes secrets back).
                    if (value != null && !value.isBlank()) {
                        secrets.put(key, cipher.encrypt(value));
                    }
                } else if (value == null || value.isBlank()) {
                    nonSecret.remove(key);
                } else {
                    nonSecret.put(key, value);
                }
            });
        }

        cfg.setDisplayName(provider.displayName());
        cfg.setEnabled(enabled);
        cfg.setConfigJson(writeMap(nonSecret));
        cfg.setSecretsJson(writeMap(secrets));
        cfg.setUpdatedBy(actor);
        cfg.touch(now);
        repo.save(cfg);
        return toState(provider, cfg);
    }

    @Transactional
    public void delete(String providerId) {
        repo.findByProviderId(providerId).ifPresent(repo::delete);
    }

    /** Provider ids with an enabled config (used to show federated login options). */
    public Set<String> enabledProviderIds() {
        return repo.findAll().stream()
                .filter(AuthProviderConfig::isEnabled)
                .map(AuthProviderConfig::getProviderId)
                .collect(Collectors.toSet());
    }

    /** Full decrypted config for an enabled provider — for the login flow (wired later). */
    public Optional<Map<String, String>> resolvedConfig(String providerId) {
        return repo.findByProviderId(providerId)
                .filter(AuthProviderConfig::isEnabled)
                .map(cfg -> {
                    Map<String, String> all = readMap(cfg.getConfigJson());
                    readMap(cfg.getSecretsJson()).forEach((k, v) -> all.put(k, cipher.decrypt(v)));
                    return all;
                });
    }

    private State toState(AuthProvider provider, AuthProviderConfig cfg) {
        Map<String, String> values = cfg == null ? Map.of() : readMap(cfg.getConfigJson());
        Set<String> secretsSet = cfg == null ? Set.of() : readMap(cfg.getSecretsJson()).keySet();
        boolean enabled = cfg != null && cfg.isEnabled();
        return new State(provider.id(), provider.displayName(), provider.kind().name(), enabled,
                cfg != null, provider.configSchema(), values, secretsSet);
    }

    private Map<String, String> readMap(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return new LinkedHashMap<>();
        }
        return json.readValue(jsonText, MAP_TYPE);
    }

    private String writeMap(Map<String, String> map) {
        return json.writeValueAsString(map);
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Read model of a provider's configuration (never carries secret values). */
    public record State(String providerId, String displayName, String kind, boolean enabled,
            boolean configured, List<ConfigField> schema, Map<String, String> values,
            Set<String> secretsSet) {
    }
}
