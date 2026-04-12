package com.example.callcenter.DTO;

import com.example.callcenter.Entity.CategoryRequest;
import com.example.callcenter.Entity.Priority;
import com.example.callcenter.Entity.RequestType;
import com.example.callcenter.Entity.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestSummaryDTO {
    private Long idR;
    private String title;
    private String description;
    private LocalDate deadline;
    private Status status;
    private RequestType requestType;
    private CategoryRequest categoryRequest;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String requesterName; // Name of the user who created the request
    private String agentName; // Name of the assigned agent (if any)
} 