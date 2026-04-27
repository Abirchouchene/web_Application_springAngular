package com.example.callcenter.Service;

import com.example.callcenter.DTO.FeedbackDTO;
import com.example.callcenter.Entity.Feedback;
import com.example.callcenter.Entity.Report;
import com.example.callcenter.Repository.FeedbackRepository;
import com.example.callcenter.Repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private ReportRepository reportRepository;

    private FeedbackService feedbackService;

    private Report report;

    @BeforeEach
    void setUp() {
        // Create service manually (constructor needs apiKey + baseUrl)
        feedbackService = new FeedbackService("", "http://localhost", feedbackRepository, reportRepository);
        // Disable OpenAI so no real HTTP call is made
        ReflectionTestUtils.setField(feedbackService, "enabled", false);
        ReflectionTestUtils.setField(feedbackService, "model", "gpt-4");
        ReflectionTestUtils.setField(feedbackService, "systemPrompt", "");

        report = new Report();
        report.setId(1L);
        report.setRequestTitle("Test Report");
    }

    // ── submitFeedback ──────────────────────────────────────────────────────

    @Test
    @DisplayName("submitFeedback — crée un nouveau feedback avec userId")
    void submitFeedback_withUserId_savesNewRecord() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(feedbackRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        Feedback saved = buildFeedback(1L, 42L, 4, "Très bien");
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submitFeedback(1L, 4, "Très bien", 42L);

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getComment()).isEqualTo("Très bien");
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("submitFeedback — met à jour un feedback existant (upsert)")
    void submitFeedback_existingRecord_updatesIt() {
        Feedback existing = buildFeedback(1L, 42L, 3, "Moyen");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(feedbackRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackDTO result = feedbackService.submitFeedback(1L, 5, "Excellent !", 42L);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent !");
    }

    @Test
    @DisplayName("submitFeedback — userId null : crée sans upsert lookup")
    void submitFeedback_nullUserId_createsWithoutLookup() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        Feedback saved = buildFeedback(1L, null, 3, "OK");
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        FeedbackDTO result = feedbackService.submitFeedback(1L, 3, "OK", null);

        assertThat(result).isNotNull();
        // findByReport_IdAndUserId doit ne PAS être appelé
        verify(feedbackRepository, never()).findByReport_IdAndUserId(any(), any());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("submitFeedback — rapport introuvable → RuntimeException")
    void submitFeedback_reportNotFound_throws() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.submitFeedback(99L, 4, "test", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("submitFeedback — note invalide (0) → IllegalArgumentException")
    void submitFeedback_ratingZero_throws() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.submitFeedback(1L, 0, "test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("submitFeedback — note invalide (6) → IllegalArgumentException")
    void submitFeedback_ratingAboveFive_throws() {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> feedbackService.submitFeedback(1L, 6, "test", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── getFeedbackByReport ─────────────────────────────────────────────────

    @Test
    @DisplayName("getFeedbackByReport — retourne la liste des feedbacks")
    void getFeedbackByReport_returnsList() {
        Feedback f1 = buildFeedback(1L, 1L, 5, "Super");
        Feedback f2 = buildFeedback(2L, 2L, 3, "Moyen");
        when(feedbackRepository.findByReport_Id(1L)).thenReturn(List.of(f1, f2));

        List<FeedbackDTO> result = feedbackService.getFeedbackByReport(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRating()).isEqualTo(5);
        assertThat(result.get(1).getRating()).isEqualTo(3);
    }

    @Test
    @DisplayName("getFeedbackByReport — liste vide si aucun feedback")
    void getFeedbackByReport_empty() {
        when(feedbackRepository.findByReport_Id(1L)).thenReturn(List.of());

        List<FeedbackDTO> result = feedbackService.getFeedbackByReport(1L);

        assertThat(result).isEmpty();
    }

    // ── getFeedbackByReportAndUser ──────────────────────────────────────────

    @Test
    @DisplayName("getFeedbackByReportAndUser — retourne le feedback de l'utilisateur")
    void getMyFeedback_found() {
        Feedback f = buildFeedback(1L, 42L, 4, "Bon");
        when(feedbackRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.of(f));

        FeedbackDTO result = feedbackService.getFeedbackByReportAndUser(1L, 42L);

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(4);
    }

    @Test
    @DisplayName("getFeedbackByReportAndUser — retourne null si pas de feedback")
    void getMyFeedback_notFound() {
        when(feedbackRepository.findByReport_IdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        FeedbackDTO result = feedbackService.getFeedbackByReportAndUser(1L, 42L);

        assertThat(result).isNull();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Feedback buildFeedback(Long id, Long userId, int rating, String comment) {
        Feedback f = new Feedback();
        f.setId(id);
        f.setReport(report);
        f.setUserId(userId);
        f.setRating(rating);
        f.setComment(comment);
        f.setSubmittedAt(LocalDateTime.now());
        return f;
    }
}
