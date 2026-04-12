package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByAgent_IdUserOrderByTimestampDesc(Long agentId);

    void deleteByAgent_IdUser(Long agentId);
}