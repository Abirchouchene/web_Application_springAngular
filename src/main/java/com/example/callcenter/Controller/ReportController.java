package com.example.callcenter.Controller;

import com.example.callcenter.Entity.Report;
import com.example.callcenter.Service.ReportService;
import com.example.callcenter.DTO.ReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")

public class ReportController {
    private final ReportService reportService;

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
    public ResponseEntity<Void> approveReport(@PathVariable Long reportId) {
        reportService.approveReport(reportId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(@PathVariable Long reportId) {
        reportService.rejectReport(reportId);
        return ResponseEntity.ok().build();
    }
} 