package com.example.callcenter.Controller;

import com.example.callcenter.DTO.AiChatDTO.*;
import com.example.callcenter.Service.AiChatService;
import com.example.callcenter.Service.SurveyAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final SurveyAssistantService surveyAssistantService;

    /** REST endpoint for AI chat */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = aiChatService.processMessage(request);
        return ResponseEntity.ok(response);
    }

    /** Get quick prompt suggestions */
    @GetMapping("/prompts")
    public ResponseEntity<List<QuickPrompt>> getQuickPrompts() {
        return ResponseEntity.ok(aiChatService.getQuickPrompts());
    }

    /** WebSocket endpoint for real-time chat */
    @MessageMapping("/chat")
    @SendTo("/topic/chat-response")
    public ChatResponse handleWsMessage(ChatRequest request) {
        return aiChatService.processMessage(request);
    }

    /**
     * Per-survey assistant — by request id. Enforces ownership via JWT.
     */
    @PostMapping("/survey/{requestId}/message")
    public ResponseEntity<ChatResponse> askAboutSurvey(
            @PathVariable Long requestId,
            @RequestBody ChatRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        return handleSurveyAsk(jwt, body, () -> surveyAssistantService.askQuestion(requestId, body, jwt.getClaim("preferred_username")));
    }

    /**
     * Per-survey assistant — by REPORT id (what the frontend usually knows).
     * The service looks up the underlying request and enforces ownership.
     */
    @PostMapping("/report/{reportId}/message")
    public ResponseEntity<ChatResponse> askAboutReport(
            @PathVariable Long reportId,
            @RequestBody ChatRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        return handleSurveyAsk(jwt, body, () -> surveyAssistantService.askByReportId(reportId, body, jwt.getClaim("preferred_username")));
    }

    private ResponseEntity<ChatResponse> handleSurveyAsk(
            Jwt jwt, ChatRequest body, java.util.function.Supplier<ChatResponse> action) {
        if (jwt == null || jwt.getClaim("preferred_username") == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(action.get());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(ChatResponse.builder()
                    .message(e.getMessage())
                    .type("error")
                    .timestamp(java.time.LocalDateTime.now())
                    .suggestedActions(java.util.List.of())
                    .build());
        } catch (RuntimeException e) {
            // Log technical detail server-side; return a clean user-facing message per spec
            org.slf4j.LoggerFactory.getLogger(AiChatController.class)
                    .warn("Survey assistant error: {}", e.getMessage());
            return ResponseEntity.status(400).body(ChatResponse.builder()
                    .message("Information not available in the retrieved context.")
                    .type("error")
                    .timestamp(java.time.LocalDateTime.now())
                    .suggestedActions(java.util.List.of())
                    .build());
        }
    }
}
