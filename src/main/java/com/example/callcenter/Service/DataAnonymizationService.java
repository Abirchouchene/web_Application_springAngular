package com.example.callcenter.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Anonymizes survey data before sending to external AI services.
 * Removes PII (names, phone numbers, emails, IDs) while preserving
 * the statistical structure needed for analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataAnonymizationService {

    private final ObjectMapper objectMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?\\d[\\d\\s\\-()]{6,}\\d");
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?i)\\b(mr|mrs|ms|dr|prof)\\.?\\s+[A-Z][a-z]+");

    /**
     * Anonymizes the full statistics JSON for safe external transmission.
     * Strips contact IDs, names, and scrubs free-text answers of PII.
     */
    public String anonymizeStatistics(String statisticsJson) {
        if (statisticsJson == null || statisticsJson.isBlank()) {
            return statisticsJson;
        }

        try {
            Map<String, Object> stats = objectMapper.readValue(
                    statisticsJson, new TypeReference<>() {});

            // Remove per-contact breakdown entirely (contains contactId)
            stats.remove("byContact");

            // Anonymize free-text responses in summaryByQuestion
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> summaryByQuestion =
                    (List<Map<String, Object>>) stats.get("summaryByQuestion");

            if (summaryByQuestion != null) {
                for (Map<String, Object> question : summaryByQuestion) {
                    anonymizeQuestionData(question);
                }
            }

            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            log.warn("Failed to anonymize statistics, returning stripped version", e);
            return stripAllPii(statisticsJson);
        }
    }

    @SuppressWarnings("unchecked")
    private void anonymizeQuestionData(Map<String, Object> question) {
        String type = (String) question.getOrDefault("type", "");

        // For text-based questions, scrub PII from individual responses
        if ("SHORT_ANSWER".equals(type) || "PARAGRAPH".equals(type)) {
            List<String> responses = (List<String>) question.get("responses");
            if (responses != null) {
                List<String> scrubbed = responses.stream()
                        .map(this::scrubPii)
                        .collect(Collectors.toList());
                question.put("responses", scrubbed);
            }
        }

        // Remove questionId (internal DB reference)
        question.remove("questionId");
    }

    /**
     * Scrubs PII patterns from a free-text string.
     */
    private String scrubPii(String text) {
        if (text == null) return null;
        String result = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE]");
        result = NAME_PATTERN.matcher(result).replaceAll("[NAME]");
        return result;
    }

    /**
     * Fallback: aggressively strip anything that looks like PII.
     */
    private String stripAllPii(String text) {
        String result = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL]");
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE]");
        return result;
    }

    /**
     * Build a safe, anonymized prompt context from report data.
     * Only includes aggregate statistics and anonymized text responses.
     */
    public String buildAnonymizedContext(String requestTitle, String requestType,
                                          int totalContacts, int contactedContacts,
                                          double contactRate, String statisticsJson) {
        String anonymizedStats = anonymizeStatistics(statisticsJson);

        StringBuilder context = new StringBuilder();
        context.append("Survey Title: ").append(requestTitle != null ? requestTitle : "Untitled").append("\n");
        context.append("Survey Type: ").append(requestType != null ? requestType : "N/A").append("\n");
        context.append("Total Contacts: ").append(totalContacts).append("\n");
        context.append("Contacted: ").append(contactedContacts).append("\n");
        context.append("Response Rate: ").append(String.format("%.1f%%", contactRate)).append("\n\n");
        context.append("Anonymized Survey Data:\n").append(anonymizedStats);

        return context.toString();
    }
}
