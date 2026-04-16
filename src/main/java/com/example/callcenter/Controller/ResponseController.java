package com.example.callcenter.Controller;

import com.example.callcenter.DTO.ResponseDTO;
import com.example.callcenter.Entity.Question;
import com.example.callcenter.Service.ResponseService;
import com.example.callcenter.Service.ConsistencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/response")
@RequiredArgsConstructor
//@CrossOrigin("http://localhost:4200")
public class ResponseController {
    private final ResponseService responseService;
    private final ConsistencyService consistencyService;

    @PostMapping("/request/{requestId}/question/{questionId}/contact/{contactId}")
    public ResponseEntity<?> addResponsesToQuestion(
            @PathVariable Long requestId,
            @PathVariable Long questionId,
            @PathVariable Long contactId,
            @RequestBody List<String> responseValues
    ) {
        try {
            ResponseDTO responseDTO = responseService.addResponseToQuestion(requestId, questionId, contactId, responseValues);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResponse(@PathVariable Long id) {
        responseService.deleteResponseById(id);
        return ResponseEntity.ok("Réponse supprimée avec succès.");
    }

    @GetMapping("/contact/{contactId}/request/{requestId}")
    public ResponseEntity<List<ResponseDTO>> getResponsesByContactAndRequest(
            @PathVariable Long contactId,
            @PathVariable Long requestId
    ) {
        List<ResponseDTO> responses = responseService.getResponsesByContactAndRequest(contactId, requestId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/consistency/{requestId}")
    public ResponseEntity<Map<String, Object>> checkConsistency(@PathVariable Long requestId) {
        Map<String, Object> analysis = consistencyService.analyzeRequest(requestId);
        return ResponseEntity.ok(analysis);
    }
}