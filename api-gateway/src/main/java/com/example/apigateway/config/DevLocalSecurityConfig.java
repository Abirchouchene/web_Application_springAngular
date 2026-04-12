package com.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Profil dev-local : DESACTIVE toute authentification JWT.
 * Utilisé quand le VPN/Keycloak est inaccessible.
 * Lancer avec : -Dspring-boot.run.profiles=dev-local
 */
@Configuration
@EnableWebFluxSecurity
@Profile("dev-local")
public class DevLocalSecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> {})
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                // Pas de oauth2ResourceServer -> pas de validation JWT
                .build();
    }
}
