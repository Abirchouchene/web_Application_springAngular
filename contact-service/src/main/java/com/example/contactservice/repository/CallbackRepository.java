package com.example.contactservice.repository;

import com.example.contactservice.entity.Callback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CallbackRepository extends JpaRepository<Callback, Long> {
    List<Callback> findByAgentIdAndScheduledDateAfter(Long agentId, LocalDateTime now);
    
    List<Callback> findByStatusAndScheduledDateBetween(
        com.example.contactservice.entity.CallbackStatus status, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );
} 