package com.example.callcenter.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data

public class AgentAvailabilityDTO {
    private Long agentId;
    private String agentName;
    private boolean available;
    private LocalDate leaveStartDate;
    private LocalDate leaveEndDate;
}
