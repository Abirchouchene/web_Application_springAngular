package com.abirch.security.service;

import com.abirch.security.user.Permission;
import com.abirch.security.user.PermissionRepository;
import com.abirch.security.user.Role;
import com.abirch.security.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role createRole(Role role) {
        String userEmail = getCurrentUserEmail();
       if (userEmail != null) {
            auditService.logAction(userEmail, "Création du rôle : " + role.getNom());
        }
        return roleRepository.save(role);
    }

    public Role assignPermissions(Integer roleId, List<Integer> permissionIds) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.getPermissions().addAll(permissions);
        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            auditService.logAction(userEmail, "Assignation de permissions au rôle : " + role.getNom());
        }
        return roleRepository.save(role);
    }
}
