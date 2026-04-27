package com.example.callcenter.Service;

import com.example.callcenter.DTO.QualityEvaluationDTO.*;
import com.example.callcenter.Entity.EvaluationRecord;
import com.example.callcenter.Entity.Report;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Repository.EvaluationRecordRepository;
import com.example.callcenter.Repository.ReportRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityEvaluationServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private EvaluationRecordRepository evaluationRecordRepository;

    private QualityEvaluationService service;

    private Report report;

    @BeforeEach
    void setUp() {
        service = new QualityEvaluationService(
                reportRepository, requestRepository, evaluationRecordRepository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "openAiEnabled", false);
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost");
        ReflectionTestUtils.setField(service, "model", "gpt-4");
        ReflectionTestUtils.setField(service, "systemPrompt", "");

        report = new Report();
        report.setId(1L);
        report.setRequestTitle("Test Report");
        report.setContactRate(80.0);
    }

    // ── generateForm ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateForm — rapport introuvable → RuntimeException")
    void generateForm_reportNotFound_throws() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateForm(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("generateForm — aucune demande liée → formulaire par défaut (4 notes + 3 binaires)")
    void generateForm_noLinkedRequest_returnsDefaultForm() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(requestRepository.findByReport_Id(1L)).thenReturn(Optional.empty());

        EvaluationForm form = service.generateForm(1L);

        assertThat(form).isNotNull();
        assertThat(form.getRatings()).hasSize(4);
        assertThat(form.getBinaryQuestions()).hasSize(3);
        assertThat(form.getReportTitle()).isEqualTo("Test Report");
    }

    @Test
    @DisplayName("generateForm — demande liée, AI désactivée → formulaire rule-based")
    void generateForm_withRequest_aiDisabled_returnsRuleBasedForm() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        Request req = buildRequest(null, null, null);
        when(requestRepository.findByReport_Id(1L)).thenReturn(Optional.of(req));

        EvaluationForm form = service.generateForm(1L);

        assertThat(form).isNotNull();
        assertThat(form.getRatings()).hasSize(4);
        assertThat(form.getContextHint()).isEqualTo("Évaluation standard de la qualité de service");
    }

    @Test
    @DisplayName("generateForm — délai dépassé → contextHint spécifique")
    void generateForm_deadlinePassed_specificContextHint() {
        report.setContactRate(90.0);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        Request req = buildRequest(LocalDate.now().minusDays(1), null, null);
        when(requestRepository.findByReport_Id(1L)).thenReturn(Optional.of(req));

        EvaluationForm form = service.generateForm(1L);

        assertThat(form.getContextHint()).contains("Délai dépassé");
        assertThat(form.getOpenQuestion()).contains("délai initial");
    }

    @Test
    @DisplayName("generateForm — taux de contact < 50 → hint réactivité")
    void generateForm_lowContactRate_reactivityHint() {
        report.setContactRate(30.0);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        Request req = buildRequest(null, null, null);
        when(requestRepository.findByReport_Id(1L)).thenReturn(Optional.of(req));

        EvaluationForm form = service.generateForm(1L);

        assertThat(form.getContextHint()).contains("réactivité");
    }

    // ── processSubmission ───────────────────────────────────────────────────

    @Test
    @DisplayName("processSubmission — rapport introuvable → RuntimeException")
    void processSubmission_reportNotFound_throws() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        EvaluationSubmission submission = buildSubmission(4, 5, 3, 4);
        assertThatThrownBy(() -> service.processSubmission(99L, 1L, submission))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("processSubmission — créé un nouveau EvaluationRecord")
    void processSubmission_savesNewRecord() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(evaluationRecordRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.empty());
        EvaluationRecord saved = buildRecord(10L, report, 42L, 4.0, "GOOD");
        when(evaluationRecordRepository.save(any(EvaluationRecord.class))).thenReturn(saved);

        EvaluationSubmission submission = buildSubmission(4, 4, 4, 4);
        EvaluationResult result = service.processSubmission(1L, 42L, submission);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAverageRating()).isEqualTo(4.0);
        assertThat(result.getQualityLevel()).isEqualTo("GOOD");
        assertThat(result.getRecordId()).isEqualTo(10L);
        verify(evaluationRecordRepository).save(any(EvaluationRecord.class));
    }

    @Test
    @DisplayName("processSubmission — met à jour un EvaluationRecord existant (upsert)")
    void processSubmission_updatesExistingRecord() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        EvaluationRecord existing = buildRecord(5L, report, 42L, 2.0, "POOR");
        when(evaluationRecordRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.of(existing));
        when(evaluationRecordRepository.save(any(EvaluationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationSubmission submission = buildSubmission(5, 5, 5, 5);
        EvaluationResult result = service.processSubmission(1L, 42L, submission);

        assertThat(result.getQualityLevel()).isEqualTo("EXCELLENT");
        assertThat(result.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("processSubmission — userId null → pas de lookup upsert")
    void processSubmission_nullUserId_createsWithoutLookup() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        EvaluationRecord saved = buildRecord(7L, report, null, 3.0, "AVERAGE");
        when(evaluationRecordRepository.save(any(EvaluationRecord.class))).thenReturn(saved);

        EvaluationSubmission submission = buildSubmission(3, 3, 3, 3);
        service.processSubmission(1L, null, submission);

        verify(evaluationRecordRepository, never()).findByReport_IdAndUserId(any(), any());
        verify(evaluationRecordRepository).save(any(EvaluationRecord.class));
    }

    @Test
    @DisplayName("processSubmission — notes vides → averageRating 0, qualityLevel POOR")
    void processSubmission_emptyRatings_zeroPoor() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(evaluationRecordRepository.findByReport_IdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        EvaluationRecord saved = buildRecord(1L, report, 1L, 0.0, "POOR");
        when(evaluationRecordRepository.save(any(EvaluationRecord.class))).thenReturn(saved);

        EvaluationSubmission submission = new EvaluationSubmission();
        submission.setRatings(new HashMap<>());
        submission.setSubmittedAt(LocalDateTime.now());
        EvaluationResult result = service.processSubmission(1L, 1L, submission);

        assertThat(result.getAverageRating()).isEqualTo(0.0);
        assertThat(result.getQualityLevel()).isEqualTo("POOR");
    }

    @Test
    @DisplayName("processSubmission — moyenne >= 4.5 → EXCELLENT")
    void processSubmission_highRatings_excellent() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(evaluationRecordRepository.findByReport_IdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        when(evaluationRecordRepository.save(any(EvaluationRecord.class))).thenAnswer(inv -> {
            EvaluationRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        EvaluationSubmission submission = buildSubmission(5, 5, 5, 4);
        EvaluationResult result = service.processSubmission(1L, 1L, submission);

        assertThat(result.getQualityLevel()).isEqualTo("EXCELLENT");
        assertThat(result.getAverageRating()).isGreaterThanOrEqualTo(4.5);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Request buildRequest(LocalDate deadline, String category, String priority) {
        Request r = new Request();
        r.setIdR(1L);
        r.setDeadline(deadline);
        if (category != null) {
            try {
                r.setCategoryRequest(com.example.callcenter.Entity.CategoryRequest.valueOf(category));
            } catch (IllegalArgumentException ignored) {}
        }
        if (priority != null) {
            try {
                r.setPriority(com.example.callcenter.Entity.Priority.valueOf(priority));
            } catch (IllegalArgumentException ignored) {}
        }
        return r;
    }

    private EvaluationSubmission buildSubmission(int r1, int r2, int r3, int r4) {
        Map<String, Integer> ratings = new HashMap<>();
        ratings.put("response_quality",      r1);
        ratings.put("resolution_speed",      r2);
        ratings.put("communication_clarity", r3);
        ratings.put("overall_satisfaction",  r4);
        EvaluationSubmission s = new EvaluationSubmission();
        s.setRatings(ratings);
        s.setSubmittedAt(LocalDateTime.now());
        return s;
    }

    private EvaluationRecord buildRecord(Long id, Report rep, Long userId, double avg, String level) {
        EvaluationRecord r = new EvaluationRecord();
        r.setId(id);
        r.setReport(rep);
        r.setUserId(userId);
        r.setAverageRating(avg);
        r.setQualityLevel(level);
        r.setSubmittedAt(LocalDateTime.now());
        return r;
    }
}
