package com.abirch.security.service;

import com.abirch.security.user.Permission;
import com.abirch.security.user.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Set<Permission> getPermissionsByIds(Set<Long> ids) {
        return new HashSet<>(permissionRepository.findAllById(ids));
    }

    public Permission createPermission(Permission permission) {
        return permissionRepository.save(permission);
    }

}