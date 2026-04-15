package com.example.callcenter.Controller;

import com.example.callcenter.DTO.AgentAvailabilityDTO;
import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.DTO.RequestResponseDTO;
import com.example.callcenter.DTO.UpdateRequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Service.RequestService;
import com.example.callcenter.Service.AutoGenerateSurveyService;
import com.example.callcenter.client.ContactClient;
import com.example.callcenter.DTO.ContactResponse;
import com.example.callcenter.Entity.RequestContactStatus;
import com.example.callcenter.Repository.RequestContactStatusRepository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final ContactClient contactClient;
    private final AutoGenerateSurveyService autoGenerateSurveyService;
    private final RequestContactStatusRepository requestContactStatusRepository;

    // Convert entity to DTO
    private RequestResponseDTO convertToResponseDTO(Request request) {
        RequestResponseDTO dto = new RequestResponseDTO();
        dto.setIdR(request.getIdR());
        dto.setTitle(request.getTitle());
        dto.setDescription(request.getDescription());
        dto.setNote(request.getNote());
        dto.setAttachmentPath(request.getAttachmentPath());
        dto.setDeadline(request.getDeadline());
        dto.setUpdatedAt(request.getUpdatedAt());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setStatus(request.getStatus());
        dto.setRequestType(request.getRequestType());
        dto.setCategoryRequest(request.getCategoryRequest());
        dto.setPriority(request.getPriority());
        dto.setUser(request.getUser());
        dto.setQuestions(request.getQuestions());
        dto.setAgent(request.getAgent());

        // Safely handle Report to avoid circular ref (Report -> Request)
        Report report = request.getReport();
        if (report != null) {
            Report safeReport = new Report();
            safeReport.setId(report.getId());
            safeReport.setRequestTitle(report.getRequestTitle());
            safeReport.setRequestType(report.getRequestType());
            safeReport.setGeneratedDate(report.getGeneratedDate());
            safeReport.setStatus(report.getStatus());
            safeReport.setApprovedDate(report.getApprovedDate());
            safeReport.setSentDate(report.getSentDate());
            safeReport.setTotalContacts(report.getTotalContacts());
            safeReport.setContactedContacts(report.getContactedContacts());
            safeReport.setContactRate(report.getContactRate());
            safeReport.setStatisticsData(report.getStatisticsData());
            // Do NOT set safeReport.setRequest() to avoid circular ref
            dto.setReport(safeReport);
        }

        // Safely load submissions (field has @JsonIgnore on entity)
        try {
            dto.setSubmissionList(request.getSubmissionList());
        } catch (Exception e) {
            dto.setSubmissionList(new java.util.ArrayList<>());
        }

        // Load contacts via RequestContactStatus (reliable) + Feign for full details
        try {
            List<RequestContactStatus> statuses = requestContactStatusRepository.findByRequestIdR(request.getIdR());
            if (statuses != null && !statuses.isEmpty()) {
                List<ContactResponse> contacts = statuses.stream()
                        .map(rcs -> {
                            ContactResponse cr;
                            try {
                                cr = contactClient.getContactById(rcs.getContactId());
                            } catch (Exception ex) {
                                cr = new ContactResponse();
                                cr.setIdC(rcs.getContactId());
                            }
                            // Override with per-request call status from RequestContactStatus
                            if (rcs.getStatus() != null) {
                                cr.setCallStatus(rcs.getStatus().name());
                            }
                            if (rcs.getCallNote() != null) {
                                cr.setCallNote(rcs.getCallNote());
                            }
                            return cr;
                        })
                        .collect(Collectors.toList());
                dto.setContacts(contacts);
            } else {
                dto.setContacts(new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            dto.setContacts(new java.util.ArrayList<>());
        }
        return dto;
    }

    // CREATE REQUEST
    @PostMapping("/submit")
    public ResponseEntity<RequestResponseDTO> submitRequest(@RequestBody RequestDTO requestDTO) {
        Request createdRequest = requestService.submitRequest(requestDTO);
        return ResponseEntity.ok(convertToResponseDTO(createdRequest));
    }

    // GET ALL REQUESTS
    @GetMapping("/All")
    public List<RequestResponseDTO> getAllRequests() {
        return requestService.getAllRequests()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // GET REQUEST BY ID
    @GetMapping("/{id}")
    public RequestResponseDTO getRequestById(@PathVariable Long id) {
        return convertToResponseDTO(requestService.getRequestById(id));
    }

    // GET REQUESTS BY USER
    @GetMapping("/user/{userId}")
    public List<RequestResponseDTO> getRequestsByUserId(@PathVariable Long userId) {
        return requestService.getRequestsByUserId(userId)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // GET REQUESTS BY TYPE
    @GetMapping("/type/{type}")
    public List<RequestResponseDTO> getRequestsByType(@PathVariable RequestType type) {
        return requestService.getRequestsByType(type)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // AGENT AVAILABILITY
    @GetMapping("/agent/availability")
    public List<AgentAvailabilityDTO> getAgentsWithAvailability(@RequestParam(required = false) String date) {
        LocalDate selectedDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        return requestService.getAllAgentsWithAvailability(selectedDate);
    }

    // APPROVE REQUEST
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<RequestResponseDTO> approveRequest(
            @PathVariable Long requestId,
            @RequestParam Status status) {

        try {
            Request updated = requestService.approveRequest(requestId, status);
            return ResponseEntity.ok(convertToResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ASSIGN AGENT
    @PutMapping("/{requestId}/assign")
    public ResponseEntity<RequestResponseDTO> assignRequestToAgent(
            @PathVariable Long requestId,
            @RequestParam Long agentId) {

        try {
            Request updated = requestService.assignRequestToAgent(requestId, agentId);
            return ResponseEntity.ok(convertToResponseDTO(updated));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // UPDATE REQUEST BY REQUESTER
    @PutMapping("/requester/{requestId}/update")
    public ResponseEntity<RequestResponseDTO> updateRequestByRequester(
            @PathVariable Long requestId,
            @RequestBody UpdateRequestDTO dto,
            @RequestParam Long requesterId) {

        Request updated = requestService.updateRequestByRequester(requestId, dto, requesterId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    // DELETE REQUEST
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRequest(@PathVariable Long id) {
        requestService.deleteRequest(id);
        return ResponseEntity.ok("Request deleted successfully");
    }

    // GET AGENTS
    @GetMapping("/agents")
    public ResponseEntity<List<User>> getAgents() {
        return ResponseEntity.ok(requestService.getUsersByRole(Role.AGENT));
    }

    // GET REQUESTS ASSIGNED TO AGENT
    @GetMapping("/assigned/{agentId}")
    public List<RequestResponseDTO> getAssignedRequests(@PathVariable Long agentId) {
        return requestService.getRequestsAssignedToAgent(agentId)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    // UPDATE STATUS
    @PutMapping("/{requestId}/update-status")
    public RequestResponseDTO updateRequestStatus(
            @PathVariable Long requestId,
            @RequestParam Status newStatus) {

        return convertToResponseDTO(
                requestService.updateRequestStatus(requestId, newStatus)
        );
    }

    // UPDATE NOTE
    @PutMapping("/{id}/update-note")
    public ResponseEntity<RequestResponseDTO> updateNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String note = body.get("note");
        Request updated = requestService.updateNote(id, note);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }

    // GET ALL CONTACTS (proxy vers contact-service via Feign)
    @GetMapping("/Contacts")
    public ResponseEntity<List<ContactResponse>> getAllContacts() {
        return ResponseEntity.ok(contactClient.getAllContacts());
    }

    // SEARCH CONTACTS BY TAG (proxy vers contact-service via Feign)
    @GetMapping("/searchByTag")
    public ResponseEntity<List<ContactResponse>> getContactsByTag(@RequestParam String tag) {
        return ResponseEntity.ok(contactClient.getContactsByTag(tag));
    }

    // AUTO-GENERATE SURVEY (manual trigger)
    @PostMapping("/auto-generate")
    public ResponseEntity<RequestResponseDTO> autoGenerateSurvey(@RequestParam Long userId) {
        Request generated = autoGenerateSurveyService.generateSurveyNow(userId);
        return ResponseEntity.ok(convertToResponseDTO(generated));
    }
}