package com.example.callcenter.Service;

import com.example.callcenter.DTO.QualityEvaluationDTO.*;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QualityEvaluationService {

    private final ReportRepository reportRepository;
    private final RequestRepository requestRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("classpath:prompts/quality-evaluation-prompt.txt")
    private Resource promptResource;

    private String systemPrompt;

    @PostConstruct
    void loadPrompt() {
        try {
            systemPrompt = new String(FileCopyUtils.copyToByteArray(promptResource.getInputStream()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load quality evaluation prompt, AI generation will be unavailable", e);
            systemPrompt = "";
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    public EvaluationForm generateForm(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        Request request = requestRepository.findByReport_Id(reportId).orElse(null);

        if (request == null) {
            log.warn("No request linked to report {}, returning default evaluation form", reportId);
            return generateDefault(report);
        }

        if (openAiEnabled && apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return generateWithAI(report, request);
            } catch (Exception e) {
                log.warn("AI quality evaluation failed, using rule-based fallback: {}", e.getMessage());
            }
        }
        return generateRuleBased(report, request);
    }

    private EvaluationForm generateDefault(Report report) {
        List<RatingQuestion> ratings = new ArrayList<>();
        ratings.add(new RatingQuestion("response_quality",      "Qualité des réponses apportées",  "star"));
        ratings.add(new RatingQuestion("resolution_speed",      "Rapidité de traitement",           "clock"));
        ratings.add(new RatingQuestion("communication_clarity", "Clarté de communication",          "message-circle"));
        ratings.add(new RatingQuestion("overall_satisfaction",  "Satisfaction globale",             "heart"));

        List<BinaryQuestion> binaryQuestions = new ArrayList<>();
        binaryQuestions.add(new BinaryQuestion("issue_resolved",      "Problème résolu ?"));
        binaryQuestions.add(new BinaryQuestion("communication_clear", "Communication claire ?"));
        binaryQuestions.add(new BinaryQuestion("needs_followup",      "Besoin de relance ?"));

        return EvaluationForm.builder()
                .reportTitle(report.getRequestTitle())
                .contextHint("Évaluation standard de la qualité de service")
                .ratings(ratings)
                .binaryQuestions(binaryQuestions)
                .openQuestion("Qu'est-ce qui pourrait être amélioré dans le traitement de votre demande ?")
                .build();
    }

    public EvaluationResult processSubmission(Long reportId, Long userId, EvaluationSubmission submission) {
        submission.setReportId(reportId);
        submission.setSubmittedAt(java.time.LocalDateTime.now());

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        double avg = submission.getRatings() == null || submission.getRatings().isEmpty() ? 0
                : submission.getRatings().values().stream()
                        .mapToInt(Integer::intValue).average().orElse(0);
        double rounded = Math.round(avg * 10.0) / 10.0;
        String level = avg >= 4.5 ? "EXCELLENT" : avg >= 3.5 ? "GOOD" : avg >= 2.5 ? "AVERAGE" : "POOR";

        // Persist — upsert: one record per user per report (new record if no userId)
        EvaluationRecord record = (userId != null)
                ? evaluationRecordRepository.findByReport_IdAndUserId(reportId, userId).orElse(new EvaluationRecord())
                : new EvaluationRecord();
        record.setReport(report);
        record.setUserId(userId);
        record.setAverageRating(rounded);
        record.setQualityLevel(level);
        record.setOpenAnswer(submission.getOpenAnswer());
        record.setSubmittedAt(submission.getSubmittedAt());
        try {
            record.setRatingsJson(objectMapper.writeValueAsString(
                    submission.getRatings() != null ? submission.getRatings() : Collections.emptyMap()));
            record.setBinaryAnswersJson(objectMapper.writeValueAsString(
                    submission.getBinaryAnswers() != null ? submission.getBinaryAnswers() : Collections.emptyMap()));
        } catch (Exception e) {
            log.warn("Could not serialize evaluation maps: {}", e.getMessage());
        }
        record = evaluationRecordRepository.save(record);

        log.info("Evaluation saved id={} report={} user={} avg={} level={}",
                record.getId(), reportId, userId, rounded, level);

        return EvaluationResult.builder()
                .success(true)
                .message("Évaluation enregistrée. Merci pour votre retour !")
                .averageRating(rounded)
                .qualityLevel(level)
                .recordId(record.getId())
                .build();
    }

    // ── Rule-based fallback ──────────────────────────────────────────────────

    private EvaluationForm generateRuleBased(Report report, Request request) {
        double contactRate = report.getContactRate() != null ? report.getContactRate() : 0;
        String category   = request.getCategoryRequest() != null ? request.getCategoryRequest().name() : "";
        String priority   = request.getPriority()        != null ? request.getPriority().name()        : "";
        boolean deadlinePassed = request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now());

        List<RatingQuestion> ratings = new ArrayList<>();
        ratings.add(new RatingQuestion("response_quality",      "Qualité des réponses apportées",  "star"));
        ratings.add(new RatingQuestion("resolution_speed",      "Rapidité de traitement",           "clock"));
        ratings.add(new RatingQuestion("communication_clarity", "Clarté de communication",          "message-circle"));
        ratings.add(new RatingQuestion("overall_satisfaction",  "Satisfaction globale",             "heart"));

        List<BinaryQuestion> binaryQuestions = new ArrayList<>();
        binaryQuestions.add(new BinaryQuestion("issue_resolved",      "Problème résolu ?"));
        binaryQuestions.add(new BinaryQuestion("communication_clear", "Communication claire ?"));
        binaryQuestions.add(new BinaryQuestion("needs_followup",      "Besoin de relance ?"));

        return EvaluationForm.builder()
                .reportTitle(report.getRequestTitle())
                .contextHint(buildContextHint(contactRate, priority, deadlinePassed, category))
                .ratings(ratings)
                .binaryQuestions(binaryQuestions)
                .openQuestion(buildOpenQuestion(contactRate, priority, deadlinePassed, category))
                .build();
    }

    private String buildContextHint(double rate, String priority, boolean deadlinePassed, String category) {
        if (deadlinePassed)                                   return "Délai dépassé — focus sur les impacts de la latence";
        if (rate < 50)                                        return "Taux de contact faible — focus sur la réactivité";
        if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) return "Demande prioritaire — évaluation de la rapidité";
        if ("RECLAMATION".equals(category))                   return "Réclamation — focus sur la résolution du problème";
        return "Évaluation standard de la qualité de service";
    }

    private String buildOpenQuestion(double rate, String priority, boolean deadlinePassed, String category) {
        if (deadlinePassed)                                   return "Qu'est-ce qui aurait pu être fait différemment pour respecter le délai initial ?";
        if (rate < 50)                                        return "Quelles difficultés avez-vous rencontrées lors de la prise de contact avec nos équipes ?";
        if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) return "La demande a été traitée en priorité — qu'est-ce qui pourrait encore être amélioré ?";
        if ("RECLAMATION".equals(category))                   return "Comment évaluez-vous la façon dont votre réclamation a été prise en charge ?";
        return "Qu'est-ce qui pourrait être amélioré dans le traitement de votre demande ?";
    }

    // ── AI generation ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private EvaluationForm generateWithAI(Report report, Request request) throws Exception {
        String contextJson = buildContextJson(report, request);
        String prompt = String.format(systemPrompt, contextJson);

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", prompt);
        messages.add(sysMsg);
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "Generate the evaluation form JSON now.");
        messages.add(userMsg);

        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 600);
        body.put("response_format", responseFormat);

        Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        Map<String, Object> json = objectMapper.readValue(content, Map.class);
        return EvaluationForm.builder()
                .reportTitle(report.getRequestTitle())
                .contextHint((String) json.getOrDefault("contextHint", "Évaluation générée par IA"))
                .ratings(parseRatings(json))
                .binaryQuestions(parseBinary(json))
                .openQuestion((String) json.getOrDefault("openQuestion", "Qu'est-ce qui pourrait être amélioré ?"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<RatingQuestion> parseRatings(Map<String, Object> json) {
        List<Map<String, String>> raw = (List<Map<String, String>>) json.get("ratings");
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(m -> new RatingQuestion(m.get("id"), m.get("label"), m.getOrDefault("icon", "star")))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<BinaryQuestion> parseBinary(Map<String, Object> json) {
        List<Map<String, String>> raw = (List<Map<String, String>>) json.get("binaryQuestions");
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                .map(m -> new BinaryQuestion(m.get("id"), m.get("label")))
                .collect(Collectors.toList());
    }

    private String buildContextJson(Report report, Request request) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("requestTitle",   report.getRequestTitle());
        ctx.put("requestType",    request.getRequestType()      != null ? request.getRequestType().name()      : null);
        ctx.put("category",       request.getCategoryRequest()  != null ? request.getCategoryRequest().name()  : null);
        ctx.put("priority",       request.getPriority()         != null ? request.getPriority().name()         : null);
        ctx.put("status",         request.getStatus()           != null ? request.getStatus().name()           : null);
        ctx.put("totalContacts",  report.getTotalContacts());
        ctx.put("contactedContacts", report.getContactedContacts());
        ctx.put("contactRate",    report.getContactRate());
        ctx.put("deadline",       request.getDeadline());
        ctx.put("deadlinePassed", request.getDeadline() != null && request.getDeadline().isBefore(LocalDate.now()));
        try {
            return objectMapper.writeValueAsString(ctx);
        } catch (Exception e) {
            return ctx.toString();
        }
    }
}
