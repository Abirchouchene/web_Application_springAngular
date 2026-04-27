package com.example.callcenter.Repository;

import com.example.callcenter.Entity.EvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, Long> {

    List<EvaluationRecord> findByReport_Id(Long reportId);

    Optional<EvaluationRecord> findByReport_IdAndUserId(Long reportId, Long userId);

    boolean existsByReport_IdAndUserId(Long reportId, Long userId);
}
