package com.example.callcenter.Controller;

import com.example.callcenter.DTO.CreateUserDTO;
import com.example.callcenter.Entity.Role;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserDTO dto) {
        try {
            User user = adminService.createUser(dto);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error creating user: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "50") int max,
            @RequestParam(required = false) String search) {
        try {
            if (search != null && !search.isBlank()) {
                return ResponseEntity.ok(adminService.searchUsers(search));
            }
            return ResponseEntity.ok(adminService.getAllUsers(first, max));
        } catch (Exception e) {
            log.error("Error listing users: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getUserById(id));
        } catch (Exception e) {
            log.error("Error getting user: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody CreateUserDTO dto) {
        try {
            User user = adminService.updateUser(id, dto);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error updating user: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting user: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/toggle-enabled")
    public ResponseEntity<?> toggleEnabled(@PathVariable Long id) {
        try {
            adminService.toggleEnabled(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error toggling user enabled: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/send-reset-email")
    public ResponseEntity<?> sendResetPasswordEmail(@PathVariable Long id) {
        try {
            adminService.sendResetPasswordEmail(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error sending reset email: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        try {
            String newPassword = (String) body.get("value");
            boolean temporary = body.containsKey("temporary") && (boolean) body.get("temporary");
            adminService.resetPassword(id, newPassword, temporary);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error resetting password: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /admin/forgot-password — public endpoint, sends reset email via Mailjet */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
            }
            adminService.sendResetEmailByEmail(email, null);
            return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé"));
        } catch (Exception e) {
            log.error("Error sending forgot password email: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /admin/reset-password-token — validate token and set new password */
    @PostMapping("/reset-password-token")
    public ResponseEntity<?> resetPasswordWithToken(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");
            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token et mot de passe requis"));
            }
            adminService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (Exception e) {
            log.error("Error resetting password with token: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /admin/users/sync — sync users from Keycloak to local DB */
    @PostMapping("/users/sync")
    public ResponseEntity<?> syncKeycloakUsers() {
        try {
            return ResponseEntity.ok(adminService.syncKeycloakUsers());
        } catch (Exception e) {
            log.error("Error syncing users: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** PUT /admin/users/{id}/role — change user role and sync to Keycloak group */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        try {
            String roleStr = body.get("role");
            if (roleStr == null || roleStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le rôle est requis"));
            }
            Role newRole = Role.valueOf(roleStr.toUpperCase());
            User user = adminService.changeUserRole(id, newRole);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Rôle invalide. Valeurs acceptées: ADMIN, MANAGER, AGENT, SURVEY_REQUESTER"));
        } catch (Exception e) {
            log.error("Error changing user role: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
