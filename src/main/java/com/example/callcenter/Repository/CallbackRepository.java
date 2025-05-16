package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Callback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CallbackRepository extends JpaRepository<Callback, Long> {

    List<Callback> findByAgent_IdUserAndScheduledDateAfter(Long agentId, LocalDateTime now);

}