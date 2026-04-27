package com.example.callcenter.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "ratings_json", columnDefinition = "TEXT")
    private String ratingsJson;

    @Column(name = "binary_answers_json", columnDefinition = "TEXT")
    private String binaryAnswersJson;

    @Column(name = "open_answer", columnDefinition = "TEXT")
    private String openAnswer;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "quality_level")
    private String qualityLevel;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
