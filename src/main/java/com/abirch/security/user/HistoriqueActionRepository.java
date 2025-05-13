package com.abirch.security.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueActionRepository extends JpaRepository<HistoriqueAction, Integer> {
    // Tu peux ajouter des méthodes personnalisées ici si nécessaire
}