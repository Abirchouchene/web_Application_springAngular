package com.example.callcenter.Controller;

import com.example.callcenter.DTO.FeedbackDTO;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.Service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    /** Requester submits (or updates) feedback on a report. */
    @PostMapping("/{reportId}/feedback")
    public ResponseEntity<FeedbackDTO> submitFeedback(
            @PathVariable Long reportId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = resolveUserId(jwt);

        Integer rating = null;
        Object ratingObj = body.get("rating");
        if (ratingObj instanceof Number) {
            rating = ((Number) ratingObj).intValue();
        }

        String comment = body.get("comment") instanceof String ? (String) body.get("comment") : null;

        FeedbackDTO result = feedbackService.submitFeedback(reportId, rating, comment, userId);
        return ResponseEntity.ok(result);
    }

    /** Get all feedback for a report (managers/admins see all, requester sees their own). */
    @GetMapping("/{reportId}/feedback")
    public ResponseEntity<List<FeedbackDTO>> getFeedback(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Jwt jwt) {

        List<FeedbackDTO> feedbackList = feedbackService.getFeedbackByReport(reportId);
        return ResponseEntity.ok(feedbackList);
    }

    /** Get the current user's own feedback for a report. */
    @GetMapping("/{reportId}/feedback/mine")
    public ResponseEntity<FeedbackDTO> getMyFeedback(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = resolveUserId(jwt);
        if (userId == null) return ResponseEntity.noContent().build();

        FeedbackDTO feedback = feedbackService.getFeedbackByReportAndUser(reportId, userId);
        if (feedback == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(feedback);
    }

    /** Manually trigger AI analysis on an existing feedback (useful if AI was disabled at submission time). */
    @PostMapping("/{reportId}/feedback/{feedbackId}/analyze")
    public ResponseEntity<FeedbackDTO> analyzeWithAi(
            @PathVariable Long reportId,
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            FeedbackDTO result = feedbackService.triggerAiAnalysis(feedbackId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("AI analysis trigger failed for feedback {}: {}", feedbackId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private Long resolveUserId(Jwt jwt) {
        if (jwt == null) return null;
        String username = jwt.getClaim("preferred_username");
        if (username == null) return null;
        return userRepository.findByUsername(username)
                .map(User::getIdUser)
                .orElse(null);
    }
}
