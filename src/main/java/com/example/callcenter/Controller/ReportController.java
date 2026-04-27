package com.example.callcenter.Controller;

import com.example.callcenter.Entity.Report;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.Service.ReportService;
import com.example.callcenter.Service.ReportSchedulerService;
import com.example.callcenter.Service.MinioStorageService;
import com.example.callcenter.Service.QualityEvaluationService;
import com.example.callcenter.DTO.ReportDTO;
import com.example.callcenter.DTO.QualityEvaluationDTO.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")

public class ReportController {
    private final ReportService reportService;
    private final ReportSchedulerService reportSchedulerService;
    private final MinioStorageService minioStorageService;
    private final QualityEvaluationService qualityEvaluationService;
    private final UserRepository userRepository;
    private final String uploadDir;

    public ReportController(ReportService reportService,
                            ReportSchedulerService reportSchedulerService,
                            MinioStorageService minioStorageService,
                            QualityEvaluationService qualityEvaluationService,
                            UserRepository userRepository,
                            @Value("${file.upload-dir}") String uploadDir) {
        this.reportService = reportService;
        this.reportSchedulerService = reportSchedulerService;
        this.minioStorageService = minioStorageService;
        this.qualityEvaluationService = qualityEvaluationService;
        this.userRepository = userRepository;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/generate/{requestId}")
    public ResponseEntity<ReportDTO> generateReport(@PathVariable Long requestId) {
        Report report = reportService.generateReport(requestId); // returns Report
        ReportDTO dto = reportService.toReportDTO(report);       // convert to DTO
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<ReportDTO> getReportByRequest(@PathVariable Long requestId) {
        Report report = reportService.getReportByRequest(requestId);
        ReportDTO dto = reportService.toReportDTO(report);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/request/{requestId}/status")
    public ResponseEntity<String> getReportStatusByRequest(@PathVariable Long requestId) {
        try {
            Report report = reportService.getReportByRequest(requestId);
            return ResponseEntity.ok(report.getStatus().name());
        } catch (Exception e) {
            return ResponseEntity.ok("NOT_GENERATED");
        }
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long reportId) {
        Report report = reportService.getReportById(reportId);
        ReportDTO dto = reportService.toReportDTO(report);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        List<ReportDTO> dtos = reports.stream().map(reportService::toReportDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{reportId}/approve")
    public ResponseEntity<Map<String, Object>> approveReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.approveReport(reportId));
    }

    @PostMapping("/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(@PathVariable Long reportId) {
        reportService.rejectReport(reportId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{reportId}/pdf")
    public ResponseEntity<byte[]> getReportPdf(@PathVariable Long reportId) {
        byte[] pdfBytes = reportService.generatePdf(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport-" + reportId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    @PostMapping("/auto-generate")
    public ResponseEntity<Map<String, Object>> triggerAutoGenerateReports() {
        int count = reportSchedulerService.triggerAutoReports();
        return ResponseEntity.ok(Map.of(
                "message", "Auto-génération terminée",
                "reportsGenerated", count
        ));
    }

    @GetMapping("/{reportId}/download-url")
    public ResponseEntity<Map<String, String>> getReportDownloadUrl(@PathVariable Long reportId) {
        Report report = reportService.getReportById(reportId);

        // Always generate/upload PDF (handles caching internally — uploads to MinIO if missing)
        reportService.generatePdf(reportId);
        report = reportService.getReportById(reportId);

        // Try MinIO presigned URL first
        if (report.getPdfPath() != null && minioStorageService.isAvailable()) {
            try {
                if (minioStorageService.fileExists(report.getPdfPath())) {
                    String url = minioStorageService.getPresignedUrl(report.getPdfPath());
                    return ResponseEntity.ok(Map.of("url", url, "stored", "minio"));
                }
            } catch (Exception e) {
                // Fall through to local
            }
        }
        // Try local file URL
        if (report.getPdfPath() != null) {
            Path localPath = Paths.get(uploadDir, report.getPdfPath());
            if (Files.exists(localPath)) {
                String localUrl = "/files/" + report.getPdfPath();
                return ResponseEntity.ok(Map.of("url", localUrl, "stored", "local"));
            }
        }
        return ResponseEntity.ok(Map.of("url", "", "stored", "false"));
    }

    /** Generate a contextual Service Quality Evaluation form for a report. */
    @GetMapping("/{reportId}/quality-evaluation")
    public ResponseEntity<EvaluationForm> getQualityEvaluationForm(@PathVariable Long reportId) {
        EvaluationForm form = qualityEvaluationService.generateForm(reportId);
        return ResponseEntity.ok(form);
    }

    /** Save a quality evaluation submission and return computed results. */
    @PostMapping("/{reportId}/quality-evaluation")
    public ResponseEntity<EvaluationResult> submitQualityEvaluation(
            @PathVariable Long reportId,
            @RequestBody EvaluationSubmission submission,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = null;
        if (jwt != null) {
            String username = jwt.getClaim("preferred_username");
            if (username != null) {
                userId = userRepository.findByUsername(username)
                        .map(User::getIdUser)
                        .orElse(null);
            }
        }
        EvaluationResult result = qualityEvaluationService.processSubmission(reportId, userId, submission);
        return ResponseEntity.ok(result);
    }
}