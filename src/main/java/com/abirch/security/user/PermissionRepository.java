package com.abirch.security.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    // Tu peux ajouter des méthodes personnalisées ici si nécessaire
}