package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponseRepository extends JpaRepository<Response, Long> {
}