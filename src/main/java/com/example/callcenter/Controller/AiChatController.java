package com.example.callcenter.Controller;

import com.example.callcenter.DTO.AiChatDTO.*;
import com.example.callcenter.Service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

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
}
