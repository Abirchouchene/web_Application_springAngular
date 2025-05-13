package com.abirch.security.controller;

import com.abirch.security.service.RoleService;
import com.abirch.security.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.createRole(role);
    }

    @PutMapping("/{id}/permissions")
    public Role addPermissionsToRole(@PathVariable Integer id, @RequestBody List<Integer> permissionIds) {
        return roleService.assignPermissions(id, permissionIds);
    }
}

