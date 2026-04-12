package com.example.callcenter.Service;

import com.example.callcenter.Entity.Logs;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Entity.Status;
import com.example.callcenter.Entity.Priority;
import com.example.callcenter.Entity.LogAction;
import com.example.callcenter.Repository.LogsRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.DTO.LogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class LogsService {
    
    private final LogsRepository logsRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a log entry from LogDTO
     */
    public Logs createLogFromDTO(LogDTO logDTO) {
        Logs log = new Logs();
        
        // Set basic information
        log.setLogAction(logDTO.getLogAction());
        log.setActionDescription(logDTO.getActionDescription());
        log.setDetails(logDTO.getDetails());
        log.setTimestamp(logDTO.getTimestamp() != null ? logDTO.getTimestamp() : LocalDateTime.now());
        log.setIpAddress(logDTO.getIpAddress());
        log.setUserAgent(logDTO.getUserAgent());
        
        // Set status changes
        log.setOldStatus(logDTO.getOldStatus());
        log.setNewStatus(logDTO.getNewStatus());
        
        // Set priority changes
        log.setOldPriority(logDTO.getOldPriority());
        log.setNewPriority(logDTO.getNewPriority());
        
        // Set agent assignment changes
        log.setOldAssignedAgent(logDTO.getOldAssignedAgent());
        log.setNewAssignedAgent(logDTO.getNewAssignedAgent());
        
        // Set request reference
        if (logDTO.getRequestId() != null) {
            Request request = requestRepository.findById(logDTO.getRequestId())
                    .orElseThrow(() -> new RuntimeException("Request not found"));
            log.setRequest(request);
        }
        
        return logsRepository.save(log);
    }
    
    /**
     * Create a log entry for request creation
     */
    public Logs logRequestCreated(Request request, User user, String ipAddress, String userAgent) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.REQUEST_CREATED);
        logDTO.setActionDescription("Nouvelle demande créée");
        logDTO.setDetails("Demande '" + request.getTitle() + "' créée par " + user.getFullName());
        logDTO.setNewStatus(request.getStatus());
        logDTO.setNewPriority(request.getPriority());
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for status change
     */
    public Logs logStatusChange(Request request, User user, Status oldStatus, Status newStatus, 
                               String ipAddress, String userAgent, String reason) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.STATUS_CHANGED);
        logDTO.setActionDescription("Statut modifié de " + oldStatus + " vers " + newStatus);
        logDTO.setDetails("Raison: " + reason);
        logDTO.setOldStatus(oldStatus);
        logDTO.setNewStatus(newStatus);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        logDTO.setReason(reason);
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for priority change
     */
    public Logs logPriorityChange(Request request, User user, Priority oldPriority, Priority newPriority,
                                 String ipAddress, String userAgent, String reason) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.PRIORITY_CHANGED);
        logDTO.setActionDescription("Priorité modifiée de " + oldPriority + " vers " + newPriority);
        logDTO.setDetails("Raison: " + reason);
        logDTO.setOldPriority(oldPriority);
        logDTO.setNewPriority(newPriority);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        logDTO.setReason(reason);
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for agent assignment
     */
    public Logs logAgentAssignment(Request request, User user, User oldAgent, User newAgent,
                                  String ipAddress, String userAgent, String reason) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.AGENT_ASSIGNED);
        logDTO.setActionDescription("Agent assigné: " + (newAgent != null ? newAgent.getFullName() : "Aucun"));
        logDTO.setDetails("Raison: " + reason);
        logDTO.setOldAssignedAgent(oldAgent != null ? oldAgent.getFullName() : "Aucun");
        logDTO.setNewAssignedAgent(newAgent != null ? newAgent.getFullName() : "Aucun");
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        logDTO.setReason(reason);
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for request approval/rejection
     */
    public Logs logRequestApproval(Request request, User user, boolean approved, String reason,
                                  String ipAddress, String userAgent) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(approved ? LogAction.REQUEST_APPROVED : LogAction.REQUEST_REJECTED);
        logDTO.setActionDescription(approved ? "Demande approuvée" : "Demande rejetée");
        logDTO.setDetails("Raison: " + reason);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        logDTO.setReason(reason);
        logDTO.setApproved(approved);
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for request update
     */
    public Logs logRequestUpdate(Request request, User user, String changes, String ipAddress, String userAgent) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.REQUEST_UPDATED);
        logDTO.setActionDescription("Demande mise à jour");
        logDTO.setDetails("Modifications: " + changes);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a log entry for request deletion
     */
    public Logs logRequestDeleted(Request request, User user, String reason, String ipAddress, String userAgent) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(LogAction.REQUEST_DELETED);
        logDTO.setActionDescription("Demande supprimée");
        logDTO.setDetails("Raison: " + reason);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        logDTO.setReason(reason);
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Create a custom log entry
     */
    public Logs createCustomLog(Request request, User user, LogAction action, String description,
                               String details, String ipAddress, String userAgent) {
        LogDTO logDTO = new LogDTO();
        logDTO.setRequestId(request.getIdR());
        logDTO.setLogAction(action);
        logDTO.setActionDescription(description);
        logDTO.setDetails(details);
        logDTO.setIpAddress(ipAddress);
        logDTO.setUserAgent(userAgent);
        logDTO.setTimestamp(LocalDateTime.now());
        logDTO.setUserId(user.getIdUser());
        logDTO.setUserFullName(user.getFullName());
        logDTO.setRequestTitle(request.getTitle());
        logDTO.setRequestDescription(request.getDescription());
        
        return createLogFromDTO(logDTO);
    }
    
    /**
     * Get all logs for a specific request
     */
    public List<Logs> getLogsByRequestId(Long requestId) {
        return logsRepository.findByRequest_IdROrderByTimestampDesc(requestId);
    }
    
    /**
     * Get recent logs (last 24 hours)
     */
    public List<Logs> getRecentLogs() {
        LocalDateTime startDate = LocalDateTime.now().minusHours(24);
        return logsRepository.findRecentLogs(startDate);
    }
    

    
    /**
     * Get logs within a date range
     */
    public List<Logs> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return logsRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate);
    }
    
    /**
     * Get status change logs
     */
    public List<Logs> getStatusChangeLogs() {
        return logsRepository.findStatusChangeLogs();
    }
    
    /**
     * Get priority change logs
     */
    public List<Logs> getPriorityChangeLogs() {
        return logsRepository.findPriorityChangeLogs();
    }
    
    /**
     * Get agent assignment logs
     */
    public List<Logs> getAgentAssignmentLogs() {
        return logsRepository.findAgentAssignmentLogs();
    }
    
    /**
     * Get logs by action type
     */
    public List<Logs> getLogsByAction(LogAction action) {
        return logsRepository.findByLogActionOrderByTimestampDesc(action);
    }
    
    /**
     * Get all logs for a specific request with pagination
     */
    public Page<Logs> getLogsByRequestIdPaginated(Long requestId, Pageable pageable) {
        return logsRepository.findByRequest_IdROrderByTimestampDesc(requestId, pageable);
    }
    
    /**
     * Get all logs with optional filtering and pagination
     */
    public Page<Logs> getAllLogsPaginated(Pageable pageable, LogAction action, LocalDateTime startDate, LocalDateTime endDate) {
        if (action != null && startDate != null && endDate != null) {
            return logsRepository.findByLogActionAndTimestampBetweenOrderByTimestampDesc(action, startDate, endDate, pageable);
        } else if (action != null) {
            return logsRepository.findByLogActionOrderByTimestampDesc(action, pageable);
        } else if (startDate != null && endDate != null) {
            return logsRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
        } else {
            return logsRepository.findAll(pageable);
        }
    }
    
    /**
     * Get logs by user ID with pagination
     */
    public Page<Logs> getLogsByUserIdPaginated(Long userId, Pageable pageable) {
        return logsRepository.findByRequest_User_IdUserOrderByTimestampDesc(userId, pageable);
    }
    
    /**
     * Get logs by action type with pagination
     */
    public Page<Logs> getLogsByActionPaginated(LogAction action, Pageable pageable) {
        return logsRepository.findByLogActionOrderByTimestampDesc(action, pageable);
    }
    
    /**
     * Get logs by date range with pagination
     */
    public Page<Logs> getLogsByDateRangePaginated(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return logsRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }
    
    /**
     * Create a new log entry
     */
    public Logs createLog(Logs log) {
        log.setTimestamp(LocalDateTime.now());
        return logsRepository.save(log);
    }
    
    /**
     * Get log statistics
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalLogs", logsRepository.count());
        statistics.put("logsToday", logsRepository.countByTimestampAfter(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)));
        statistics.put("logsThisWeek", logsRepository.countByTimestampAfter(LocalDateTime.now().minusWeeks(1)));
        statistics.put("logsThisMonth", logsRepository.countByTimestampAfter(LocalDateTime.now().minusMonths(1)));
        return statistics;
    }
    
    /**
     * Get recent activity logs
     */
    public List<Logs> getRecentActivity(int limit) {
        return logsRepository.findTopNByOrderByTimestampDesc(limit);
    }
} 