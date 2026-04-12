package com.example.callcenter.Controller;

import com.example.callcenter.Entity.Logs;
import com.example.callcenter.Entity.LogAction;
import com.example.callcenter.Service.LogsService;
import com.example.callcenter.DTO.LogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/logs")
//@CrossOrigin("http://localhost:4200")
@RequiredArgsConstructor
public class LogsController {
    
    private final LogsService logsService;
    
    /**
     * Get all logs for a specific request with pagination
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<Page<Logs>> getLogsByRequestId(
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Logs> logs = logsService.getLogsByRequestIdPaginated(requestId, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get recent logs (last 24 hours)
     */
    @GetMapping("/recent-24h")
    public ResponseEntity<List<Logs>> getRecentLogs() {
        List<Logs> logs = logsService.getRecentLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get logs within a date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Logs>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Logs> logs = logsService.getLogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get status change logs
     */
    @GetMapping("/status-changes")
    public ResponseEntity<List<Logs>> getStatusChangeLogs() {
        List<Logs> logs = logsService.getStatusChangeLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get priority change logs
     */
    @GetMapping("/priority-changes")
    public ResponseEntity<List<Logs>> getPriorityChangeLogs() {
        List<Logs> logs = logsService.getPriorityChangeLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get agent assignment logs
     */
    @GetMapping("/agent-assignments")
    public ResponseEntity<List<Logs>> getAgentAssignmentLogs() {
        List<Logs> logs = logsService.getAgentAssignmentLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get logs by action type
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<Logs>> getLogsByAction(@PathVariable LogAction action) {
        List<Logs> logs = logsService.getLogsByAction(action);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get all logs with optional filtering and pagination
     */
    @GetMapping
    public ResponseEntity<Page<Logs>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) LogAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Logs> logs = logsService.getAllLogsPaginated(pageable, action, startDate, endDate);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get logs by user ID with pagination
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Logs>> getLogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Logs> logs = logsService.getLogsByUserIdPaginated(userId, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get logs by action type with pagination
     */
    @GetMapping("/action/{action}/paginated")
    public ResponseEntity<Page<Logs>> getLogsByActionPaginated(
            @PathVariable LogAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Logs> logs = logsService.getLogsByActionPaginated(action, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get logs by date range with pagination
     */
    @GetMapping("/date-range/paginated")
    public ResponseEntity<Page<Logs>> getLogsByDateRangePaginated(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Logs> logs = logsService.getLogsByDateRangePaginated(startDate, endDate, pageable);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Create a new log entry using the unified LogDTO
     */
    @PostMapping
    public ResponseEntity<Logs> createLog(@RequestBody LogDTO logDTO) {
        Logs savedLog = logsService.createLogFromDTO(logDTO);
        return ResponseEntity.ok(savedLog);
    }
    
    /**
     * Get log statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLogStatistics() {
        Map<String, Object> statistics = logsService.getLogStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Get recent activity logs
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Logs>> getRecentActivity(@RequestParam(defaultValue = "10") int limit) {
        List<Logs> logs = logsService.getRecentActivity(limit);
        return ResponseEntity.ok(logs);
    }
} 