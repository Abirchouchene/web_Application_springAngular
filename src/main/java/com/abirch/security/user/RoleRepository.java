package com.abirch.security.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    boolean existsByNom(String nom);

    Optional<Role> findByNom(String nom);
}
