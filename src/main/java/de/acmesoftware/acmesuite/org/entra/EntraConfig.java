package de.acmesoftware.acmesuite.org.entra;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires up the Entra provisioning beans — only when {@code acme.entra.enabled=true}. Without the
 * flag, neither the client nor the provisioner nor the endpoint exists.
 */
@Configuration
@ConditionalOnProperty(name = "acme.entra.enabled", havingValue = "true")
class EntraConfig {

    @Bean
    EntraTokenProvider entraTokenProvider(EntraProperties props) {
        RestClient login = RestClient.builder().baseUrl("https://login.microsoftonline.com").build();
        return new EntraTokenProvider(login, props);
    }

    @Bean
    EntraGraphClient entraGraphClient(EntraTokenProvider tokenProvider) {
        RestClient graph = RestClient.builder().baseUrl("https://graph.microsoft.com/v1.0").build();
        return new EntraGraphClient(graph, tokenProvider);
    }
}
