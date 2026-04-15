package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ReportRepository;
import com.example.callcenter.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportSchedulerService {

    private final RequestRepository requestRepository;
    private final ReportRepository reportRepository;
    private final ReportService reportService;

    /**
     * Runs every day at 8:00 AM.
     * Finds completed requests (RESOLVED/CLOSED) that have no report yet,
     * generates the report, auto-approves it, and sends the email.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void autoGenerateAndApproveReports() {
        log.info("=== Auto Report Scheduler: checking for completed requests without reports ===");

        try {
            List<Request> pendingRequests = requestRepository.findCompletedRequestsWithoutReport();
            log.info("Found {} completed requests without reports", pendingRequests.size());

            for (Request request : pendingRequests) {
                try {
                    log.info("Auto-generating report for request #{} '{}'", request.getIdR(), request.getTitle());

                    // 1. Generate report (includes AI insights)
                    Report report = reportService.generateReport(request.getIdR());
                    log.info("Report #{} generated for request #{}", report.getId(), request.getIdR());

                    // 2. Auto-approve and send email
                    reportService.approveReport(report.getId());
                    log.info("Report #{} auto-approved and email sent", report.getId());

                } catch (Exception e) {
                    log.error("Failed to auto-process report for request #{}: {}",
                            request.getIdR(), e.getMessage());
                }
            }

            log.info("=== Auto Report Scheduler: completed ===");
        } catch (Exception e) {
            log.error("Auto Report Scheduler error: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing — generates and approves all pending reports.
     */
    public int triggerAutoReports() {
        log.info("Manual trigger: auto-generating reports");
        List<Request> pendingRequests = requestRepository.findCompletedRequestsWithoutReport();
        int count = 0;

        for (Request request : pendingRequests) {
            try {
                Report report = reportService.generateReport(request.getIdR());
                reportService.approveReport(report.getId());
                count++;
                log.info("Auto-processed report for request #{}", request.getIdR());
            } catch (Exception e) {
                log.error("Failed for request #{}: {}", request.getIdR(), e.getMessage());
            }
        }

        return count;
    }
}
