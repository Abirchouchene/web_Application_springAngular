package com.example.contactservice.dto;

import com.example.contactservice.entity.CallbackStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackDTO {
    private Long id;
    private Long contactId;
    private Long requestId;
    private Long agentId;
    private LocalDateTime scheduledDate;
    private String notes;
    private CallbackStatus status;
} 