package com.example.callcenter.Controller;

import com.example.callcenter.DTO.CallbackDTO;
import com.example.callcenter.Entity.Callback;
import com.example.callcenter.Entity.CallbackStatus;
import com.example.callcenter.Service.CallbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/callbacks")
@CrossOrigin("http://localhost:4200")

public class CallbackController {
    private final CallbackService callbackService;

    public CallbackController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping
    public ResponseEntity<CallbackDTO> createCallback(@RequestBody CallbackDTO callbackDTO) {
        Callback savedCallback = callbackService.scheduleCallback(callbackDTO);
        CallbackDTO responseDto = callbackService.mapEntityToDto(savedCallback);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/upcoming/{agentId}")
    public ResponseEntity<List<CallbackDTO>> getUpcomingCallbacks(@PathVariable Long agentId) {
        List<Callback> callbacks = callbackService.getUpcomingCallbacks(agentId);
        List<CallbackDTO> callbackDTOs = callbacks.stream()
                .map(callbackService::mapEntityToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(callbackDTOs);
    }

    @GetMapping
    public ResponseEntity<List<CallbackDTO>> getAllCallbacks() {
        List<Callback> callbacks = callbackService.getAllCallbacks();
        List<CallbackDTO> callbackDTOs = callbacks.stream()
                .map(callbackService::mapEntityToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(callbackDTOs);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CallbackDTO> updateCallbackStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {

        CallbackStatus status = CallbackStatus.valueOf(statusUpdate.get("status"));
        Callback updatedCallback = callbackService.updateCallbackStatus(id, status);
        CallbackDTO responseDto = callbackService.mapEntityToDto(updatedCallback);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCallback(@PathVariable Long id) {
        callbackService.deleteCallback(id);
        return ResponseEntity.noContent().build();
    }
}