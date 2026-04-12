package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Role;
import com.example.callcenter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(Role role);
    @Query("SELECT u FROM User u WHERE u.role = 'AGENT'")
    List<User> findAllAgents();
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:q% OR u.email LIKE %:q% OR u.username LIKE %:q%")
    List<User> search(String q);
}