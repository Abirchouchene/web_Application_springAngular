package com.example.callcenter.DTO;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class QualityEvaluationDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationForm {
        private String reportTitle;
        private String contextHint;
        private List<RatingQuestion> ratings;
        private List<BinaryQuestion> binaryQuestions;
        private String openQuestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingQuestion {
        private String id;
        private String label;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinaryQuestion {
        private String id;
        private String label;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationSubmission {
        private Long reportId;
        private Map<String, Integer> ratings;        // id -> 1..5
        private Map<String, Boolean> binaryAnswers;  // id -> true/false
        private String openAnswer;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationResult {
        private boolean success;
        private String message;
        private double averageRating;
        private String qualityLevel;   // EXCELLENT / GOOD / AVERAGE / POOR
        private Long recordId;
    }
}
