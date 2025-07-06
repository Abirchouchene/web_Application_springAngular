package com.example.callcenter.DTO;

import com.example.callcenter.Entity.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDTO {
    // Basic log information
    private Long id;
    private Long requestId;
    private LogAction logAction;
    private String actionDescription;
    private String details;
    
    // Status changes
    private Status oldStatus;
    private Status newStatus;
    
    // Priority changes
    private Priority oldPriority;
    private Priority newPriority;
    
    // Agent assignment changes
    private String oldAssignedAgent;
    private String newAssignedAgent;
    
    // Timestamp and metadata
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    
    // User information (who performed the action)
    private Long userId;
    private String userFullName;
    
    // Request information (for context)
    private String requestTitle;
    private String requestDescription;
    
    // Additional fields for specific operations
    private String reason; // For approval/rejection reasons
    private boolean approved; // For approval operations
} 