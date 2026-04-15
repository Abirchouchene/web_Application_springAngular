package com.example.callcenter.Service;

import com.example.callcenter.DTO.CreateUserDTO;
import com.example.callcenter.Entity.PasswordResetToken;
import com.example.callcenter.Entity.Role;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Repository.PasswordResetTokenRepository;
import com.example.callcenter.Repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final Keycloak keycloak;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // =================== LOCAL DB OPERATIONS ===================

    @Transactional
    public User createUser(CreateUserDTO dto) {
        // 1. Create local DB user
        User localUser = new User();
        localUser.setFullName(dto.getFirstName() + " " + dto.getLastName());
        localUser.setEmail(dto.getEmail());
        localUser.setUsername(dto.getUsername());
        localUser.setEnabled(true);
        localUser.setRole(dto.getRole() != null ? dto.getRole() : Role.AGENT);
        localUser = userRepository.save(localUser);
        log.info("Created local user {} (id={})", dto.getUsername(), localUser.getIdUser());

        // 2. Try to create in Keycloak (optional — graceful fallback)
        try {
            String kcId = createKeycloakUser(dto);
            if (kcId != null) {
                String roleName = dto.getRole() != null ? dto.getRole().name() : "AGENT";
                tryAssignRealmRole(kcId, roleName);
                tryAssignGroup(kcId, roleName);
            }
        } catch (Exception e) {
            log.warn("Keycloak sync failed for {}: {}", dto.getUsername(), e.getMessage());
        }

        // 3. Send activation/welcome email via Mailjet
        try {
            emailService.sendUserCreatedEmail(
                    dto.getEmail(),
                    dto.getFirstName() + " " + dto.getLastName(),
                    dto.getUsername(),
                    dto.getPassword()
            );
        } catch (Exception e) {
            log.warn("Could not send welcome email to {}: {}", dto.getEmail(), e.getMessage());
        }

        return localUser;
    }

    public List<User> getAllUsers(int first, int max) {
        return userRepository.findAll(PageRequest.of(first / Math.max(max, 1), Math.max(max, 1))).getContent();
    }

    /**
     * Change a user's role in local DB and sync to Keycloak (realm role + group).
     */
    @Transactional
    public User changeUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Changed role of user {} from {} to {}", user.getUsername(), oldRole, newRole);

        // Sync to Keycloak
        try {
            List<UserRepresentation> kcUsers = getUsersResource().search(user.getEmail(), true);
            if (!kcUsers.isEmpty()) {
                String kcUserId = kcUsers.get(0).getId();
                // Remove old realm role
                tryRemoveRealmRole(kcUserId, oldRole.name());
                // Remove old group
                tryRemoveGroup(kcUserId, oldRole.name());
                // Assign new realm role
                tryAssignRealmRole(kcUserId, newRole.name());
                // Assign new group
                tryAssignGroup(kcUserId, newRole.name());
            }
        } catch (Exception e) {
            log.warn("Could not sync role change to Keycloak for {}: {}", user.getUsername(), e.getMessage());
        }
        return user;
    }

    /**
     * Sync users from Keycloak to local DB and return the merged list.
     */
    @Transactional
    public List<User> syncKeycloakUsers() {
        try {
            List<UserRepresentation> kcUsers = getUsersResource().list(0, 200);
            for (UserRepresentation kc : kcUsers) {
                if (kc.getEmail() == null) continue;
                Optional<User> existing = userRepository.findByEmail(kc.getEmail());
                if (existing.isEmpty()) {
                    // Also check by username
                    existing = userRepository.findByUsername(kc.getUsername());
                }
                if (existing.isEmpty()) {
                    User newUser = new User();
                    newUser.setUsername(kc.getUsername());
                    newUser.setEmail(kc.getEmail());
                    newUser.setFullName(
                            (kc.getFirstName() != null ? kc.getFirstName() : "") + " " +
                            (kc.getLastName() != null ? kc.getLastName() : "")
                    );
                    newUser.setEnabled(kc.isEnabled());
                    // Map KC realm roles to app Role
                    newUser.setRole(mapKeycloakRole(kc));
                    userRepository.save(newUser);
                    log.info("Synced Keycloak user {} to local DB", kc.getUsername());
                } else {
                    // Update existing
                    User user = existing.get();
                    if (kc.getFirstName() != null) {
                        user.setFullName(kc.getFirstName() + " " + (kc.getLastName() != null ? kc.getLastName() : ""));
                    }
                    user.setEnabled(kc.isEnabled());
                    if (user.getEmail() == null || user.getEmail().isBlank()) {
                        user.setEmail(kc.getEmail());
                    }
                    if (user.getUsername() == null || user.getUsername().isBlank()) {
                        user.setUsername(kc.getUsername());
                    }
                    userRepository.save(user);
                }
            }
            log.info("Synced {} Keycloak users to local DB", kcUsers.size());
        } catch (Exception e) {
            log.warn("Could not sync from Keycloak: {}", e.getMessage());
        }
        return userRepository.findAll();
    }

    private Role mapKeycloakRole(UserRepresentation kcUser) {
        try {
            // 1. Check realm roles
            var roles = getUsersResource().get(kcUser.getId()).roles().realmLevel().listEffective();
            for (var role : roles) {
                Role mapped = mapRoleName(role.getName());
                if (mapped != null) return mapped;
            }
            // 2. Check groups
            var groups = getUsersResource().get(kcUser.getId()).groups();
            for (var group : groups) {
                Role mapped = mapGroupName(group.getName());
                if (mapped != null) return mapped;
            }
        } catch (Exception e) {
            log.warn("Could not get roles for KC user {}: {}", kcUser.getUsername(), e.getMessage());
        }
        return Role.AGENT;
    }

    /**
     * Map a Keycloak realm role name to the app Role enum.
     * Supports: admin, manager, agent, demandeur, survey_requester
     */
    static Role mapRoleName(String roleName) {
        if (roleName == null) return null;
        switch (roleName.toLowerCase().trim()) {
            case "admin":
            case "admins": return Role.ADMIN;
            case "manager":
            case "managers": return Role.MANAGER;
            case "agent":
            case "agents": return Role.AGENT;
            case "demandeur":
            case "demandeurs":
            case "survey_requester": return Role.SURVEY_REQUESTER;
            default: return null;
        }
    }

    /**
     * Map a Keycloak group name to the app Role enum.
     * Groups: Admins, Managers, Agents, Demandeurs
     */
    static Role mapGroupName(String groupName) {
        if (groupName == null) return null;
        switch (groupName.toLowerCase()) {
            case "admins": return Role.ADMIN;
            case "managers": return Role.MANAGER;
            case "agents": return Role.AGENT;
            case "demandeurs": return Role.SURVEY_REQUESTER;
            default: return null;
        }
    }

    public List<User> searchUsers(String query) {
        return userRepository.search(query);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    @Transactional
    public User updateUser(Long userId, CreateUserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (dto.getFirstName() != null && dto.getLastName() != null) {
            user.setFullName(dto.getFirstName() + " " + dto.getLastName());
        }
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        return userRepository.save(user);
    }

    @Transactional
    public void toggleEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public void sendResetPasswordEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        sendResetEmailByEmail(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void sendResetEmailByEmail(String email, String name) {
        // Delete any existing tokens for this email
        tokenRepository.deleteByEmail(email);

        // Generate token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        // Build reset link
        String resetLink = frontendUrl + "/authentication/reset-password?token=" + token;

        // Send via Mailjet
        emailService.sendResetPasswordEmail(email, name, resetLink);
        log.info("Reset password token generated and email sent to {}", email);
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Le lien a expiré. Veuillez en demander un nouveau.");
        }
        if (resetToken.isUsed()) {
            throw new RuntimeException("Ce lien a déjà été utilisé.");
        }

        // Try to reset in Keycloak
        try {
            List<UserRepresentation> kcUsers = getUsersResource().search(resetToken.getEmail(), true);
            if (!kcUsers.isEmpty()) {
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(newPassword);
                cred.setTemporary(false);
                getUsersResource().get(kcUsers.get(0).getId()).resetPassword(cred);
                log.info("Password reset in Keycloak for {}", resetToken.getEmail());
            }
        } catch (Exception e) {
            log.warn("Could not reset password in Keycloak for {}: {}", resetToken.getEmail(), e.getMessage());
        }

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    public void resetPassword(Long userId, String newPassword, boolean temporary) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        try {
            List<UserRepresentation> kcUsers = getUsersResource().search(user.getEmail(), true);
            if (!kcUsers.isEmpty()) {
                CredentialRepresentation cred = new CredentialRepresentation();
                cred.setType(CredentialRepresentation.PASSWORD);
                cred.setValue(newPassword);
                cred.setTemporary(temporary);
                getUsersResource().get(kcUsers.get(0).getId()).resetPassword(cred);
                log.info("Password reset for user {}", user.getEmail());
                return;
            }
        } catch (Exception e) {
            log.warn("Could not reset password via Keycloak for {}: {}", user.getEmail(), e.getMessage());
        }
        throw new RuntimeException("Could not reset password. Keycloak admin access not available.");
    }

    // =================== KEYCLOAK HELPERS ===================

    private RealmResource getRealmResource() {
        return keycloak.realm(realm);
    }

    private UsersResource getUsersResource() {
        return getRealmResource().users();
    }

    private String createKeycloakUser(CreateUserDTO dto) {
        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(dto.getUsername());
        kcUser.setEmail(dto.getEmail());
        kcUser.setFirstName(dto.getFirstName());
        kcUser.setLastName(dto.getLastName());
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(false);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setTemporary(true);
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setValue(dto.getPassword());
            kcUser.setCredentials(Collections.singletonList(cred));
        }

        Response response = getUsersResource().create(kcUser);
        if (response.getStatus() == 201) {
            String loc = response.getHeaderString("Location");
            String kcId = loc.substring(loc.lastIndexOf("/") + 1);
            response.close();
            log.info("Created Keycloak user {} (kcId={})", dto.getUsername(), kcId);
            return kcId;
        }
        String body = response.readEntity(String.class);
        log.warn("Keycloak user creation returned {}: {}", response.getStatus(), body);
        response.close();
        return null;
    }

    private void tryAssignRealmRole(String kcUserId, String roleName) {
        String kcRoleName = toKeycloakRoleName(roleName);
        try {
            RoleRepresentation role = getRealmResource().roles().get(kcRoleName).toRepresentation();
            getUsersResource().get(kcUserId).roles().realmLevel().add(Collections.singletonList(role));
            log.info("Assigned realm role '{}' to KC user {}", kcRoleName, kcUserId);
        } catch (Exception e) {
            log.warn("Could not assign role '{}': {}", kcRoleName, e.getMessage());
        }
    }

    private void tryRemoveRealmRole(String kcUserId, String roleName) {
        String kcRoleName = toKeycloakRoleName(roleName);
        try {
            RoleRepresentation role = getRealmResource().roles().get(kcRoleName).toRepresentation();
            getUsersResource().get(kcUserId).roles().realmLevel().remove(Collections.singletonList(role));
            log.info("Removed realm role '{}' from KC user {}", kcRoleName, kcUserId);
        } catch (Exception e) {
            log.warn("Could not remove role '{}': {}", kcRoleName, e.getMessage());
        }
    }

    private void tryAssignGroup(String kcUserId, String roleName) {
        String groupName = toKeycloakGroupName(roleName);
        if (groupName == null) return;
        try {
            List<GroupRepresentation> groups = getRealmResource().groups().groups(groupName, 0, 1);
            if (!groups.isEmpty()) {
                getUsersResource().get(kcUserId).joinGroup(groups.get(0).getId());
                log.info("Assigned KC user {} to group '{}'", kcUserId, groupName);
            } else {
                log.warn("Keycloak group '{}' not found", groupName);
            }
        } catch (Exception e) {
            log.warn("Could not assign group '{}': {}", groupName, e.getMessage());
        }
    }

    private void tryRemoveGroup(String kcUserId, String roleName) {
        String groupName = toKeycloakGroupName(roleName);
        if (groupName == null) return;
        try {
            List<GroupRepresentation> groups = getRealmResource().groups().groups(groupName, 0, 1);
            if (!groups.isEmpty()) {
                getUsersResource().get(kcUserId).leaveGroup(groups.get(0).getId());
                log.info("Removed KC user {} from group '{}'", kcUserId, groupName);
            }
        } catch (Exception e) {
            log.warn("Could not remove group '{}': {}", groupName, e.getMessage());
        }
    }

    /**
     * Map app Role enum name to Keycloak realm role name.
     * ADMIN→admin, MANAGER→manager, AGENT→agent, SURVEY_REQUESTER→demandeur
     */
    static String toKeycloakRoleName(String appRole) {
        if (appRole == null) return "agent";
        switch (appRole.toUpperCase()) {
            case "ADMIN": return "admin";
            case "MANAGER": return "manager";
            case "AGENT": return "agent";
            case "SURVEY_REQUESTER": return "demandeur";
            default: return appRole.toLowerCase();
        }
    }

    /**
     * Map app Role enum name to Keycloak group name.
     * ADMIN→Admins, MANAGER→Managers, AGENT→Agents, SURVEY_REQUESTER→Demandeurs
     */
    static String toKeycloakGroupName(String appRole) {
        if (appRole == null) return "Agents";
        switch (appRole.toUpperCase()) {
            case "ADMIN": return "Admins";
            case "MANAGER": return "Managers";
            case "AGENT": return "Agents";
            case "SURVEY_REQUESTER": return "Demandeurs";
            default: return null;
        }
    }

    private void trySendActionsEmail(String kcUserId, String email, List<String> actions) {
        try {
            getUsersResource().get(kcUserId).executeActionsEmail(actions);
            log.info("Actions email sent to {}: {}", email, actions);
        } catch (Exception e) {
            log.warn("Could not send actions email to {}: {}", email, e.getMessage());
        }
    }
}
