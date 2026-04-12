package com.example.callcenter.Config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Feign interceptor : transmet le JWT entrant ou récupère un token client_credentials.
 * Utilisé par ContactClient pour appeler contact-service.
 */
@Configuration
public class ContactClientConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Bean
    public RequestInterceptor feignClientInterceptor() {
        return (RequestTemplate requestTemplate) -> {
            // 1) Essayer de récupérer le token JWT de la requête entrante
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authHeader);
                    return;
                }
            }

            // 2) Fallback : obtenir un token via client_credentials
            try {
                String token = getClientCredentialsToken();
                requestTemplate.header("Authorization", "Bearer " + token);
            } catch (Exception e) {
                System.err.println("Feign: impossible d'obtenir le token Keycloak : " + e.getMessage());
            }
        };
    }

    @SuppressWarnings("unchecked")
    private String getClientCredentialsToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = keycloakUrl + "/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, headers), Map.class);

        return (String) response.getBody().get("access_token");
    }
}