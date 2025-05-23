package com.abirch.security.service;

import com.abirch.security.user.Permission;
import com.abirch.security.user.PermissionRepository;
import com.abirch.security.user.Role;
import com.abirch.security.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

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

    public Role createRole(Role roleInput) {
        // Extraire les IDs de permission reçus dans le JSON
        Set<Long> permissionIds = roleInput.getPermissions()
                .stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        // Charger les vraies entités Permission depuis la base
        Set<Permission> permissions = permissionService.getPermissionsByIds(permissionIds);

        // Construire le rôle avec les vraies permissions
        Role roleToSave = Role.builder()
                .nom(roleInput.getNom())
                .description(roleInput.getDescription())
                .permissions(permissions)
                .build();

        return roleRepository.save(roleToSave);
    }

}
