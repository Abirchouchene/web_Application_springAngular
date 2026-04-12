package com.example.callcenter.Config;


import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!dev-local")
    public class KeyCloakConfig {

        @Value("${keycloak.auth-server-url}")
        private String serverUrl;
        @Value("${keycloak.realm}")
        private String realm;
        @Value("${keycloak.client-id}")
        private String clientId;
        @Value("${keycloak.client-secret}")
        private String clientSecret;

        /** Base URL without /realms/xxx – needed by the Admin Client */
        private String getBaseUrl() {
            int idx = serverUrl.indexOf("/realms");
            return idx > 0 ? serverUrl.substring(0, idx) : serverUrl;
        }

        @Bean
        public Keycloak keycloak() {
            return KeycloakBuilder.builder()
                    .serverUrl(getBaseUrl())
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();
        }
    }