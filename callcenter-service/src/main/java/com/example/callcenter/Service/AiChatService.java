package com.example.callcenter.Service;

import com.example.callcenter.DTO.AiChatDTO.*;
import com.example.callcenter.DTO.DashboardDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final RequestContactStatusRepository contactStatusRepository;
    private final LogsRepository logsRepository;
    private final DashboardService dashboardService;

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4}")
    private String model;

    public ChatResponse processMessage(ChatRequest request) {
        String userMessage = request.getMessage().trim().toLowerCase();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

        // Try OpenAI if enabled, otherwise use rule-based
        if (openAiEnabled && apiKey != null && !apiKey.isEmpty()) {
            try {
                return processWithOpenAi(request.getMessage(), sessionId);
            } catch (Exception e) {
                log.warn("OpenAI chat failed, falling back to rule-based: {}", e.getMessage());
            }
        }

        return processRuleBased(userMessage, sessionId);
    }

    @SuppressWarnings("unchecked")
    private ChatResponse processWithOpenAi(String userMessage, String sessionId) {
        // Build context from live data
        String context = buildDataContext();

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        String systemPrompt = "Tu es un assistant IA pour un centre d'appels. " +
                "Tu analyses les donnees en temps reel et tu reponds en francais. " +
                "Sois concis, actionnable et professionnel. " +
                "Voici les donnees actuelles du dashboard :\n\n" + context;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.4,
                "max_tokens", 500
        );

        Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        return ChatResponse.builder()
                .message(content)
                .sessionId(sessionId)
                .type("text")
                .timestamp(LocalDateTime.now())
                .suggestedActions(List.of())
                .build();
    }

    private ChatResponse processRuleBased(String userMessage, String sessionId) {
        List<Request> requests = requestRepository.findAllWithDetails();
        List<User> agents = userRepository.findAllAgents();
        List<Report> reports = reportRepository.findAll();
        List<RequestContactStatus> statuses = contactStatusRepository.findAll();

        String responseText;
        String type = "text";
        List<SuggestedAction> actions = new ArrayList<>();

        // Pattern matching for user intents
        if (matchesAny(userMessage, "performance", "score", "kpi", "indicateur")) {
            responseText = buildPerformanceResponse(requests, agents, reports, statuses);
            actions.add(SuggestedAction.builder().label("Voir les details").action("navigate").target("/dashboards/dashboard1").build());

        } else if (matchesAny(userMessage, "urgent", "urgence", "critique", "priorite")) {
            responseText = buildUrgentResponse(requests);
            type = "alert";
            actions.add(SuggestedAction.builder().label("Filtrer les urgences").action("filter").target("priority:URGENT").build());

        } else if (matchesAny(userMessage, "agent", "equipe", "charge", "workload")) {
            responseText = buildAgentResponse(agents, requests);
            actions.add(SuggestedAction.builder().label("Gestion des agents").action("navigate").target("/apps/agents").build());

        } else if (matchesAny(userMessage, "retard", "overdue", "sla", "delai", "echeance")) {
            responseText = buildSlaResponse(requests);
            type = "alert";

        } else if (matchesAny(userMessage, "tendance", "trend", "evolution", "semaine", "jour")) {
            responseText = buildTrendResponse(requests);
            type = "chart";

        } else if (matchesAny(userMessage, "contact", "appel", "telephone", "joint")) {
            responseText = buildContactResponse(statuses, reports);

        } else if (matchesAny(userMessage, "rapport", "report", "resume")) {
            responseText = buildReportResponse(reports);
            actions.add(SuggestedAction.builder().label("Voir les rapports").action("navigate").target("/apps/reports").build());

        } else if (matchesAny(userMessage, "categorie", "type", "repartition")) {
            responseText = buildCategoryResponse(requests);

        } else if (matchesAny(userMessage, "recommandation", "conseil", "suggestion", "ameliorer", "optimiser")) {
            responseText = buildRecommendationResponse(requests, agents, statuses, reports);

        } else if (matchesAny(userMessage, "bonjour", "salut", "hello", "hey", "aide", "help")) {
            responseText = buildWelcomeResponse();
            actions.addAll(List.of(
                    SuggestedAction.builder().label("Performance globale").action("prompt").target("Comment est la performance globale ?").build(),
                    SuggestedAction.builder().label("Urgences").action("prompt").target("Y a-t-il des urgences ?").build(),
                    SuggestedAction.builder().label("Agents").action("prompt").target("Quelle est la charge des agents ?").build()
            ));

        } else {
            responseText = buildGeneralResponse(requests, agents, reports, statuses);
            actions.add(SuggestedAction.builder().label("Aide").action("prompt").target("aide").build());
        }

        return ChatResponse.builder()
                .message(responseText)
                .sessionId(sessionId)
                .type(type)
                .suggestedActions(actions)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========= RESPONSE BUILDERS =========

    private String buildPerformanceResponse(List<Request> requests, List<User> agents,
                                            List<Report> reports, List<RequestContactStatus> statuses) {
        long total = requests.size();
        long resolved = requests.stream().filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED).count();
        double resRate = total > 0 ? (resolved * 100.0) / total : 0;

        long urgent = requests.stream().filter(r -> r.getPriority() == Priority.URGENT && r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED).count();
        long activeAgents = agents.stream().filter(User::isEnabled).count();
        long contacted = statuses.stream().filter(s -> s.getStatus() != ContactStatus.NOT_CONTACTED).count();
        double contactRate = !statuses.isEmpty() ? (contacted * 100.0) / statuses.size() : 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**Analyse de Performance**\n\n"));
        sb.append(String.format("- **Taux de resolution** : %.1f%% (%d/%d demandes)\n", resRate, resolved, total));
        sb.append(String.format("- **Agents actifs** : %d/%d\n", activeAgents, agents.size()));
        sb.append(String.format("- **Urgences ouvertes** : %d\n", urgent));
        sb.append(String.format("- **Taux de contact** : %.1f%%\n", contactRate));
        sb.append(String.format("- **Rapports** : %d au total\n\n", reports.size()));

        if (resRate >= 80) sb.append("Excellente performance ! L'equipe maintient un bon rythme de resolution.");
        else if (resRate >= 50) sb.append("Performance correcte. Il y a des marges d'amelioration sur le taux de resolution.");
        else sb.append("Le taux de resolution est bas. Il faut prioriser les demandes en attente et renforcer l'equipe.");

        return sb.toString();
    }

    private String buildUrgentResponse(List<Request> requests) {
        List<Request> urgents = requests.stream()
                .filter(r -> r.getPriority() == Priority.URGENT && r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED)
                .collect(Collectors.toList());

        if (urgents.isEmpty()) {
            return "Bonne nouvelle ! Il n'y a actuellement aucune demande urgente ouverte.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%d urgence(s) ouverte(s)**\n\n", urgents.size()));

        long pending = urgents.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        long assigned = urgents.stream().filter(r -> r.getStatus() == Status.ASSIGNED).count();
        long inProgress = urgents.stream().filter(r -> r.getStatus() == Status.IN_PROGRESS).count();

        if (pending > 0) sb.append(String.format("- %d en attente (necessite qu'on les assigne)\n", pending));
        if (assigned > 0) sb.append(String.format("- %d assignees (en cours de traitement)\n", assigned));
        if (inProgress > 0) sb.append(String.format("- %d en cours\n", inProgress));

        sb.append("\nAction recommandee : Priorisez les demandes urgentes en attente et assignez-les aux agents disponibles.");
        return sb.toString();
    }

    private String buildAgentResponse(List<User> agents, List<Request> requests) {
        long activeAgents = agents.stream().filter(User::isEnabled).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**Analyse de l'equipe** (%d agents, %d actifs)\n\n", agents.size(), activeAgents));

        for (User agent : agents) {
            if (!agent.isEnabled()) continue;
            long assigned = requests.stream()
                    .filter(r -> r.getAgent() != null && r.getAgent().getIdUser().equals(agent.getIdUser()))
                    .filter(r -> r.getStatus() == Status.ASSIGNED || r.getStatus() == Status.IN_PROGRESS)
                    .count();
            long resolvedAgent = requests.stream()
                    .filter(r -> r.getAgent() != null && r.getAgent().getIdUser().equals(agent.getIdUser()))
                    .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                    .count();

            String status = assigned > 5 ? "Surcharge" : assigned > 2 ? "Occupe" : "Disponible";
            sb.append(String.format("- **%s** : %d actives, %d resolues (%s)\n",
                    agent.getFullName(), assigned, resolvedAgent, status));
        }

        long unassigned = requests.stream()
                .filter(r -> r.getAgent() == null && r.getStatus() == Status.PENDING)
                .count();
        if (unassigned > 0) {
            sb.append(String.format("\n%d demandes non assignees en attente.", unassigned));
        }

        return sb.toString();
    }

    private String buildSlaResponse(List<Request> requests) {
        LocalDate today = LocalDate.now();
        long overdue = requests.stream()
                .filter(r -> r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED)
                .filter(r -> r.getDeadline() != null && r.getDeadline().isBefore(today))
                .count();

        long total = requests.size();
        long resolved = requests.stream().filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED).count();
        long onTime = requests.stream()
                .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                .filter(r -> r.getDeadline() == null || r.getUpdatedAt().toLocalDate().isBefore(r.getDeadline()))
                .count();
        double slaRate = resolved > 0 ? (onTime * 100.0) / resolved : 100;

        StringBuilder sb = new StringBuilder();
        sb.append("**Analyse SLA & Delais**\n\n");
        sb.append(String.format("- **Conformite SLA** : %.1f%%\n", slaRate));
        sb.append(String.format("- **Demandes en retard** : %d\n", overdue));

        if (overdue > 0) {
            sb.append(String.format("\nAttention : %d demandes depassent l'echeance prevue. ", overdue));
            sb.append("Reassignez-les ou ajustez les delais pour eviter l'impact sur la satisfaction client.");
        } else {
            sb.append("\nTous les delais sont respectes. Bonne gestion du temps !");
        }

        return sb.toString();
    }

    private String buildTrendResponse(List<Request> requests) {
        LocalDate today = LocalDate.now();
        long todayCount = requests.stream().filter(r -> r.getCreatedAt().toLocalDate().equals(today)).count();
        long yesterdayCount = requests.stream().filter(r -> r.getCreatedAt().toLocalDate().equals(today.minusDays(1))).count();
        long thisWeek = requests.stream().filter(r -> r.getCreatedAt().toLocalDate().isAfter(today.minusDays(7))).count();
        long lastWeek = requests.stream().filter(r -> {
            LocalDate d = r.getCreatedAt().toLocalDate();
            return d.isAfter(today.minusDays(14)) && d.isBefore(today.minusDays(6));
        }).count();

        double weekChange = lastWeek > 0 ? ((thisWeek - lastWeek) * 100.0) / lastWeek : (thisWeek > 0 ? 100 : 0);

        StringBuilder sb = new StringBuilder();
        sb.append("**Tendances**\n\n");
        sb.append(String.format("- **Aujourd'hui** : %d demandes creees\n", todayCount));
        sb.append(String.format("- **Hier** : %d demandes\n", yesterdayCount));
        sb.append(String.format("- **Cette semaine** : %d demandes (%.0f%% vs semaine derniere)\n", thisWeek, weekChange));

        if (weekChange > 30) sb.append("\nTendance a la hausse. Anticipez une augmentation de charge.");
        else if (weekChange < -30) sb.append("\nPeriode calme. Profitez-en pour traiter les demandes en retard.");
        else sb.append("\nFlux stable. Continuez sur ce rythme.");

        return sb.toString();
    }

    private String buildContactResponse(List<RequestContactStatus> statuses, List<Report> reports) {
        long total = statuses.size();
        long contacted = statuses.stream().filter(s -> s.getStatus() != ContactStatus.NOT_CONTACTED).count();
        double rate = total > 0 ? (contacted * 100.0) / total : 0;

        Map<ContactStatus, Long> distribution = statuses.stream()
                .collect(Collectors.groupingBy(RequestContactStatus::getStatus, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("**Analyse des contacts**\n\n");
        sb.append(String.format("- **Taux de contact** : %.1f%% (%d/%d)\n", rate, contacted, total));

        distribution.forEach((status, count) -> {
            String label = switch (status) {
                case NOT_CONTACTED -> "Non contacte";
                case CONTACTED_AVAILABLE -> "Contacte - Disponible";
                case CONTACTED_UNAVAILABLE -> "Contacte - Indisponible";
                case NO_ANSWER -> "Pas de reponse";
                case CALL_BACK_LATER -> "A rappeler";
                case WRONG_NUMBER -> "Mauvais numero";
            };
            sb.append(String.format("- %s : %d\n", label, count));
        });

        return sb.toString();
    }

    private String buildReportResponse(List<Report> reports) {
        long pending = reports.stream().filter(r -> r.getStatus() == ReportStatus.PENDING_APPROVAL).count();
        long approved = reports.stream().filter(r -> r.getStatus() == ReportStatus.APPROVED).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**Rapports** (%d au total)\n\n", reports.size()));
        sb.append(String.format("- En attente d'approbation : %d\n", pending));
        sb.append(String.format("- Approuves : %d\n", approved));
        sb.append(String.format("- Rejetes : %d\n", reports.size() - pending - approved));

        if (pending > 0) {
            sb.append(String.format("\n%d rapport(s) attendent votre validation.", pending));
        }

        return sb.toString();
    }

    private String buildCategoryResponse(List<Request> requests) {
        Map<String, Long> byCategory = requests.stream()
                .filter(r -> r.getCategoryRequest() != null)
                .collect(Collectors.groupingBy(r -> r.getCategoryRequest().name(), Collectors.counting()));

        Map<String, Long> byType = requests.stream()
                .filter(r -> r.getRequestType() != null)
                .collect(Collectors.groupingBy(r -> r.getRequestType().name(), Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("**Repartition des demandes**\n\n");
        sb.append("Par categorie :\n");
        byCategory.forEach((cat, count) -> sb.append(String.format("- %s : %d\n", cat, count)));
        sb.append("\nPar type :\n");
        byType.forEach((type, count) -> sb.append(String.format("- %s : %d\n", type, count)));

        return sb.toString();
    }

    private String buildRecommendationResponse(List<Request> requests, List<User> agents,
                                               List<RequestContactStatus> statuses, List<Report> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Recommandations IA**\n\n");

        long total = requests.size();
        long resolved = requests.stream().filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED).count();
        double resRate = total > 0 ? (resolved * 100.0) / total : 0;

        long urgent = requests.stream().filter(r -> r.getPriority() == Priority.URGENT && r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED).count();
        long pending = requests.stream().filter(r -> r.getStatus() == Status.PENDING).count();

        int num = 1;
        if (resRate < 50) {
            sb.append(String.format("%d. **Ameliorer le taux de resolution** (%.1f%%) : Formez les agents ou renforcez l'equipe.\n", num++, resRate));
        }
        if (urgent > 3) {
            sb.append(String.format("%d. **Reduire les urgences** (%d ouvertes) : Assignez-les immediatement.\n", num++, urgent));
        }
        if (pending > total * 0.3) {
            sb.append(String.format("%d. **Debloquer la file d'attente** (%d en attente) : Automatisez le tri initial.\n", num++, pending));
        }

        long contacted = statuses.stream().filter(s -> s.getStatus() != ContactStatus.NOT_CONTACTED).count();
        double contactRate = !statuses.isEmpty() ? (contacted * 100.0) / statuses.size() : 100;
        if (contactRate < 50) {
            sb.append(String.format("%d. **Ameliorer le taux de contact** (%.1f%%) : Planifiez des relances systematiques.\n", num++, contactRate));
        }

        long activeAgents = agents.stream().filter(User::isEnabled).count();
        if (activeAgents > 0 && total / activeAgents > 10) {
            sb.append(String.format("%d. **Surcharge detectee** : %.0f demandes/agent. Envisagez de recruter.\n", num++, (double) total / activeAgents));
        }

        if (num == 1) {
            sb.append("Tout semble bien ! Les indicateurs sont dans les normes. Continuez ainsi.");
        }

        return sb.toString();
    }

    private String buildWelcomeResponse() {
        return "Bonjour ! Je suis votre assistant IA du centre d'appels.\n\n" +
                "Je peux vous aider avec :\n" +
                "- **Performance** : Taux de resolution, SLA, scores\n" +
                "- **Urgences** : Demandes critiques, retards\n" +
                "- **Agents** : Charge de travail, disponibilite\n" +
                "- **Tendances** : Evolution quotidienne/hebdomadaire\n" +
                "- **Contacts** : Taux de contact, distribution\n" +
                "- **Recommandations** : Suggestions d'amelioration\n\n" +
                "Posez-moi votre question !";
    }

    private String buildGeneralResponse(List<Request> requests, List<User> agents,
                                        List<Report> reports, List<RequestContactStatus> statuses) {
        long total = requests.size();
        long resolved = requests.stream().filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED).count();
        long pending = requests.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        long urgent = requests.stream().filter(r -> r.getPriority() == Priority.URGENT && r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED).count();

        return String.format("**Resume rapide**\n\n" +
                        "- %d demandes au total (%d resolues, %d en attente)\n" +
                        "- %d urgence(s) ouverte(s)\n" +
                        "- %d agents actifs\n" +
                        "- %d rapports\n\n" +
                        "Pour plus de details, demandez-moi sur un sujet specifique (performance, agents, urgences, etc.)",
                total, resolved, pending, urgent,
                agents.stream().filter(User::isEnabled).count(),
                reports.size());
    }

    public List<QuickPrompt> getQuickPrompts() {
        return List.of(
                QuickPrompt.builder().icon("chart-line").label("Performance globale").prompt("Comment est la performance globale ?").build(),
                QuickPrompt.builder().icon("urgent").label("Urgences").prompt("Y a-t-il des urgences en cours ?").build(),
                QuickPrompt.builder().icon("users").label("Charge agents").prompt("Quelle est la charge des agents ?").build(),
                QuickPrompt.builder().icon("trending-up").label("Tendances").prompt("Montrez-moi les tendances recentes").build(),
                QuickPrompt.builder().icon("shield-check").label("SLA").prompt("Comment est la conformite SLA ?").build(),
                QuickPrompt.builder().icon("bulb").label("Recommandations").prompt("Donnez-moi des recommandations").build()
        );
    }

    // ========= HELPERS =========

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String buildDataContext() {
        List<Request> requests = requestRepository.findAllWithDetails();
        List<User> agents = userRepository.findAllAgents();
        List<Report> reports = reportRepository.findAll();
        List<RequestContactStatus> statuses = contactStatusRepository.findAll();

        long total = requests.size();
        long resolved = requests.stream().filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED).count();
        long pending = requests.stream().filter(r -> r.getStatus() == Status.PENDING).count();
        long urgent = requests.stream().filter(r -> r.getPriority() == Priority.URGENT && r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED).count();
        long overdue = requests.stream()
                .filter(r -> r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED)
                .filter(r -> r.getDeadline() != null && r.getDeadline().isBefore(LocalDate.now()))
                .count();

        return String.format(
                "Total demandes: %d, Resolues: %d, En attente: %d, Urgentes ouvertes: %d, En retard: %d, " +
                        "Agents actifs: %d/%d, Rapports: %d, Contacts: %d",
                total, resolved, pending, urgent, overdue,
                agents.stream().filter(User::isEnabled).count(), agents.size(),
                reports.size(), statuses.size()
        );
    }
}
