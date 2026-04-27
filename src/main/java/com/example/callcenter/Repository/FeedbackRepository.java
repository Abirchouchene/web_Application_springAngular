package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByReport_Id(Long reportId);

    Optional<Feedback> findByReport_IdAndUserId(Long reportId, Long userId);

    boolean existsByReport_IdAndUserId(Long reportId, Long userId);
}
