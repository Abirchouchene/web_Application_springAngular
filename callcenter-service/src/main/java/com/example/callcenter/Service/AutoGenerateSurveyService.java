package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoGenerateSurveyService {

    private final RequestRepository requestRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    /**
     * Auto-generates satisfaction surveys every day at 8:00 AM.
     * Creates an AUTO_GENERATED request for the RECLAMATION category
     * so agents can handle it without manual creation.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional
    public void autoGenerateWeeklySurveys() {
        log.info("Starting auto-generation of weekly surveys...");

        try {
            // Find the system/admin user (first user or ID 1)
            User systemUser = userRepository.findById(1L)
                    .orElse(null);
            if (systemUser == null) {
                log.warn("No system user found (ID=1), skipping auto-generation");
                return;
            }

            // Get questions from RECLAMATION category for auto-generated surveys
            List<Question> reclamationQuestions = requestRepository.findQuestionsByReclamationRequests();

            // Create auto-generated satisfaction survey
            Request autoRequest = new Request();
            autoRequest.setTitle("Enquête de satisfaction automatique - " + LocalDate.now());
            autoRequest.setDescription("Enquête générée automatiquement pour le suivi de la satisfaction client.");
            autoRequest.setStatus(Status.AUTO_GENERATED);
            autoRequest.setRequestType(RequestType.STATISTICS);
            autoRequest.setCategoryRequest(CategoryRequest.RECLAMATION);
            autoRequest.setPriority(Priority.MEDIUM);
            autoRequest.setDeadline(LocalDate.now().plusDays(7));
            autoRequest.setUser(systemUser);

            if (!reclamationQuestions.isEmpty()) {
                autoRequest.setQuestions(new ArrayList<>(reclamationQuestions));
            }

            requestRepository.save(autoRequest);
            log.info("Auto-generated survey created: {}", autoRequest.getIdR());

        } catch (Exception e) {
            log.error("Error during auto-generation of surveys: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for auto-generating a survey (callable from controller).
     */
    @Transactional
    public Request generateSurveyNow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Question> reclamationQuestions = requestRepository.findQuestionsByReclamationRequests();

        Request autoRequest = new Request();
        autoRequest.setTitle("Enquête de satisfaction - " + LocalDate.now());
        autoRequest.setDescription("Enquête générée manuellement par le manager.");
        autoRequest.setStatus(Status.AUTO_GENERATED);
        autoRequest.setRequestType(RequestType.STATISTICS);
        autoRequest.setCategoryRequest(CategoryRequest.RECLAMATION);
        autoRequest.setPriority(Priority.MEDIUM);
        autoRequest.setDeadline(LocalDate.now().plusDays(7));
        autoRequest.setUser(user);

        if (!reclamationQuestions.isEmpty()) {
            autoRequest.setQuestions(new ArrayList<>(reclamationQuestions));
        }

        return requestRepository.save(autoRequest);
    }
}
