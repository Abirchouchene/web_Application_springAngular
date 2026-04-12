package com.example.callcenter.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    // KPI Cards
    private long totalRequests;
    private long pendingRequests;
    private long inProgressRequests;
    private long resolvedRequests;
    private long totalAgents;
    private long activeAgents;
    private double avgContactRate;

    // === NEW ADVANCED KPIs ===
    private double resolutionRate;          // % resolved out of total
    private double slaComplianceRate;       // % resolved before deadline
    private double avgResolutionHours;      // average time from creation to resolution
    private long urgentOpenRequests;        // URGENT/IMMEDIATE not yet resolved
    private long overdueRequests;           // past deadline, not resolved
    private long requestsToday;             // created today
    private long resolvedToday;             // resolved today
    private double weekOverWeekChange;      // % change vs last week

    // Requests by status
    private Map<String, Long> requestsByStatus;

    // Requests by priority
    private Map<String, Long> requestsByPriority;

    // Requests by type
    private Map<String, Long> requestsByType;

    // Requests by category
    private Map<String, Long> requestsByCategory;

    // Contact status distribution
    private Map<String, Long> contactStatusDistribution;

    // Reports overview
    private long totalReports;
    private long pendingReports;
    private long approvedReports;

    // Agent workload
    private List<AgentWorkloadDTO> agentWorkload;

    // Recent activity (last 10 logs)
    private List<RecentActivityDTO> recentActivity;

    // AI Insights (generated analysis)
    private String aiInsights;

    // === NEW: Structured AI recommendations ===
    private List<AiRecommendation> aiRecommendations;

    // Requests created per day (last 30 days)
    private List<TimeSeriesPoint> requestsTrend;

    // === NEW: Resolution trend (last 30 days) ===
    private List<TimeSeriesPoint> resolutionTrend;

    // === NEW: Hourly activity heatmap (0-23h) ===
    private Map<Integer, Long> hourlyActivity;

    // === NEW: Performance score (0-100) ===
    private int performanceScore;
    private List<PerformanceMetric> performanceBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentWorkloadDTO {
        private String agentName;
        private long assignedRequests;
        private long resolvedRequests;
        private double contactRate;
        private double resolutionRate;
        private String status; // AVAILABLE, BUSY, OVERLOADED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDTO {
        private String action;
        private String description;
        private String timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String date;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecommendation {
        private String icon;      // tabler icon name
        private String title;
        private String message;
        private String severity;  // critical, warning, info, success
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetric {
        private String label;
        private int score;      // 0-100
        private String color;   // hex color
    }
}
