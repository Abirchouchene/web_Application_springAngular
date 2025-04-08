package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Role;
import com.example.callcenter.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByRole(Role role);
}