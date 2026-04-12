package com.example.callcenter.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightDTO {
    private Long reportId;
    private String summary;
    private List<RecommendationDTO> recommendations;
    private List<KeyFindingDTO> keyFindings;
    private SentimentAnalysisDTO sentimentAnalysis;
    private LocalDateTime generatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationDTO {
        private String title;
        private String description;
        private String priority;   // HIGH, MEDIUM, LOW
        private String category;   // PROCESS_IMPROVEMENT, TRAINING, RESOURCE_ALLOCATION, FOLLOW_UP
        private String actionable; // Concrete next step
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeyFindingDTO {
        private String finding;
        private String evidence;
        private String impact; // HIGH, MEDIUM, LOW
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SentimentAnalysisDTO {
        private double positivePercent;
        private double neutralPercent;
        private double negativePercent;
        private String overallSentiment; // POSITIVE, NEUTRAL, NEGATIVE, MIXED
    }
}
