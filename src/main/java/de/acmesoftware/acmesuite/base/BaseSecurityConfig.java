package de.acmesoftware.acmesuite.base;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security layer of <em>ACMEbase</em>. Config-toggled via {@link AuthProperties}:
 *
 * <ul>
 *   <li>{@code acme.base.auth.enabled=false} (default): all endpoints open — for local
 *       development; keeps the existing test suite green.</li>
 *   <li>{@code =true}: the suite APIs require a Base-issued session JWT (bearer). Reading (GET)
 *       needs {@link AccessRole#WATCH}, writing needs {@link AccessRole#WORK}; master-data
 *       endpoints are additionally restricted via {@code @PreAuthorize("hasRole('ADMIN')")}.</li>
 * </ul>
 *
 * <p>The token is minted by Base after a successful login (local or federated) and validated here
 * as a resource server — modules never see the external IdP. The {@code role} claim carries the
 * locally assigned {@link AccessRole}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class BaseSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties props) {
        return NimbusJwtDecoder.withSecretKey(secretKey(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKeySpec secretKey(AuthProperties props) {
        return new SecretKeySpec(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties props, JwtDecoder jwtDecoder)
            throws Exception {
        // Stateless API (bearer token, no browser session) -> CSRF off.
        http.csrf(csrf -> csrf.disable());

        if (props.isEnabled()) {
            http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/**").permitAll()
                            // Public login surface.
                            .requestMatchers(HttpMethod.POST, "/api/base/auth/login").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/base/auth/providers").permitAll()
                            // Bootstrap self-claim (only actually reachable when
                            // acme.base.auth.bootstrap.allow-self-claim=true; see AuthController).
                            .requestMatchers(HttpMethod.GET, "/api/base/auth/bootstrap-status").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/base/auth/claim-admin").permitAll()
                            // Public OIDC redirect flow (start + IdP callback).
                            .requestMatchers(HttpMethod.GET, "/api/base/auth/oidc/**").permitAll()
                            // Any authenticated user may read their profile / set their own password.
                            .requestMatchers("/api/base/auth/me", "/api/base/auth/password").authenticated()
                            // Version history is gated by the orthogonal AUDIT capability (ADR-0010),
                            // not by ADMIN — must precede the ADMIN user-management rule below.
                            .requestMatchers(HttpMethod.GET, "/api/base/users/*/history").hasRole("AUDIT")
                            // Admin surface: user/role management + provider configuration + reindex
                            // + DB schema browser.
                            .requestMatchers("/api/base/users/**",
                                    "/api/base/auth/provider-configs/**",
                                    "/api/base/db/**",
                                    "/api/search/reindex").hasRole(AccessRole.ADMIN.name())
                            // ACMEassist: any authenticated user may converse (WATCH+); write
                            // safety is enforced at the tool layer, not here (ADR-0008).
                            .requestMatchers(HttpMethod.POST, "/api/base/assist/messages")
                                .hasAnyRole(AccessRole.WATCH.name(), AccessRole.WORK.name(),
                                        AccessRole.ADMIN.name())
                            // Suite APIs: read = WATCH+, write = WORK+ (ADMIN adds master-data guards).
                            .requestMatchers(HttpMethod.GET, "/api/**")
                                .hasAnyRole(AccessRole.WATCH.name(), AccessRole.WORK.name(),
                                        AccessRole.ADMIN.name())
                            .requestMatchers("/api/**")
                                .hasAnyRole(AccessRole.WORK.name(), AccessRole.ADMIN.name())
                            .anyRequest().permitAll())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(roleConverter())));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    /**
     * Maps the Base token to authorities: the {@code role} claim to a single {@code ROLE_*}
     * access authority, plus {@code ROLE_AUDIT} when the orthogonal {@code audit} claim is set
     * (may view version history; ADR-0010).
     */
    private static JwtAuthenticationConverter roleConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            String role = jwt.getClaimAsString("role");
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            if (Boolean.TRUE.equals(jwt.getClaimAsBoolean("audit"))) {
                authorities.add(new SimpleGrantedAuthority("ROLE_AUDIT"));
            }
            return authorities;
        });
        return converter;
    }

    /**
     * Role hierarchy {@code ADMIN > WORK > WATCH}. Used by method security ({@code @PreAuthorize})
     * so master-data guards on {@code hasRole('ADMIN')} automatically include higher roles.
     */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(AccessRole.ADMIN.name()).implies(AccessRole.WORK.name())
                .role(AccessRole.WORK.name()).implies(AccessRole.WATCH.name())
                .build();
    }
}
