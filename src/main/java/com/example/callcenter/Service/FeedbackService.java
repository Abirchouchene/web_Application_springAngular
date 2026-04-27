package com.example.callcenter.Service;

import com.example.callcenter.DTO.FeedbackDTO;
import com.example.callcenter.Entity.Feedback;
import com.example.callcenter.Entity.Report;
import com.example.callcenter.Repository.FeedbackRepository;
import com.example.callcenter.Repository.ReportRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final WebClient webClient;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    @Value("classpath:prompts/feedback-analysis-prompt.txt")
    private Resource promptResource;

    private String systemPrompt;

    @PostConstruct
    void loadPrompt() {
        try {
            systemPrompt = new String(
                    FileCopyUtils.copyToByteArray(promptResource.getInputStream()),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load feedback analysis prompt", e);
            systemPrompt = "";
        }
    }

    public FeedbackService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            FeedbackRepository feedbackRepository,
            ReportRepository reportRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reportRepository = reportRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    public FeedbackDTO submitFeedback(Long reportId, Integer rating, String comment, Long userId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rapport introuvable : " + reportId));

        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5");
        }

        Feedback feedback = (userId != null)
                ? feedbackRepository.findByReport_IdAndUserId(reportId, userId).orElse(new Feedback())
                : new Feedback();

        feedback.setReport(report);
        feedback.setUserId(userId);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setSubmittedAt(LocalDateTime.now());
        feedback.setAiAnalysis(null);
        feedback.setAiAnalysisDate(null);

        feedback = feedbackRepository.save(feedback);

        if (enabled) {
            try {
                String analysis = callOpenAiAnalysis(rating, comment, report.getRequestTitle());
                feedback.setAiAnalysis(analysis);
                feedback.setAiAnalysisDate(LocalDateTime.now());
                feedback = feedbackRepository.save(feedback);
            } catch (Exception e) {
                log.warn("AI feedback analysis failed, storing feedback without AI: {}", e.getMessage());
            }
        }

        return toDTO(feedback);
    }

    public List<FeedbackDTO> getFeedbackByReport(Long reportId) {
        return feedbackRepository.findByReport_Id(reportId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FeedbackDTO getFeedbackByReportAndUser(Long reportId, Long userId) {
        return feedbackRepository.findByReport_IdAndUserId(reportId, userId)
                .map(this::toDTO)
                .orElse(null);
    }

    public FeedbackDTO triggerAiAnalysis(Long feedbackId) {
        if (feedbackId == null) throw new RuntimeException("feedbackId requis");
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback introuvable : " + feedbackId));

        if (!enabled) {
            throw new RuntimeException("L'analyse IA n'est pas activée");
        }

        String reportTitle = feedback.getReport() != null ? feedback.getReport().getRequestTitle() : "";
        String analysis = callOpenAiAnalysis(feedback.getRating(), feedback.getComment(), reportTitle);
        feedback.setAiAnalysis(analysis);
        feedback.setAiAnalysisDate(LocalDateTime.now());
        feedback = feedbackRepository.save(feedback);
        return toDTO(feedback);
    }

    @SuppressWarnings("unchecked")
    private String callOpenAiAnalysis(int rating, String comment, String reportTitle) {
        String userContent = "Rapport : " + (reportTitle != null ? reportTitle : "N/A")
                + "\nNote : " + rating + "/5"
                + "\nCommentaire : " + (comment != null && !comment.trim().isEmpty() ? comment : "(aucun commentaire)");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userContent);
        messages.add(userMsg);

        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 500);
        body.put("response_format", responseFormat);

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("Réponse vide d'OpenAI");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("Aucun choix dans la réponse OpenAI");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private FeedbackDTO toDTO(Feedback f) {
        String title = (f.getReport() != null) ? f.getReport().getRequestTitle() : null;
        Long reportId = (f.getReport() != null) ? f.getReport().getId() : null;
        return FeedbackDTO.builder()
                .id(f.getId())
                .reportId(reportId)
                .reportTitle(title)
                .userId(f.getUserId())
                .rating(f.getRating())
                .comment(f.getComment())
                .submittedAt(f.getSubmittedAt())
                .aiAnalysis(f.getAiAnalysis())
                .aiAnalysisDate(f.getAiAnalysisDate())
                .aiAnalyzed(f.getAiAnalysis() != null)
                .build();
    }
}
