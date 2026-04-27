package com.example.callcenter.Service;

import com.example.callcenter.DTO.AiChatDTO.*;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ReportRepository;
import com.example.callcenter.Repository.RequestContactStatusRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import com.example.callcenter.Repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Per-survey AI assistant for requesters (SURVEY_REQUESTER role).
 * Answers questions grounded in a specific request's own survey data (responses,
 * contact stats, report) — not global dashboard metrics.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyAssistantService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final RequestContactStatusRepository contactStatusRepository;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("classpath:prompts/survey-assistant-prompt.txt")
    private Resource promptResource;

    private String systemPromptTemplate;

    @PostConstruct
    void loadPrompt() {
        try {
            systemPromptTemplate = new String(FileCopyUtils.copyToByteArray(promptResource.getInputStream()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load survey assistant prompt", e);
            systemPromptTemplate = "You are a survey analyst. Context JSON:\n%s";
        }
    }

    private static final int MAX_HISTORY = 10;
    private final Map<String, List<Map<String, String>>> sessionHistory = new ConcurrentHashMap<>();
    private final Map<String, Integer> summaryTurnCounter = new ConcurrentHashMap<>();

    /** Ask about a survey identified by its REPORT id (looks up the underlying request). */
    public ChatResponse askByReportId(Long reportId, ChatRequest req, String username) {
        // The Request table owns the FK (report_id) — query it directly, not the lazy inverse
        Request request = requestRepository.findByReport_Id(reportId)
                .orElseThrow(() -> new RuntimeException("No request linked to report " + reportId));
        return askQuestion(request.getIdR(), req, username);
    }

    public ChatResponse askQuestion(Long requestId, ChatRequest req, String username) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        // Ownership check — only the requester (or admin/manager, enforced upstream if needed)
        User authUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        boolean isOwner = request.getUser() != null
                && request.getUser().getIdUser().equals(authUser.getIdUser());
        boolean isPrivileged = authUser.getRole() == Role.ADMIN
                || authUser.getRole() == Role.MANAGER;
        if (!isOwner && !isPrivileged) {
            throw new AccessDeniedException("You are not authorized to access this request.");
        }

        String sessionId = req.getSessionId() != null ? req.getSessionId() : UUID.randomUUID().toString();
        String surveyData = buildSurveyDataJson(request);

        if (openAiEnabled && apiKey != null && !apiKey.isEmpty()) {
            try {
                return callOpenAi(req.getMessage(), surveyData, sessionId);
            } catch (Exception e) {
                log.warn("OpenAI survey assistant failed, falling back: {}", e.getMessage());
            }
        }
        return ruleBasedAnswer(req.getMessage(), request, sessionId);
    }

    @SuppressWarnings("unchecked")
    private ChatResponse callOpenAi(String userQuestion, String surveyDataJson, String sessionId) {
        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        String systemPrompt = String.format(systemPromptTemplate, surveyDataJson);

        List<Map<String, String>> history = sessionHistory.computeIfAbsent(
                sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        synchronized (history) {
            messages.addAll(history);
        }

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userQuestion);
        messages.add(userMsg);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 600);

        Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        synchronized (history) {
            Map<String, String> uEntry = new HashMap<>();
            uEntry.put("role", "user");
            uEntry.put("content", userQuestion);
            history.add(uEntry);

            Map<String, String> aEntry = new HashMap<>();
            aEntry.put("role", "assistant");
            aEntry.put("content", content);
            history.add(aEntry);

            while (history.size() > MAX_HISTORY * 2) history.remove(0);
        }

        return ChatResponse.builder()
                .message(content)
                .sessionId(sessionId)
                .type("text")
                .timestamp(LocalDateTime.now())
                .suggestedActions(Collections.emptyList())
                .build();
    }

    private String buildSurveyDataJson(Request request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", request.getIdR());
        data.put("title", request.getTitle());
        data.put("description", request.getDescription());
        data.put("requestType", request.getRequestType() != null ? request.getRequestType().name() : null);
        data.put("category", request.getCategoryRequest() != null ? request.getCategoryRequest().name() : null);
        data.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        data.put("priority", request.getPriority() != null ? request.getPriority().name() : null);
        data.put("createdAt", request.getCreatedAt());
        data.put("deadline", request.getDeadline());

        // Contact stats
        List<RequestContactStatus> statuses = contactStatusRepository.findByRequestIdR(request.getIdR());
        Map<String, Object> contactStats = new LinkedHashMap<>();
        contactStats.put("total", statuses.size());
        Map<String, Long> byStatus = statuses.stream()
                .filter(s -> s.getStatus() != null)
                .collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));
        contactStats.put("byStatus", byStatus);
        long contacted = statuses.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() != ContactStatus.NOT_CONTACTED)
                .count();
        contactStats.put("contacted", contacted);
        contactStats.put("contactRatePercent", statuses.isEmpty() ? 0
                : Math.round(contacted * 1000.0 / statuses.size()) / 10.0);
        data.put("contacts", contactStats);

        // Questions
        if (request.getQuestions() != null) {
            List<Map<String, Object>> questions = request.getQuestions().stream().map(q -> {
                Map<String, Object> qm = new LinkedHashMap<>();
                qm.put("id", q.getId());
                qm.put("text", q.getText());
                qm.put("type", q.getQuestionType() != null ? q.getQuestionType().name() : null);
                if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                    qm.put("options", q.getOptions());
                }
                return qm;
            }).collect(Collectors.toList());
            data.put("questions", questions);
        }

        // Responses — aggregate by question
        List<Submission> submissions = submissionRepository.findByRequestIdR(request.getIdR());
        Map<Long, List<Map<String, Object>>> responsesByQuestion = new HashMap<>();
        for (Submission sub : submissions) {
            if (sub.getResponses() == null) continue;
            for (Response resp : sub.getResponses()) {
                if (resp.getQuestion() == null) continue;
                Long qid = resp.getQuestion().getId();
                Map<String, Object> answer = new LinkedHashMap<>();
                answer.put("contactId", sub.getContactId());
                if (resp.getAnswer() != null) answer.put("answer", resp.getAnswer());
                if (resp.getMultiAnswer() != null && !resp.getMultiAnswer().isEmpty())
                    answer.put("multiAnswer", resp.getMultiAnswer());
                if (resp.getBooleanAnswer() != null) answer.put("boolean", resp.getBooleanAnswer());
                if (resp.getNumberAnswer() != null) answer.put("number", resp.getNumberAnswer());
                if (resp.getDateAnswer() != null) answer.put("date", resp.getDateAnswer());
                if (resp.getTimeAnswer() != null) answer.put("time", resp.getTimeAnswer());
                responsesByQuestion.computeIfAbsent(qid, k -> new ArrayList<>()).add(answer);
            }
        }
        data.put("responsesByQuestion", responsesByQuestion);
        data.put("totalSubmissions", submissions.size());

        // Report summary
        if (request.getReport() != null) {
            Report r = request.getReport();
            Map<String, Object> reportInfo = new LinkedHashMap<>();
            reportInfo.put("id", r.getId());
            reportInfo.put("status", r.getStatus() != null ? r.getStatus().name() : null);
            reportInfo.put("totalContacts", r.getTotalContacts());
            reportInfo.put("contactedContacts", r.getContactedContacts());
            reportInfo.put("contactRate", r.getContactRate());
            reportInfo.put("generatedDate", r.getGeneratedDate());
            data.put("report", reportInfo);
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize survey data, using toString: {}", e.getMessage());
            return data.toString();
        }
    }

    private ChatResponse ruleBasedAnswer(String question, Request request, String sessionId) {
        String lc = question.toLowerCase();
        List<RequestContactStatus> statuses = contactStatusRepository.findByRequestIdR(request.getIdR());
        List<Submission> subs = submissionRepository.findByRequestIdR(request.getIdR());
        int totalContacts = statuses.size();
        long contacted = statuses.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() != ContactStatus.NOT_CONTACTED)
                .count();
        double rate = totalContacts > 0 ? (contacted * 100.0) / totalContacts : 0;
        int totalSubs = subs.size();
        int nbQuestions = request.getQuestions() != null ? request.getQuestions().size() : 0;

        // 1. Privacy guard — user asks about OTHER requests/users
        if (matches(lc, "autres demandes", "autres utilisateurs", "other users", "other request",
                "other survey", "d'autres", "autre demande")) {
            return reply("Cette discussion est limitée à votre propre enquête. Pour consulter d'autres demandes, veuillez contacter un administrateur.", sessionId);
        }

        // 2. Greetings / help
        if (matches(lc, "bonjour", "salut", "hello", "hey", "aide", "help")) {
            return reply("Bonjour ! Je suis votre assistant d'enquête. Posez-moi une question sur : statut, taux de contact, réponses, tendances, recommandations, ou questions du sondage.", sessionId);
        }

        // 3. Trends / analytical
        if (matches(lc, "tendance", "trend", "pattern", "evolution", "évolution", "analyse",
                "analysis", "insight", "retour", "feedback", "sentiment", "satisfaction")) {
            return reply(buildTrendInsight(request, statuses, subs, totalContacts, contacted, rate), sessionId);
        }

        // 4. Recommendations
        if (matches(lc, "recommandation", "conseil", "suggest", "améliorer", "optimiser",
                "action", "que faire", "quoi faire", "améliore")) {
            return reply(buildRecommendations(request, totalContacts, contacted, rate, totalSubs, nbQuestions), sessionId);
        }

        // 5. Questions in the survey
        if (matches(lc, "questions du", "quelles questions", "liste des questions",
                "sondage", "les questions", "types de question")) {
            return reply(buildQuestionsList(request), sessionId);
        }

        // 6. Priority / category
        if (matches(lc, "priorit", "urgent", "urgence", "critique", "catégori", "categorie", "categor")) {
            return reply(String.format("• Priorité : **%s**%n• Catégorie : **%s**%n• Type : **%s**",
                    safe(request.getPriority()), safe(request.getCategoryRequest()), safe(request.getRequestType())), sessionId);
        }

        // 7. Deadline / timeline
        if (matches(lc, "deadline", "échéance", "echeance", "delai", "délai", "date limite",
                "quand", "jours restants", "restant")) {
            return reply(buildTimelineInsight(request), sessionId);
        }

        // 8. Contact rate / counts
        if (matches(lc, "taux", "rate", "pourcent", "percent", "contact", "joint", "répondu", "réponse jointe")) {
            return reply(buildContactInsight(totalContacts, contacted, rate, subs, statuses), sessionId);
        }

        // 9. Responses
        if (matches(lc, "répon", "respon", "reponse", "answer", "résultat", "result", "données collectées")) {
            return reply(buildResponsesInsight(subs, nbQuestions, request), sessionId);
        }

        // 10. Status / state
        if (matches(lc, "statut", "status", "état", "etat", "state", "avancement")) {
            return reply(String.format("• Statut : **%s**%n• Priorité : %s%n• Rapport : %s",
                    safe(request.getStatus()),
                    safe(request.getPriority()),
                    request.getReport() != null ? request.getReport().getStatus() : "Non généré"), sessionId);
        }

        // 11. Summary / overview — rotates perspective each time the user asks again
        if (matches(lc, "résumé", "resume", "summary", "aperçu", "apercu", "vue d'ensemble", "panorama")) {
            return reply(buildRotatingSummary(sessionId, request, totalContacts, contacted, rate, totalSubs, nbQuestions), sessionId);
        }

        // 12. Vague / unknown — per spec, prompt for clarification (do NOT return a generic summary)
        return reply("Veuillez préciser : statut, réponses, taux de contact, tendances, recommandations, ou résumé.", sessionId);
    }

    // ========== intent builders ==========

    private String buildTrendInsight(Request r, List<RequestContactStatus> statuses,
                                      List<Submission> subs, int totalContacts, long contacted, double rate) {
        StringBuilder sb = new StringBuilder("**Analyse des tendances**\n\n");
        // Engagement level from contact rate
        if (totalContacts == 0) {
            sb.append("• Aucun contact associé à cette enquête — impossible de dégager une tendance d'engagement.\n");
        } else if (rate == 0) {
            sb.append("• Engagement nul : aucun des ").append(totalContacts).append(" contacts n'a encore été joint.\n");
        } else if (rate < 30) {
            sb.append("• Engagement faible (").append(String.format("%.0f%%", rate)).append(") — la majorité des contacts reste à solliciter.\n");
        } else if (rate < 70) {
            sb.append("• Engagement modéré (").append(String.format("%.0f%%", rate)).append(") — l'enquête progresse mais nécessite un suivi.\n");
        } else {
            sb.append("• Fort engagement (").append(String.format("%.0f%%", rate)).append(") — la couverture est bonne.\n");
        }
        // Response volume vs contacts
        if (totalContacts > 0) {
            double responseDensity = (subs.size() * 100.0) / totalContacts;
            sb.append(String.format("• Densité de réponse : %.0f%% (%d réponses / %d contacts).%n", responseDensity, subs.size(), totalContacts));
        }
        // Status distribution
        Map<ContactStatus, Long> byStatus = statuses.stream()
                .filter(s -> s.getStatus() != null)
                .collect(Collectors.groupingBy(RequestContactStatus::getStatus, Collectors.counting()));
        if (!byStatus.isEmpty()) {
            sb.append("• Répartition : ");
            sb.append(byStatus.entrySet().stream()
                    .map(e -> e.getValue() + " " + friendlyStatus(e.getKey()))
                    .collect(Collectors.joining(", ")));
            sb.append(".\n");
        }
        // Actionable closer
        if (rate < 50) sb.append("\n➡️ Tendance à surveiller : taux de contact sous la moyenne attendue.");
        else if (subs.size() < totalContacts / 2) sb.append("\n➡️ Écart entre contacts joints et réponses collectées.");
        else sb.append("\n➡️ Dynamique positive — continuez sur ce rythme.");
        return sb.toString();
    }

    private String buildRecommendations(Request r, int totalContacts, long contacted,
                                         double rate, int totalSubs, int nbQuestions) {
        StringBuilder sb = new StringBuilder("**Recommandations**\n\n");
        int n = 1;
        if (totalContacts == 0) {
            sb.append(n++).append(". Ajouter des contacts à l'enquête avant de lancer la collecte.\n");
        }
        if (rate < 50 && totalContacts > 0) {
            sb.append(n++).append(". Relancer les ").append(totalContacts - contacted).append(" contacts non joints (taux actuel ").append(String.format("%.0f%%", rate)).append(").\n");
        }
        if (totalSubs == 0 && contacted > 0) {
            sb.append(n++).append(". Des contacts ont été joints mais aucune réponse n'a été soumise — vérifier le processus de saisie.\n");
        }
        if (r.getStatus() == Status.PENDING) {
            sb.append(n++).append(". La demande est encore en attente (PENDING) — demander la validation du manager.\n");
        }
        if (nbQuestions < 3) {
            sb.append(n++).append(". Enquête courte (").append(nbQuestions).append(" questions) — envisager d'enrichir le questionnaire si besoin.\n");
        }
        if (r.getDeadline() != null && r.getDeadline().isBefore(LocalDate.now())) {
            sb.append(n++).append(". ⚠ Échéance dépassée : revoir la date limite ou clôturer la demande.\n");
        }
        if (n == 1) sb.append("Aucune action critique identifiée — l'enquête est sur une bonne dynamique.");
        return sb.toString();
    }

    private String buildQuestionsList(Request r) {
        if (r.getQuestions() == null || r.getQuestions().isEmpty()) {
            return "Aucune question définie pour cette enquête.";
        }
        StringBuilder sb = new StringBuilder("**Questions de l'enquête** (").append(r.getQuestions().size()).append(")\n\n");
        int i = 1;
        for (Question q : r.getQuestions()) {
            sb.append(i++).append(". ").append(q.getText() != null ? q.getText() : "—")
              .append("  _(").append(q.getQuestionType() != null ? q.getQuestionType().name() : "?").append(")_\n");
            if (i > 10) { sb.append("… (").append(r.getQuestions().size() - 10).append(" autres)"); break; }
        }
        return sb.toString();
    }

    private String buildTimelineInsight(Request r) {
        StringBuilder sb = new StringBuilder();
        sb.append("• Créée le : ").append(r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : "—").append("\n");
        sb.append("• Échéance : ").append(r.getDeadline() != null ? r.getDeadline() : "—").append("\n");
        if (r.getDeadline() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), r.getDeadline());
            if (days < 0) sb.append("⚠ Échéance dépassée de ").append(-days).append(" jour(s).");
            else if (days == 0) sb.append("⚠ Échéance aujourd'hui.");
            else sb.append("• ").append(days).append(" jour(s) restant(s).");
        }
        return sb.toString();
    }

    private String buildContactInsight(int totalContacts, long contacted, double rate,
                                        List<Submission> subs, List<RequestContactStatus> statuses) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("• %d contacts au total, %d joints (**%.1f%%**).%n", totalContacts, contacted, rate));
        long notContacted = statuses.stream().filter(s -> s.getStatus() == ContactStatus.NOT_CONTACTED).count();
        if (notContacted > 0) sb.append("• ").append(notContacted).append(" restent à contacter.\n");
        if (rate < 30) sb.append("➡️ Taux bas — priorisez les relances.");
        else if (rate < 70) sb.append("➡️ Progression à consolider.");
        else sb.append("➡️ Bonne couverture.");
        return sb.toString();
    }

    private String buildResponsesInsight(List<Submission> subs, int nbQuestions, Request r) {
        StringBuilder sb = new StringBuilder();
        sb.append("• ").append(subs.size()).append(" soumission(s) enregistrée(s).\n");
        sb.append("• ").append(nbQuestions).append(" question(s) dans le sondage.\n");
        long totalAnswers = subs.stream()
                .filter(s -> s.getResponses() != null)
                .mapToLong(s -> s.getResponses().size())
                .sum();
        sb.append("• ").append(totalAnswers).append(" réponse(s) individuelle(s) collectée(s).\n");
        if (nbQuestions > 0 && subs.size() > 0) {
            double completion = (totalAnswers * 100.0) / (subs.size() * nbQuestions);
            sb.append(String.format("• Taux de complétion : **%.0f%%**.", completion));
        }
        return sb.toString();
    }

    private String buildRotatingSummary(String sessionId, Request r, int totalContacts,
                                         long contacted, double rate, int totalSubs, int nbQuestions) {
        int perspective = summaryTurnCounter.merge(sessionId, 1, Integer::sum) - 1;
        switch (perspective % 4) {
            case 0:
                return String.format(
                        "**Vue contacts**%n• %d contacts, %d joints (%.0f%%)%n• %d non contactés%n• Priorité : %s",
                        totalContacts, contacted, rate, totalContacts - contacted, safe(r.getPriority()));
            case 1: {
                long totalAnswers = 0;
                List<Submission> subs = submissionRepository.findByRequestIdR(r.getIdR());
                for (Submission s : subs) if (s.getResponses() != null) totalAnswers += s.getResponses().size();
                return String.format(
                        "**Vue réponses**%n• %d soumission(s)%n• %d réponse(s) individuelle(s)%n• %d question(s) posée(s)",
                        totalSubs, totalAnswers, nbQuestions);
            }
            case 2:
                return String.format(
                        "**Vue statut**%n• État : %s%n• Catégorie : %s%n• Rapport : %s",
                        safe(r.getStatus()), safe(r.getCategoryRequest()),
                        r.getReport() != null ? r.getReport().getStatus() : "Non généré");
            default:
                return String.format(
                        "**Vue calendrier**%n• Créée : %s%n• Échéance : %s%n• Taux de contact : %.0f%%",
                        r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : "—",
                        r.getDeadline() != null ? r.getDeadline() : "—", rate);
        }
    }

    private String friendlyStatus(ContactStatus s) {
        switch (s) {
            case NOT_CONTACTED: return "non contactés";
            case CONTACTED_AVAILABLE: return "joints (disponibles)";
            case CONTACTED_UNAVAILABLE: return "joints (indisponibles)";
            case NO_ANSWER: return "sans réponse";
            case CALL_BACK_LATER: return "à rappeler";
            case WRONG_NUMBER: return "mauvais numéro";
            default: return s.name();
        }
    }

    private String safe(Object o) { return o != null ? o.toString() : "—"; }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private ChatResponse reply(String message, String sessionId) {
        return ChatResponse.builder()
                .message(message)
                .sessionId(sessionId)
                .type("text")
                .timestamp(LocalDateTime.now())
                .suggestedActions(Collections.emptyList())
                .build();
    }
}
