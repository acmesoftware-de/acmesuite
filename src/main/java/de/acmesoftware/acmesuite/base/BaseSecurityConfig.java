package de.acmesoftware.acmesuite.base;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security layer of <em>ACMEbase</em>. Config-toggled via {@link AuthProperties}:
 *
 * <ul>
 *   <li>{@code acme.base.auth.enabled=false} (default): all endpoints open — for local
 *       development; keeps the existing test suite green.</li>
 *   <li>{@code =true}: role rules apply via HTTP Basic. Reading (GET) from {@link AccessRole#WATCH},
 *       writing from {@link AccessRole#WORK}. Master-data endpoints are additionally
 *       restricted fine-grained via {@code @PreAuthorize(\"hasRole('ADMIN')\")} (per controller,
 *       when the respective module is touched).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class BaseSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties props) throws Exception {
        // Stateless API (no browser form login) -> CSRF off; protection comes from roles.
        http.csrf(csrf -> csrf.disable());

        if (props.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/**")
                                .hasAnyRole(AccessRole.WATCH.name(), AccessRole.WORK.name(),
                                        AccessRole.ADMIN.name())
                            .requestMatchers("/api/**")
                                .hasAnyRole(AccessRole.WORK.name(), AccessRole.ADMIN.name())
                            .anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    /**
     * Role hierarchy {@code ADMIN > WORK > WATCH}. Used by method security
     * ({@code @PreAuthorize}) so that master-data guards on {@code hasRole('ADMIN')}
     * automatically include higher roles.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(AccessRole.ADMIN.name()).implies(AccessRole.WORK.name())
                .role(AccessRole.WORK.name()).implies(AccessRole.WATCH.name())
                .build();
    }

    /**
     * Demo accounts for the enabled mode. Not for production use — hence
     * {@code {noop}} passwords; a real UserDetails store or OIDC connection will follow.
     */
    @Bean
    UserDetailsService acmesuiteUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("watch").password("{noop}watch").roles(AccessRole.WATCH.name()).build(),
                User.withUsername("work").password("{noop}work").roles(AccessRole.WORK.name()).build(),
                User.withUsername("admin").password("{noop}admin").roles(AccessRole.ADMIN.name()).build());
    }
}
