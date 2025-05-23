package com.abirch.security.service;

import com.abirch.security.user.Role;
import com.abirch.security.user.RoleRepository;
import com.abirch.security.user.User;
import com.abirch.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    // injection de reposirtoy
    private final UserRepository userRepository;
    private final AuditService auditService ;
    private final RoleRepository roleRepository;


    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // ou "anonyme" selon besoin
        }
        // Dans la plupart des cas, getName() retourne l'email ou username connecté
        return authentication.getName();
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User createUser(User user) {
        User created = userRepository.save(user);

        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            auditService.logAction(userEmail, "Création de l'utilisateur : " + user.getEmail());
        }
        return created;
    }

    public User updateUser(Integer id, User updatedUser) {
        User user = getUserById(id);
        user.setFirstname(updatedUser.getFirstname());
        user.setLastname(updatedUser.getLastname());
        user.setEmail(updatedUser.getEmail());
        user.setRole(updatedUser.getRole());

        User updated = userRepository.save(user);

        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            auditService.logAction(userEmail, "Mise à jour de l'utilisateur : " + user.getEmail());
        }

        return updated;
    }

    public void deleteUser(Integer id) {
        User user = getUserById(id);
        userRepository.delete(user);

        String userEmail = getCurrentUserEmail();
       if (userEmail != null) {
            auditService.logAction(userEmail, "Suppression de l'utilisateur : " + user.getEmail());
        }
    } }

