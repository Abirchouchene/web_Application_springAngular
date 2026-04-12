package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Logs;
import com.example.callcenter.Entity.LogAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogsRepository extends JpaRepository<Logs, Long> {
    
    // Find all logs for a specific request
    List<Logs> findByRequest_IdROrderByTimestampDesc(Long requestId);
    
    // Find all logs for a specific request with pagination
    Page<Logs> findByRequest_IdROrderByTimestampDesc(Long requestId, Pageable pageable);
    
    // Find logs by action type
    List<Logs> findByLogActionOrderByTimestampDesc(LogAction logAction);
    
    // Find logs by action type with pagination
    Page<Logs> findByLogActionOrderByTimestampDesc(LogAction logAction, Pageable pageable);
    
    // Find logs by action and date range with pagination
    Page<Logs> findByLogActionAndTimestampBetweenOrderByTimestampDesc(LogAction logAction, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find logs within a date range with pagination
    Page<Logs> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Find logs by user ID with pagination
    Page<Logs> findByRequest_User_IdUserOrderByTimestampDesc(Long userId, Pageable pageable);
    
    // Count logs after a specific timestamp
    @Query("SELECT COUNT(l) FROM Logs l WHERE l.timestamp > :timestamp")
    long countByTimestampAfter(@Param("timestamp") LocalDateTime timestamp);
    
    // Find top N logs ordered by timestamp desc
    @Query(value = "SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<Logs> findTopNByOrderByTimestampDesc(@Param("limit") int limit);
    
    // Find logs within a date range (non-paginated)
    List<Logs> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find logs for a specific request and action
    List<Logs> findByRequest_IdRAndLogActionOrderByTimestampDesc(Long requestId, LogAction logAction);
    
    // Find recent logs (last 24 hours)
    @Query("SELECT l FROM Logs l WHERE l.timestamp >= :startDate ORDER BY l.timestamp DESC")
    List<Logs> findRecentLogs(@Param("startDate") LocalDateTime startDate);
    
    // Find logs with status changes
    @Query("SELECT l FROM Logs l WHERE l.oldStatus IS NOT NULL AND l.newStatus IS NOT NULL ORDER BY l.timestamp DESC")
    List<Logs> findStatusChangeLogs();
    
    // Find logs with priority changes
    @Query("SELECT l FROM Logs l WHERE l.oldPriority IS NOT NULL AND l.newPriority IS NOT NULL ORDER BY l.timestamp DESC")
    List<Logs> findPriorityChangeLogs();
    
    // Find logs with agent assignments
    @Query("SELECT l FROM Logs l WHERE l.logAction IN ('AGENT_ASSIGNED', 'AGENT_UNASSIGNED') ORDER BY l.timestamp DESC")
    List<Logs> findAgentAssignmentLogs();
} 