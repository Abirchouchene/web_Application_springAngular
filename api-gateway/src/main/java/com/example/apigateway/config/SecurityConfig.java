package com.example.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Profile("!dev-local")
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {}) // active le CORS défini dans application.properties
                .authorizeExchange(exchanges -> exchanges
                        // Preflight CORS (OPTIONS) toujours autorisé
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints publics : auth (login/register/token), actuator
                        .pathMatchers(
                                "/api/auth/**",
                                "/actuator/**",
                                "/api/requests/**",
                                "/api/response/**",
                                "/api/reports/**",
                                "/api/notifications/**",
                                "/api/logs/**",
                                "/api/request-contact-status/**",
                                "/api/files/**",
                                "/api/questions/**",
                                "/api/questions",
                                "/api/contacts/**",
                                "/api/contacts",
                                "/api/callbacks/**",
                                "/api/admin/**",
                                "/api/dashboard/**",
                                "/api/user/**",
                                "/ws/**",
                                "/api/ai-chat/**"
                        ).permitAll()
                        // Tous les autres endpoints : authentifié
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
                )
                .build();
    }
}
