package com.example.callcenter.Controller;

import com.example.callcenter.Service.CallCopilotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/copilot")
@RequiredArgsConstructor
public class CallCopilotController {

    private final CallCopilotService callCopilotService;

    /**
     * Analyze a live response during a call — returns AI suggestions, detected points, reformulation hints.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeLiveResponse(@RequestBody Map<String, Object> payload) {
        Long requestId = toLong(payload.get("requestId"));
        Long contactId = toLong(payload.get("contactId"));
        Long questionId = toLong(payload.get("questionId"));
        String answer = (String) payload.get("answer");

        if (requestId == null || contactId == null || questionId == null || answer == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }

        Map<String, Object> analysis = callCopilotService.analyzeLiveResponse(requestId, contactId, questionId, answer);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Generate an auto-summary of a call with a contact — summary text + tags.
     */
    @PostMapping("/summary/{requestId}/{contactId}")
    public ResponseEntity<Map<String, Object>> generateCallSummary(
            @PathVariable Long requestId,
            @PathVariable Long contactId) {
        Map<String, Object> summary = callCopilotService.generateCallSummary(requestId, contactId);
        return ResponseEntity.ok(summary);
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
