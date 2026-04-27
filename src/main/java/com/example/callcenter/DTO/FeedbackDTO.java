package com.example.callcenter.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {

    private Long id;
    private Long reportId;
    private String reportTitle;
    private Long userId;

    /** 1 (très insatisfait) → 5 (très satisfait) */
    private Integer rating;

    private String comment;

    private LocalDateTime submittedAt;

    /** JSON string returned by AI analysis (null until AI has run) */
    private String aiAnalysis;

    private LocalDateTime aiAnalysisDate;

    private boolean aiAnalyzed;
}
