package de.acmesoftware.acmesuite.org.directory;

import de.acmesoftware.acmesuite.org.domain.PersonRepository;
import de.acmesoftware.acmesuite.org.domain.RoleAssignmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the directory-provisioning beans — only when {@code acme.directory.enabled=true}
 * (ADR-0011 §8). Without the flag there is no token source, no adapter, no service and no endpoint:
 * an installation that has not asked for a directory writer does not have one.
 */
@Configuration
@ConditionalOnProperty(name = "acme.directory.enabled", havingValue = "true")
class DirectoryConfig {

    @Bean
    GraphTokenProvider graphTokenProvider(DirectoryProperties props) {
        RestClient login = RestClient.builder().baseUrl("https://login.microsoftonline.com").build();
        return new GraphTokenProvider(login, props);
    }

    @Bean
    DirectoryProvisioner directoryProvisioner(GraphTokenProvider token) {
        RestClient graph = RestClient.builder().baseUrl("https://graph.microsoft.com/v1.0").build();
        return new GraphDirectoryProvisioner(graph, token::token);
    }

    @Bean
    DirectoryProvisioningService directoryProvisioningService(PersonRepository persons,
            RoleAssignmentRepository roleAssignments, DirectoryProvisioner directory,
            DirectoryProperties props) {
        return new DirectoryProvisioningService(persons, roleAssignments, directory, props);
    }
}
