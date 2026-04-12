package com.example.contactservice.controller;

import com.example.contactservice.dto.CallbackDTO;
import com.example.contactservice.entity.Callback;
import com.example.contactservice.entity.CallbackStatus;
import com.example.contactservice.service.CallbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/callbacks")
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

    @GetMapping("/upcoming")
    public ResponseEntity<List<CallbackDTO>> getAllUpcomingCallbacks() {
        List<Callback> callbacks = callbackService.getAllUpcomingCallbacks();
        List<CallbackDTO> callbackDTOs = callbacks.stream()
                .map(callbackService::mapEntityToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(callbackDTOs);
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