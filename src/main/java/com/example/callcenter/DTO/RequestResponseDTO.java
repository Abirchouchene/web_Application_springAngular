package com.example.callcenter.DTO;

import com.example.callcenter.Entity.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestResponseDTO {
    private Long idR;
    private String title;
    private String description;
    private String note;
    private String attachmentPath;
    private LocalDate deadline;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private Status status;
    private RequestType requestType;
    private CategoryRequest categoryRequest;
    private Priority priority;
    
    @JsonIgnoreProperties({"requests", "assignedRequests", "password", "role"})
    private User user;
    
    private List<Question> questions;
    
    @JsonIgnoreProperties({"requests", "assignedRequests", "password", "role"})
    private User agent;
    
    // Exclude logs to prevent circular reference
    // private List<Logs> logs;
    
    private Report report;
    private List<Submission> submissionList;
    private List<ContactResponse> contacts;
} 