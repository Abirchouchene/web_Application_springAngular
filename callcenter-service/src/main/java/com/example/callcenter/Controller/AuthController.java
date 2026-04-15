package com.example.callcenter.Controller;

import com.example.callcenter.DTO.UserInfoDTO;
import com.example.callcenter.Entity.Role;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.Service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        // Extract realm roles
        List<String> realmRoles = Collections.emptyList();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List) {
            realmRoles = (List<String>) realmAccess.get("roles");
        }

        // Determine primary app role from realm roles
        String appRole = determineAppRole(realmRoles);

        // Extract groups if present
        List<String> groups = jwt.getClaim("groups");
        if (groups == null) groups = Collections.emptyList();

        // Look up DB user to get the internal id
        String username = jwt.getClaim("preferred_username");
        String email = jwt.getClaim("email");
        String fullName = jwt.getClaim("name");
        User dbUser = userRepository.findByUsername(username).orElse(null);

        // If Keycloak roles didn't resolve properly, use DB role as fallback
        if ("AGENT".equals(appRole) && dbUser != null && dbUser.getRole() != null) {
            appRole = dbUser.getRole().name();
        }

        // Auto-create DB user if not found (syncs from Keycloak on first login)
        if (dbUser == null) {
            try {
                dbUser = new User();
                dbUser.setUsername(username);
                dbUser.setEmail(email != null ? email : "");
                dbUser.setFullName(fullName != null ? fullName : username);
                dbUser.setEnabled(true);
                try {
                    dbUser.setRole(Role.valueOf(appRole));
                } catch (IllegalArgumentException e) {
                    dbUser.setRole(Role.AGENT);
                }
                dbUser = userRepository.save(dbUser);
                log.info("Auto-created DB user: {} with role {}", username, appRole);
            } catch (Exception e) {
                log.warn("Failed to auto-create user {}: {}", username, e.getMessage());
                dbUser = null;
            }
        }

        Long dbId = dbUser != null ? dbUser.getIdUser() : null;

        UserInfoDTO userInfo = UserInfoDTO.builder()
                .id(dbId)
                .sub(jwt.getSubject())
                .username(username)
                .email(email)
                .fullName(fullName)
                .role(appRole)
                .realmRoles(realmRoles)
                .groups(groups)
                .build();

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Determine the primary app role from Keycloak realm roles.
     * Priority: ADMIN > MANAGER > AGENT > SURVEY_REQUESTER (demandeur)
     */
    private String determineAppRole(List<String> realmRoles) {
        for (String role : realmRoles) {
            String r = role.toLowerCase().trim();
            if (r.equals("admin") || r.equals("admins")) return "ADMIN";
        }
        for (String role : realmRoles) {
            String r = role.toLowerCase().trim();
            if (r.equals("manager") || r.equals("managers")) return "MANAGER";
        }
        for (String role : realmRoles) {
            String r = role.toLowerCase().trim();
            if (r.equals("agent") || r.equals("agents")) return "AGENT";
        }
        for (String role : realmRoles) {
            String r = role.toLowerCase().trim();
            if (r.equals("demandeur") || r.equals("survey_requester")
                    || r.equals("requester") || r.equals("demandeurs"))
                return "SURVEY_REQUESTER";
        }
        return "AGENT"; // default
    }
}
