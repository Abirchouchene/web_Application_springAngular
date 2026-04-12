package com.example.apigateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Extrait les rôles Keycloak depuis realm_access.roles
 * et les mappe en ROLE_xxx pour Spring Security.
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultConverter.convert(jwt).stream(),
                extractRealmRoles(jwt).stream()
        ).collect(Collectors.toSet());

        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("sub")));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return List.of();
        return roles.stream()
                .map(this::normalizeRoleName)
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    /**
     * Normalize Keycloak realm role names to app Role enum names.
     * Maps: admin→ADMIN, manager→MANAGER, agent→AGENT, demandeur→SURVEY_REQUESTER
     */
    private String normalizeRoleName(String kcRole) {
        if (kcRole == null) return null;
        switch (kcRole.toLowerCase()) {
            case "admin": return "ADMIN";
            case "manager": return "MANAGER";
            case "agent": return "AGENT";
            case "demandeur":
            case "survey_requester": return "SURVEY_REQUESTER";
            default: return kcRole;
        }
    }
}
