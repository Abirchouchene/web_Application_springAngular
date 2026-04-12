package com.example.callcenter.Service;

import com.example.callcenter.DTO.AiInsightDTO;
import com.example.callcenter.DTO.AiInsightDTO.*;
import com.example.callcenter.Entity.Report;
import com.example.callcenter.Repository.ReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class OpenAiService {

    private final WebClient webClient;
    private final DataAnonymizationService anonymizationService;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    public OpenAiService(@Value("${openai.api-key:}") String apiKey,
                         @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                         DataAnonymizationService anonymizationService,
                         ReportRepository reportRepository,
                         ObjectMapper objectMapper) {
        this.anonymizationService = anonymizationService;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Generate AI insights for a report. If OpenAI is disabled, returns rule-based fallback.
     */
    public AiInsightDTO generateInsights(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (!enabled) {
            log.info("OpenAI disabled, generating rule-based insights for report {}", reportId);
            return generateFallbackInsights(report);
        }

        try {
            // Build anonymized context — no PII leaves the system
            String anonymizedContext = anonymizationService.buildAnonymizedContext(
                    report.getRequestTitle(),
                    report.getRequestType() != null ? report.getRequestType().name() : null,
                    report.getTotalContacts() != null ? report.getTotalContacts() : 0,
                    report.getContactedContacts() != null ? report.getContactedContacts() : 0,
                    report.getContactRate() != null ? report.getContactRate() : 0.0,
                    report.getStatisticsData()
            );

            String aiResponse = callOpenAi(anonymizedContext);
            AiInsightDTO insight = parseAiResponse(aiResponse, reportId);

            // Persist AI insights in the report
            report.setAiInsightsData(objectMapper.writeValueAsString(insight));
            report.setAiGeneratedDate(LocalDateTime.now());
            reportRepository.save(report);

            return insight;
        } catch (Exception e) {
            log.error("OpenAI call failed for report {}, falling back to rule-based", reportId, e);
            return generateFallbackInsights(report);
        }
    }

    /**
     * Get cached AI insights if available, generate otherwise.
     */
    public AiInsightDTO getInsights(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        if (report.getAiInsightsData() != null) {
            try {
                return objectMapper.readValue(report.getAiInsightsData(), AiInsightDTO.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached insights, regenerating", e);
            }
        }

        return generateInsights(reportId);
    }

    private String callOpenAi(String surveyContext) {
        String systemPrompt = """
                You are an expert survey data analyst for a call center system.
                Analyze the provided anonymized survey data and return a JSON response with this exact structure:
                {
                  "summary": "A concise 2-3 sentence executive summary of the survey findings",
                  "recommendations": [
                    {
                      "title": "Short title",
                      "description": "Detailed description",
                      "priority": "HIGH|MEDIUM|LOW",
                      "category": "PROCESS_IMPROVEMENT|TRAINING|RESOURCE_ALLOCATION|FOLLOW_UP",
                      "actionable": "Specific next step to take"
                    }
                  ],
                  "keyFindings": [
                    {
                      "finding": "What was found",
                      "evidence": "Data supporting this finding",
                      "impact": "HIGH|MEDIUM|LOW"
                    }
                  ],
                  "sentimentAnalysis": {
                    "positivePercent": 0.0,
                    "neutralPercent": 0.0,
                    "negativePercent": 0.0,
                    "overallSentiment": "POSITIVE|NEUTRAL|NEGATIVE|MIXED"
                  }
                }
                Provide 3-5 recommendations and 3-5 key findings. Return ONLY valid JSON, no markdown.
                """;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Analyze this survey data:\n\n" + surveyContext)
                ),
                "temperature", 0.3,
                "max_tokens", 2000
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Empty response from OpenAI");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in OpenAI response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private AiInsightDTO parseAiResponse(String aiResponse, Long reportId) {
        try {
            // Clean response (in case of markdown wrapping)
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            AiInsightDTO dto = objectMapper.readValue(cleaned, AiInsightDTO.class);
            dto.setReportId(reportId);
            dto.setGeneratedAt(LocalDateTime.now());
            return dto;
        } catch (Exception e) {
            log.warn("Failed to parse AI response, creating minimal insight", e);
            return AiInsightDTO.builder()
                    .reportId(reportId)
                    .summary(aiResponse.length() > 500 ? aiResponse.substring(0, 500) : aiResponse)
                    .recommendations(List.of())
                    .keyFindings(List.of())
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Rule-based fallback when OpenAI is disabled or unavailable.
     */
    @SuppressWarnings("unchecked")
    private AiInsightDTO generateFallbackInsights(Report report) {
        List<RecommendationDTO> recommendations = new ArrayList<>();
        List<KeyFindingDTO> findings = new ArrayList<>();

        double contactRate = report.getContactRate() != null ? report.getContactRate() : 0;
        int total = report.getTotalContacts() != null ? report.getTotalContacts() : 0;
        int contacted = report.getContactedContacts() != null ? report.getContactedContacts() : 0;

        // Analyze contact rate
        if (contactRate < 50) {
            recommendations.add(RecommendationDTO.builder()
                    .title("Améliorer le taux de réponse")
                    .description("Le taux de réponse est de " + String.format("%.1f%%", contactRate) +
                            ", ce qui est inférieur au seuil recommandé de 50%.")
                    .priority("HIGH")
                    .category("PROCESS_IMPROVEMENT")
                    .actionable("Revoir la stratégie de contact et envisager des relances ciblées pour les non-répondants.")
                    .build());
            findings.add(KeyFindingDTO.builder()
                    .finding("Taux de réponse faible")
                    .evidence(String.format("%d contacts sur %d ont répondu (%.1f%%)", contacted, total, contactRate))
                    .impact("HIGH")
                    .build());
        } else if (contactRate >= 80) {
            findings.add(KeyFindingDTO.builder()
                    .finding("Excellent taux de réponse")
                    .evidence(String.format("%d contacts sur %d ont répondu (%.1f%%)", contacted, total, contactRate))
                    .impact("LOW")
                    .build());
        }

        // Analyze statistics data
        if (report.getStatisticsData() != null) {
            try {
                Map<String, Object> stats = objectMapper.readValue(
                        report.getStatisticsData(), new TypeReference<>() {});
                List<Map<String, Object>> summaryByQuestion =
                        (List<Map<String, Object>>) stats.get("summaryByQuestion");

                if (summaryByQuestion != null) {
                    analyzeQuestionStats(summaryByQuestion, recommendations, findings);
                }
            } catch (Exception e) {
                log.warn("Failed to parse statistics for fallback insights", e);
            }
        }

        // Default recommendation if none generated
        if (recommendations.isEmpty()) {
            recommendations.add(RecommendationDTO.builder()
                    .title("Continuer le suivi régulier")
                    .description("Les résultats sont dans les normes. Continuer le processus actuel.")
                    .priority("LOW")
                    .category("FOLLOW_UP")
                    .actionable("Planifier une revue trimestrielle des résultats d'enquête.")
                    .build());
        }

        // Build summary
        String summary = String.format(
                "Rapport basé sur %d contacts (%d réponses, taux %.1f%%). " +
                "%d observations clés identifiées avec %d recommandations d'amélioration.",
                total, contacted, contactRate, findings.size(), recommendations.size());

        return AiInsightDTO.builder()
                .reportId(report.getId())
                .summary(summary)
                .recommendations(recommendations)
                .keyFindings(findings)
                .sentimentAnalysis(SentimentAnalysisDTO.builder()
                        .positivePercent(0)
                        .neutralPercent(100)
                        .negativePercent(0)
                        .overallSentiment("NEUTRAL")
                        .build())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void analyzeQuestionStats(List<Map<String, Object>> summaryByQuestion,
                                       List<RecommendationDTO> recommendations,
                                       List<KeyFindingDTO> findings) {
        for (Map<String, Object> question : summaryByQuestion) {
            String type = (String) question.getOrDefault("type", "");
            String questionText = (String) question.getOrDefault("questionText", "Question");

            switch (type) {
                case "YES_OR_NO" -> {
                    Map<String, Object> counts = (Map<String, Object>) question.get("optionCounts");
                    if (counts != null) {
                        long yes = toLong(counts.get("Yes"));
                        long no = toLong(counts.get("No"));
                        long dTotal = yes + no;
                        if (dTotal > 0 && no > yes) {
                            findings.add(KeyFindingDTO.builder()
                                    .finding("Réponse majoritairement négative: " + questionText)
                                    .evidence(String.format("%d Non vs %d Oui", no, yes))
                                    .impact("HIGH")
                                    .build());
                            recommendations.add(RecommendationDTO.builder()
                                    .title("Investiguer les réponses négatives")
                                    .description("La question \"" + questionText +
                                            "\" a reçu une majorité de réponses négatives.")
                                    .priority("HIGH")
                                    .category("FOLLOW_UP")
                                    .actionable("Organiser des entretiens de suivi avec les répondants négatifs.")
                                    .build());
                        }
                    }
                }
                case "NUMBER" -> {
                    Map<String, Object> numStats = (Map<String, Object>) question.get("stats");
                    if (numStats != null) {
                        double avg = toDouble(numStats.get("average"));
                        double min = toDouble(numStats.get("min"));
                        double max = toDouble(numStats.get("max"));
                        if (max - min > avg * 2 && avg > 0) {
                            findings.add(KeyFindingDTO.builder()
                                    .finding("Grande dispersion des réponses: " + questionText)
                                    .evidence(String.format("Min: %.1f, Max: %.1f, Moyenne: %.1f", min, max, avg))
                                    .impact("MEDIUM")
                                    .build());
                        }
                    }
                }
                case "MULTIPLE_CHOICE", "DROPDOWN" -> {
                    Map<String, Object> optCounts = (Map<String, Object>) question.get("optionCounts");
                    if (optCounts != null && !optCounts.isEmpty()) {
                        Optional<Map.Entry<String, Object>> dominant = optCounts.entrySet().stream()
                                .max(Comparator.comparingLong(e -> toLong(e.getValue())));
                        long totalResponses = optCounts.values().stream().mapToLong(this::toLong).sum();
                        if (dominant.isPresent() && totalResponses > 0) {
                            double dominantPct = (toLong(dominant.get().getValue()) * 100.0) / totalResponses;
                            if (dominantPct > 70) {
                                findings.add(KeyFindingDTO.builder()
                                        .finding("Consensus fort pour: " + questionText)
                                        .evidence(String.format("%.0f%% ont choisi \"%s\"",
                                                dominantPct, dominant.get().getKey()))
                                        .impact("MEDIUM")
                                        .build());
                            }
                        }
                    }
                }
            }
        }
    }

    private long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return 0L;
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
}
