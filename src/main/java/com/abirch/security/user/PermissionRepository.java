package com.abirch.security.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
  //  List<Permission> findAllById(Set<Long> ids);

    // Tu peux ajouter des méthodes personnalisées ici si nécessaire
}