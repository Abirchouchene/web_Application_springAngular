package com.example.callcenter.Controller;

import com.example.callcenter.DTO.RequestContactStatusDTO;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Service.RequestContactStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/request-contact-status")
@RequiredArgsConstructor
//@CrossOrigin("http://localhost:4200")
public class RequestContactStatusController {

    private final RequestContactStatusService requestContactStatusService;

    /**
     * Get contact status for a specific request-contact pair
     */
    @GetMapping("/{requestId}/{contactId}")
    public ResponseEntity<RequestContactStatusDTO> getContactStatus(
            @PathVariable("requestId") Long requestIdR,
            @PathVariable Long contactId) {
        RequestContactStatusDTO status = requestContactStatusService.getContactStatus(requestIdR, contactId);
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all contact statuses for a specific request
     */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<RequestContactStatusDTO>> getContactStatusesByRequest(
            @PathVariable("requestId") Long requestIdR) {
        List<RequestContactStatusDTO> statuses = requestContactStatusService.getContactStatusesByRequest(requestIdR);
        return ResponseEntity.ok(statuses);
    }

    /**
     * Get all contact statuses for a specific contact across all requests
     */
    @GetMapping("/contact/{contactId}")
    public ResponseEntity<List<RequestContactStatusDTO>> getContactStatusesByContact(
            @PathVariable Long contactId) {
        List<RequestContactStatusDTO> statuses = requestContactStatusService.getContactStatusesByContact(contactId);
        return ResponseEntity.ok(statuses);
    }

    /**
     * Update contact status for a request-contact pair
     */
    @PutMapping("/{requestId}/{contactId}")
    public ResponseEntity<RequestContactStatusDTO> updateContactStatus(
            @PathVariable("requestId") Long requestIdR,
            @PathVariable Long contactId,
            @RequestParam ContactStatus status,
            @RequestParam(required = false, defaultValue = "") String callNote) {
        
        RequestContactStatusDTO updatedStatus = requestContactStatusService.updateContactStatus(
            requestIdR, contactId, status, callNote);
        return ResponseEntity.ok(updatedStatus);
    }

    /**
     * Update last call attempt for a request-contact pair
     */
    @PutMapping("/{requestId}/{contactId}/call-attempt")
    public ResponseEntity<String> updateLastCallAttempt(
            @PathVariable("requestId") Long requestIdR,
            @PathVariable Long contactId) {
        
        requestContactStatusService.updateLastCallAttempt(requestIdR, contactId);
        return ResponseEntity.ok("Last call attempt updated successfully");
    }

    /**
     * Initialize contact statuses for a request
     */
    @PostMapping("/{requestId}/initialize")
    public ResponseEntity<String> initializeContactStatuses(
            @PathVariable("requestId") Long requestIdR,
            @RequestBody List<Long> contactIds) {
        
        requestContactStatusService.initializeContactStatuses(requestIdR, contactIds);
        return ResponseEntity.ok("Contact statuses initialized successfully");
    }

    /**
     * Get contact statuses as a map for easy lookup
     */
    @GetMapping("/{requestId}/map")
    public ResponseEntity<Map<Long, ContactStatus>> getContactStatusMap(
            @PathVariable("requestId") Long requestIdR) {
        
        Map<Long, ContactStatus> statusMap = requestContactStatusService.getContactStatusMap(requestIdR);
        return ResponseEntity.ok(statusMap);
    }

    /**
     * Delete all contact statuses for a request
     */
    @DeleteMapping("/request/{requestId}")
    public ResponseEntity<String> deleteByRequestId(@PathVariable("requestId") Long requestIdR) {
        requestContactStatusService.deleteByRequestIdR(requestIdR);
        return ResponseEntity.ok("Contact statuses deleted successfully");
    }

    /**
     * Delete all contact statuses for a contact
     */
    @DeleteMapping("/contact/{contactId}")
    public ResponseEntity<String> deleteByContactId(@PathVariable Long contactId) {
        requestContactStatusService.deleteByContactId(contactId);
        return ResponseEntity.ok("Contact statuses deleted successfully");
    }
} 