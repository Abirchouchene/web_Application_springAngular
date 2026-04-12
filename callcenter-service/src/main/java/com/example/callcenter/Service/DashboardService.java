package com.example.callcenter.Service;

import com.example.callcenter.DTO.DashboardDTO;
import com.example.callcenter.DTO.DashboardDTO.*;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final RequestContactStatusRepository contactStatusRepository;
    private final LogsRepository logsRepository;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardDTO getDashboardStats() {
        List<Request> allRequests = requestRepository.findAllWithDetails();
        List<User> allAgents = userRepository.findAllAgents();
        List<Report> allReports = reportRepository.findAll();
        List<RequestContactStatus> allContactStatuses = contactStatusRepository.findAll();

        long resolved = countResolved(allRequests);
        long total = allRequests.size();
        double resolutionRate = total > 0 ? (resolved * 100.0) / total : 0;

        return DashboardDTO.builder()
                // Existing KPIs
                .totalRequests(total)
                .pendingRequests(countByStatus(allRequests, Status.PENDING))
                .inProgressRequests(countByStatus(allRequests, Status.IN_PROGRESS) + countByStatus(allRequests, Status.ASSIGNED))
                .resolvedRequests(resolved)
                .totalAgents(allAgents.size())
                .activeAgents(allAgents.stream().filter(User::isEnabled).count())
                .avgContactRate(calculateAvgContactRate(allReports))
                // New advanced KPIs
                .resolutionRate(Math.round(resolutionRate * 10.0) / 10.0)
                .slaComplianceRate(calculateSlaCompliance(allRequests))
                .avgResolutionHours(calculateAvgResolutionHours(allRequests))
                .urgentOpenRequests(countUrgentOpen(allRequests))
                .overdueRequests(countOverdue(allRequests))
                .requestsToday(countCreatedToday(allRequests))
                .resolvedToday(countResolvedToday(allRequests))
                .weekOverWeekChange(calculateWoWChange(allRequests))
                // Distributions
                .requestsByStatus(buildStatusMap(allRequests))
                .requestsByPriority(buildPriorityMap(allRequests))
                .requestsByType(buildTypeMap(allRequests))
                .requestsByCategory(buildCategoryMap(allRequests))
                .contactStatusDistribution(buildContactStatusMap(allContactStatuses))
                // Reports
                .totalReports(allReports.size())
                .pendingReports(allReports.stream().filter(r -> r.getStatus() == ReportStatus.PENDING_APPROVAL).count())
                .approvedReports(allReports.stream().filter(r -> r.getStatus() == ReportStatus.APPROVED).count())
                // Agent & Activity
                .agentWorkload(buildAgentWorkload(allAgents, allRequests, allReports))
                .recentActivity(buildRecentActivity())
                // Trends
                .requestsTrend(buildRequestsTrend(allRequests))
                .resolutionTrend(buildResolutionTrend(allRequests))
                .hourlyActivity(buildHourlyActivity(allRequests))
                // AI
                .aiInsights(generateAiInsights(allRequests, allReports, allAgents, allContactStatuses))
                .aiRecommendations(generateAiRecommendations(allRequests, allReports, allAgents, allContactStatuses))
                // Performance
                .performanceScore(calculatePerformanceScore(allRequests, allReports, allContactStatuses))
                .performanceBreakdown(buildPerformanceBreakdown(allRequests, allReports, allContactStatuses))
                .build();
    }

    /** Push real-time dashboard update via WebSocket */
    public void pushDashboardUpdate() {
        DashboardDTO stats = getDashboardStats();
        messagingTemplate.convertAndSend("/topic/dashboard", stats);
    }

    // ========= CORE HELPERS =========

    private long countByStatus(List<Request> requests, Status status) {
        return requests.stream().filter(r -> r.getStatus() == status).count();
    }

    private long countResolved(List<Request> requests) {
        return requests.stream()
                .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                .count();
    }

    // ========= NEW ADVANCED KPIs =========

    private double calculateSlaCompliance(List<Request> requests) {
        List<Request> withDeadline = requests.stream()
                .filter(r -> r.getDeadline() != null)
                .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                .collect(Collectors.toList());
        if (withDeadline.isEmpty()) return 100.0;

        long onTime = withDeadline.stream()
                .filter(r -> {
                    LocalDate resolvedDate = r.getUpdatedAt() != null ? r.getUpdatedAt().toLocalDate() : LocalDate.now();
                    return !resolvedDate.isAfter(r.getDeadline());
                }).count();
        return Math.round((onTime * 1000.0) / withDeadline.size()) / 10.0;
    }

    private double calculateAvgResolutionHours(List<Request> requests) {
        OptionalDouble avg = requests.stream()
                .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                .filter(r -> r.getCreatedAt() != null && r.getUpdatedAt() != null)
                .mapToDouble(r -> ChronoUnit.HOURS.between(r.getCreatedAt(), r.getUpdatedAt()))
                .average();
        return Math.round(avg.orElse(0) * 10.0) / 10.0;
    }

    private long countUrgentOpen(List<Request> requests) {
        return requests.stream()
                .filter(r -> r.getPriority() == Priority.URGENT || r.getPriority() == Priority.IMMEDIATE)
                .filter(r -> r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED)
                .count();
    }

    private long countOverdue(List<Request> requests) {
        LocalDate today = LocalDate.now();
        return requests.stream()
                .filter(r -> r.getDeadline() != null && r.getDeadline().isBefore(today))
                .filter(r -> r.getStatus() != Status.RESOLVED && r.getStatus() != Status.CLOSED)
                .count();
    }

    private long countCreatedToday(List<Request> requests) {
        LocalDate today = LocalDate.now();
        return requests.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(today))
                .count();
    }

    private long countResolvedToday(List<Request> requests) {
        LocalDate today = LocalDate.now();
        return requests.stream()
                .filter(r -> (r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED))
                .filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().toLocalDate().equals(today))
                .count();
    }

    private double calculateWoWChange(List<Request> requests) {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.minusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusDays(7);

        long thisWeekCount = requests.stream()
                .filter(r -> r.getCreatedAt() != null)
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(thisWeekStart))
                .count();
        long lastWeekCount = requests.stream()
                .filter(r -> r.getCreatedAt() != null)
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(lastWeekStart)
                        && r.getCreatedAt().toLocalDate().isBefore(thisWeekStart))
                .count();
        if (lastWeekCount == 0) return thisWeekCount > 0 ? 100.0 : 0.0;
        return Math.round(((thisWeekCount - lastWeekCount) * 1000.0) / lastWeekCount) / 10.0;
    }

    // ========= DISTRIBUTIONS =========

    private Map<String, Long> buildStatusMap(List<Request> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));
    }

    private Map<String, Long> buildPriorityMap(List<Request> requests) {
        return requests.stream()
                .filter(r -> r.getPriority() != null)
                .collect(Collectors.groupingBy(r -> r.getPriority().name(), Collectors.counting()));
    }

    private Map<String, Long> buildTypeMap(List<Request> requests) {
        return requests.stream()
                .collect(Collectors.groupingBy(r -> r.getRequestType().name(), Collectors.counting()));
    }

    private Map<String, Long> buildCategoryMap(List<Request> requests) {
        return requests.stream()
                .filter(r -> r.getCategoryRequest() != null)
                .collect(Collectors.groupingBy(r -> r.getCategoryRequest().name(), Collectors.counting()));
    }

    private Map<String, Long> buildContactStatusMap(List<RequestContactStatus> statuses) {
        return statuses.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus().name(), Collectors.counting()));
    }

    private double calculateAvgContactRate(List<Report> reports) {
        return reports.stream()
                .filter(r -> r.getContactRate() != null)
                .mapToDouble(Report::getContactRate)
                .average()
                .orElse(0.0);
    }

    // ========= AGENT WORKLOAD =========

    private List<AgentWorkloadDTO> buildAgentWorkload(List<User> agents, List<Request> requests, List<Report> reports) {
        return agents.stream().map(agent -> {
            List<Request> agentRequests = requests.stream()
                    .filter(r -> r.getAgent() != null && r.getAgent().getIdUser().equals(agent.getIdUser()))
                    .collect(Collectors.toList());

            long resolved = agentRequests.stream()
                    .filter(r -> r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED)
                    .count();

            long active = agentRequests.stream()
                    .filter(r -> r.getStatus() == Status.IN_PROGRESS || r.getStatus() == Status.ASSIGNED)
                    .count();

            double contactRate = agentRequests.stream()
                    .filter(r -> r.getReport() != null && r.getReport().getContactRate() != null)
                    .mapToDouble(r -> r.getReport().getContactRate())
                    .average()
                    .orElse(0.0);

            double agentResRate = agentRequests.isEmpty() ? 0 : (resolved * 100.0) / agentRequests.size();

            String status = active > 8 ? "OVERLOADED" : active > 3 ? "BUSY" : "AVAILABLE";

            return AgentWorkloadDTO.builder()
                    .agentName(agent.getFullName())
                    .assignedRequests(agentRequests.size())
                    .resolvedRequests(resolved)
                    .contactRate(Math.round(contactRate * 100.0) / 100.0)
                    .resolutionRate(Math.round(agentResRate * 10.0) / 10.0)
                    .status(status)
                    .build();
        }).sorted(Comparator.comparingLong(AgentWorkloadDTO::getAssignedRequests).reversed())
          .collect(Collectors.toList());
    }

    // ========= ACTIVITY =========

    private List<RecentActivityDTO> buildRecentActivity() {
        List<Logs> recentLogs = logsRepository.findTopNByOrderByTimestampDesc(10);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return recentLogs.stream().map(log -> RecentActivityDTO.builder()
                .action(log.getLogAction().name())
                .description(log.getActionDescription())
                .timestamp(log.getTimestamp().format(fmt))
                .build()
        ).collect(Collectors.toList());
    }

    // ========= TRENDS =========

    private List<TimeSeriesPoint> buildRequestsTrend(List<Request> requests) {
        return buildDailyTrend(requests, true);
    }

    private List<TimeSeriesPoint> buildResolutionTrend(List<Request> requests) {
        return buildDailyTrend(requests, false);
    }

    private List<TimeSeriesPoint> buildDailyTrend(List<Request> requests, boolean created) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        Map<LocalDate, Long> countByDate;
        if (created) {
            countByDate = requests.stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().toLocalDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));
        } else {
            countByDate = requests.stream()
                    .filter(r -> (r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED))
                    .filter(r -> r.getUpdatedAt() != null && !r.getUpdatedAt().toLocalDate().isBefore(startDate))
                    .collect(Collectors.groupingBy(r -> r.getUpdatedAt().toLocalDate(), Collectors.counting()));
        }

        List<TimeSeriesPoint> trend = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            trend.add(TimeSeriesPoint.builder()
                    .date(d.format(fmt))
                    .count(countByDate.getOrDefault(d, 0L))
                    .build());
        }
        return trend;
    }

    private Map<Integer, Long> buildHourlyActivity(List<Request> requests) {
        Map<Integer, Long> hourly = new TreeMap<>();
        for (int h = 0; h < 24; h++) hourly.put(h, 0L);
        requests.stream()
                .filter(r -> r.getCreatedAt() != null)
                .forEach(r -> {
                    int hour = r.getCreatedAt().getHour();
                    hourly.merge(hour, 1L, Long::sum);
                });
        return hourly;
    }

    // ========= PERFORMANCE SCORE =========

    private int calculatePerformanceScore(List<Request> requests, List<Report> reports, List<RequestContactStatus> statuses) {
        int resScore = calcResolutionScore(requests);
        int slaScore = (int) Math.round(calculateSlaCompliance(requests));
        int contactScore = calcContactScore(statuses);
        int throughputScore = calcThroughputScore(requests);
        // Weighted average
        return (int) Math.round(resScore * 0.3 + slaScore * 0.25 + contactScore * 0.25 + throughputScore * 0.2);
    }

    private List<PerformanceMetric> buildPerformanceBreakdown(List<Request> requests, List<Report> reports, List<RequestContactStatus> statuses) {
        int resScore = calcResolutionScore(requests);
        int slaScore = (int) Math.round(calculateSlaCompliance(requests));
        int contactScore = calcContactScore(statuses);
        int throughputScore = calcThroughputScore(requests);

        return List.of(
                PerformanceMetric.builder().label("Résolution").score(resScore).color(scoreColor(resScore)).build(),
                PerformanceMetric.builder().label("SLA").score(slaScore).color(scoreColor(slaScore)).build(),
                PerformanceMetric.builder().label("Contact").score(contactScore).color(scoreColor(contactScore)).build(),
                PerformanceMetric.builder().label("Productivité").score(throughputScore).color(scoreColor(throughputScore)).build()
        );
    }

    private int calcResolutionScore(List<Request> requests) {
        if (requests.isEmpty()) return 0;
        double rate = (countResolved(requests) * 100.0) / requests.size();
        return Math.min(100, (int) Math.round(rate));
    }

    private int calcContactScore(List<RequestContactStatus> statuses) {
        if (statuses.isEmpty()) return 100;
        long contacted = statuses.stream().filter(s -> s.getStatus() != ContactStatus.NOT_CONTACTED).count();
        return Math.min(100, (int) Math.round((contacted * 100.0) / statuses.size()));
    }

    private int calcThroughputScore(List<Request> requests) {
        long last7 = requests.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
        long resolved7 = requests.stream()
                .filter(r -> (r.getStatus() == Status.RESOLVED || r.getStatus() == Status.CLOSED))
                .filter(r -> r.getUpdatedAt() != null && r.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .count();
        if (last7 == 0) return 100;
        return Math.min(100, (int) Math.round((resolved7 * 100.0) / last7));
    }

    private String scoreColor(int score) {
        if (score >= 80) return "#13deb9";
        if (score >= 60) return "#ffb22b";
        return "#fa896b";
    }

    // ========= AI INSIGHTS (text) =========

    private String generateAiInsights(List<Request> requests, List<Report> reports,
                                       List<User> agents, List<RequestContactStatus> contactStatuses) {
        StringBuilder insights = new StringBuilder();
        long total = requests.size();
        if (total == 0) return "Aucune donnée disponible pour l'analyse. Commencez par créer des demandes.";

        long resolved = countResolved(requests);
        double resolutionRate = (resolved * 100.0) / total;
        insights.append(String.format("📊 Taux de résolution : %.1f%% (%d/%d). ", resolutionRate, resolved, total));
        if (resolutionRate >= 80) insights.append("Excellent ! ");
        else if (resolutionRate < 50) insights.append("⚠️ Action requise. ");

        long pending = countByStatus(requests, Status.PENDING);
        if (pending > total * 0.3)
            insights.append(String.format("\n⏳ %d demandes en attente (%.0f%%). ", pending, (pending * 100.0) / total));

        long urgent = countUrgentOpen(requests);
        if (urgent > 0) insights.append(String.format("\n🔴 %d urgence(s) ouverte(s). ", urgent));

        long overdue = countOverdue(requests);
        if (overdue > 0) insights.append(String.format("\n⏰ %d demande(s) en retard. ", overdue));

        long activeAgents = agents.stream().filter(User::isEnabled).count();
        if (activeAgents > 0) {
            double avgLoad = (double) requests.stream()
                    .filter(r -> r.getAgent() != null && (r.getStatus() == Status.IN_PROGRESS || r.getStatus() == Status.ASSIGNED))
                    .count() / activeAgents;
            insights.append(String.format("\n👥 Charge moyenne : %.1f demandes/agent. ", avgLoad));
        }

        return insights.toString();
    }

    // ========= AI RECOMMENDATIONS (structured) =========

    private List<AiRecommendation> generateAiRecommendations(List<Request> requests, List<Report> reports,
                                                               List<User> agents, List<RequestContactStatus> statuses) {
        List<AiRecommendation> recs = new ArrayList<>();
        long total = requests.size();
        if (total == 0) {
            recs.add(AiRecommendation.builder().icon("database").title("Données insuffisantes")
                    .message("Créez des demandes pour activer les analyses IA.").severity("info").build());
            return recs;
        }

        long resolved = countResolved(requests);
        double resRate = (resolved * 100.0) / total;
        if (resRate < 50) {
            recs.add(AiRecommendation.builder().icon("alert-triangle").title("Taux de résolution critique")
                    .message(String.format("Seulement %.0f%% des demandes résolues. Renforcez l'équipe ou priorisez les demandes urgentes.", resRate))
                    .severity("critical").build());
        } else if (resRate >= 80) {
            recs.add(AiRecommendation.builder().icon("trophy").title("Performance excellente")
                    .message(String.format("%.0f%% de résolution — l'équipe est très performante.", resRate))
                    .severity("success").build());
        }

        long overdue = countOverdue(requests);
        if (overdue > 0) {
            recs.add(AiRecommendation.builder().icon("clock-exclamation").title(overdue + " demande(s) en retard")
                    .message("Réassignez-les ou ajustez les échéances pour maintenir la qualité SLA.")
                    .severity("critical").build());
        }

        long urgent = countUrgentOpen(requests);
        if (urgent > 0) {
            recs.add(AiRecommendation.builder().icon("urgent").title(urgent + " urgence(s) non traitée(s)")
                    .message("Priorisez immédiatement ces demandes urgentes.")
                    .severity("warning").build());
        }

        long pending = countByStatus(requests, Status.PENDING);
        if (pending > total * 0.3) {
            recs.add(AiRecommendation.builder().icon("hourglass").title("File d'attente élevée")
                    .message(String.format("%d demandes en attente (%.0f%%). Assignez plus d'agents.", pending, (pending * 100.0) / total))
                    .severity("warning").build());
        }

        double sla = calculateSlaCompliance(requests);
        if (sla < 80) {
            recs.add(AiRecommendation.builder().icon("shield-x").title("SLA en danger")
                    .message(String.format("Conformité SLA à %.0f%%. Améliorez les temps de traitement.", sla))
                    .severity("warning").build());
        }

        long contactedCount = statuses.stream().filter(s -> s.getStatus() != ContactStatus.NOT_CONTACTED).count();
        if (!statuses.isEmpty()) {
            double contactPct = (contactedCount * 100.0) / statuses.size();
            if (contactPct < 50) {
                recs.add(AiRecommendation.builder().icon("phone-off").title("Taux de contact bas")
                        .message(String.format("%.0f%% de contacts joints. Vérifiez les numéros et planifiez des relances.", contactPct))
                        .severity("warning").build());
            }
        }

        double wow = calculateWoWChange(requests);
        if (wow > 30) {
            recs.add(AiRecommendation.builder().icon("trending-up").title("Hausse des demandes")
                    .message(String.format("+%.0f%% cette semaine. Anticipez la charge.", wow))
                    .severity("info").build());
        } else if (wow < -30) {
            recs.add(AiRecommendation.builder().icon("trending-down").title("Baisse des demandes")
                    .message(String.format("%.0f%% cette semaine — période calme.", wow))
                    .severity("info").build());
        }

        if (recs.isEmpty()) {
            recs.add(AiRecommendation.builder().icon("circle-check").title("Tout est en ordre")
                    .message("Les indicateurs sont bons. Continuez ainsi !").severity("success").build());
        }

        return recs;
    }
}
