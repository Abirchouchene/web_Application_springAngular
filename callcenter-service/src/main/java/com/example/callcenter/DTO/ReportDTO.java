package com.example.callcenter.DTO;

import com.example.callcenter.Entity.ReportStatus;
import com.example.callcenter.Entity.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private RequestSummaryDTO request;
    private String requestTitle;
    private RequestType requestType;
    private LocalDateTime generatedDate;
    private ReportStatus status;
    private LocalDateTime approvedDate;
    private LocalDateTime sentDate;
    private Integer totalContacts;
    private Integer contactedContacts;
    private Double contactRate;
    private String statisticsData;
    private String aiInsightsData;
    private LocalDateTime aiGeneratedDate;
}