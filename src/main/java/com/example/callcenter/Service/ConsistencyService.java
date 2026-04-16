package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.ResponseRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import com.example.callcenter.client.ContactClient;
import com.example.callcenter.DTO.ContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsistencyService {

    private final RequestRepository requestRepository;
    private final SubmissionRepository submissionRepository;
    private final ResponseRepository responseRepository;
    private final ContactClient contactClient;

    /**
     * Analyze all contacts for a request and return consistency/completeness data.
     */
    public Map<String, Object> analyzeRequest(Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        List<Question> questions = new ArrayList<>(request.getQuestions());
        List<Submission> allSubmissions = submissionRepository.findByRequest(request);

        // Collect all contact IDs from submissions + request contacts
        Set<Long> allContactIds = new HashSet<>();
        if (request.getSubmissionList() != null) {
            request.getSubmissionList().forEach(s -> allContactIds.add(s.getContactId()));
        }
        allSubmissions.forEach(s -> allContactIds.add(s.getContactId()));

        int totalQuestions = questions.size();
        List<Map<String, Object>> contactAnalysis = new ArrayList<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        int totalAnswered = 0;
        int totalExpected = 0;

        for (Long contactId : allContactIds) {
            List<Submission> subs = submissionRepository.findAllByRequestIdRAndContactId(requestId, contactId);
            List<Response> responses = new ArrayList<>();
            if (!subs.isEmpty()) {
                responses = responseRepository.findBySubmission(subs.get(0));
            }

            // Contact name lookup
            String contactName = "Contact #" + contactId;
            try {
                ContactResponse c = contactClient.getContactById(contactId);
                if (c != null && c.getName() != null) contactName = c.getName();
            } catch (Exception ignored) {}

            // Find which questions have been answered
            Set<Long> answeredQuestionIds = responses.stream()
                    .filter(r -> r.getQuestion() != null)
                    .map(r -> r.getQuestion().getId())
                    .collect(Collectors.toSet());

            List<Map<String, Object>> missingQuestions = new ArrayList<>();
            List<Map<String, Object>> inconsistencies = new ArrayList<>();

            for (Question q : questions) {
                if (!answeredQuestionIds.contains(q.getId())) {
                    missingQuestions.add(Map.of(
                            "questionId", q.getId(),
                            "questionText", q.getText(),
                            "questionType", q.getQuestionType().name()
                    ));
                }
            }

            // Check for inconsistencies in existing responses
            for (Response r : responses) {
                if (r.getQuestion() == null) continue;
                Question q = r.getQuestion();
                String issue = checkResponseConsistency(q, r);
                if (issue != null) {
                    inconsistencies.add(Map.of(
                            "questionId", q.getId(),
                            "questionText", q.getText(),
                            "issue", issue
                    ));
                }
            }

            int answered = answeredQuestionIds.size();
            totalAnswered += answered;
            totalExpected += totalQuestions;

            double completionRate = totalQuestions > 0 ? (answered * 100.0) / totalQuestions : 100.0;

            Map<String, Object> contactData = new LinkedHashMap<>();
            contactData.put("contactId", contactId);
            contactData.put("contactName", contactName);
            contactData.put("answeredCount", answered);
            contactData.put("totalQuestions", totalQuestions);
            contactData.put("completionRate", Math.round(completionRate * 10.0) / 10.0);
            contactData.put("missingQuestions", missingQuestions);
            contactData.put("inconsistencies", inconsistencies);
            contactData.put("isComplete", missingQuestions.isEmpty() && inconsistencies.isEmpty());
            contactAnalysis.add(contactData);

            // Build issues list
            for (Map<String, Object> mq : missingQuestions) {
                issues.add(Map.of(
                        "type", "MISSING",
                        "severity", "WARNING",
                        "contactId", contactId,
                        "contactName", contactName,
                        "questionId", mq.get("questionId"),
                        "message", "Question manquante pour " + contactName + " : " + mq.get("questionText")
                ));
            }
            for (Map<String, Object> inc : inconsistencies) {
                issues.add(Map.of(
                        "type", "INCONSISTENT",
                        "severity", "ERROR",
                        "contactId", contactId,
                        "contactName", contactName,
                        "questionId", inc.get("questionId"),
                        "message", inc.get("issue")
                ));
            }
        }

        // Overall statistics
        double overallCompletion = totalExpected > 0 ? (totalAnswered * 100.0) / totalExpected : 0.0;
        long completeContacts = contactAnalysis.stream().filter(c -> (boolean) c.get("isComplete")).count();
        long incompleteContacts = contactAnalysis.size() - completeContacts;

        // Build summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requestId", requestId);
        summary.put("totalContacts", allContactIds.size());
        summary.put("totalQuestions", totalQuestions);
        summary.put("overallCompletionRate", Math.round(overallCompletion * 10.0) / 10.0);
        summary.put("completeContacts", completeContacts);
        summary.put("incompleteContacts", incompleteContacts);
        summary.put("totalIssues", issues.size());
        summary.put("canClose", issues.isEmpty());

        // Generate recommendation
        String recommendation = generateRecommendation(issues, allContactIds.size(), totalQuestions, overallCompletion);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("contacts", contactAnalysis);
        result.put("issues", issues);
        result.put("recommendation", recommendation);
        return result;
    }

    /**
     * Check a single response for inconsistencies.
     */
    private String checkResponseConsistency(Question q, Response r) {
        switch (q.getQuestionType()) {
            case NUMBER -> {
                if (r.getNumberAnswer() == null) {
                    return "Réponse numérique manquante pour \"" + q.getText() + "\"";
                }
            }
            case YES_OR_NO -> {
                if (r.getBooleanAnswer() == null) {
                    return "Réponse Oui/Non manquante pour \"" + q.getText() + "\"";
                }
            }
            case MULTIPLE_CHOICE, DROPDOWN -> {
                if (r.getAnswer() == null || r.getAnswer().isBlank()) {
                    return "Aucune option sélectionnée pour \"" + q.getText() + "\"";
                }
                if (q.getOptions() != null && !q.getOptions().isEmpty() && !q.getOptions().contains(r.getAnswer())) {
                    return "Option invalide \"" + r.getAnswer() + "\" pour \"" + q.getText() + "\"";
                }
            }
            case CHECKBOXES -> {
                if (r.getMultiAnswer() == null || r.getMultiAnswer().isEmpty()) {
                    return "Aucune case cochée pour \"" + q.getText() + "\"";
                }
            }
            case SHORT_ANSWER, PARAGRAPH -> {
                if (r.getAnswer() == null || r.getAnswer().isBlank()) {
                    return "Réponse textuelle vide pour \"" + q.getText() + "\"";
                }
            }
            case DATE -> {
                if (r.getDateAnswer() == null) {
                    return "Date manquante pour \"" + q.getText() + "\"";
                }
            }
            case TIME -> {
                if (r.getTimeAnswer() == null) {
                    return "Heure manquante pour \"" + q.getText() + "\"";
                }
            }
        }
        return null;
    }

    /**
     * Generate a natural-language recommendation.
     */
    private String generateRecommendation(List<Map<String, Object>> issues, int totalContacts, int totalQuestions, double completion) {
        if (issues.isEmpty()) {
            return "✅ Toutes les réponses sont complètes et cohérentes. Vous pouvez clôturer cette demande.";
        }

        long missingCount = issues.stream().filter(i -> "MISSING".equals(i.get("type"))).count();
        long inconsistentCount = issues.stream().filter(i -> "INCONSISTENT".equals(i.get("type"))).count();

        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ ");

        if (missingCount > 0) {
            sb.append(String.format("%d question(s) manquante(s)", missingCount));
        }
        if (inconsistentCount > 0) {
            if (missingCount > 0) sb.append(" et ");
            sb.append(String.format("%d réponse(s) incohérente(s)", inconsistentCount));
        }

        sb.append(String.format(" détectée(s). Taux de complétion : %.0f%%. ", completion));
        sb.append("Veuillez corriger ces problèmes avant de clôturer la demande.");

        return sb.toString();
    }
}
