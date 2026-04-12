package com.example.callcenter.Controller;

import com.example.callcenter.DTO.AiInsightDTO;
import com.example.callcenter.Service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class AiInsightController {

    private final OpenAiService openAiService;

    /**
     * Generate AI insights for a report (calls OpenAI or falls back to rule-based).
     */
    @PostMapping("/{reportId}/ai-insights/generate")
    public ResponseEntity<AiInsightDTO> generateInsights(@PathVariable Long reportId) {
        AiInsightDTO insights = openAiService.generateInsights(reportId);
        return ResponseEntity.ok(insights);
    }

    /**
     * Get AI insights for a report (cached if available, generates otherwise).
     */
    @GetMapping("/{reportId}/ai-insights")
    public ResponseEntity<AiInsightDTO> getInsights(@PathVariable Long reportId) {
        AiInsightDTO insights = openAiService.getInsights(reportId);
        return ResponseEntity.ok(insights);
    }
}
