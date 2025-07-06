package com.example.callcenter.Controller;

import com.example.callcenter.DTO.AgentAvailabilityDTO;
import com.example.callcenter.DTO.QuestionDTO;
import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.DTO.RequestResponseDTO;
import com.example.callcenter.DTO.UpdateRequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Service.RequestService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
public class RequestController {

    private final RequestService requestService;
    
    // Helper method to convert Request to RequestResponseDTO
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
        dto.setReport(request.getReport());
        dto.setSubmissionList(request.getSubmissionList());
        return dto;
    }
    
    @PostMapping("/submit")
    public ResponseEntity<RequestResponseDTO> submitRequest(@RequestBody RequestDTO requestDTO) {
        Request createdRequest = requestService.submitRequest(requestDTO);
        return ResponseEntity.ok(convertToResponseDTO(createdRequest));
    }

    @GetMapping("/All")
    public List<RequestResponseDTO> getAllRequests() {
        List<Request> requests = requestService.getAllRequests();
        return requests.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getQuestionsByCategoryAndType(
            @RequestParam(required = false) CategoryRequest category,
            @RequestParam(required = false) QuestionType questionType) {
        List<Question> questions = requestService.getQuestionsByCategoryAndType(category, questionType);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public RequestResponseDTO getRequestById(@PathVariable Long id) {
        Request request = requestService.getRequestById(id);
        return convertToResponseDTO(request);
    }
    
    @GetMapping("/user/{userId}")
    public List<RequestResponseDTO> getRequestsByUserId(@PathVariable Long userId) {
        List<Request> requests = requestService.getRequestsByUserId(userId);
        return requests.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/type/{type}")
    public List<RequestResponseDTO> getRequestsByType(@PathVariable RequestType type) {
        List<Request> requests = requestService.getRequestsByType(type);
        return requests.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/agent/availability")
    public List<AgentAvailabilityDTO> getAgentsWithAvailability(@RequestParam("date") String dateStr) {
        LocalDate selectedDate = LocalDate.parse(dateStr);
        return requestService.getAllAgentsWithAvailability(selectedDate);
    }

    @PutMapping("/{requestId}/approve")
    public ResponseEntity<RequestResponseDTO> approveRequest(@PathVariable Long requestId,
                                                  @RequestParam Status status) {
        try {
            Request updatedRequest = requestService.approveRequest(requestId, status);
            return ResponseEntity.ok(convertToResponseDTO(updatedRequest));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{requestId}/assign")
    public ResponseEntity<RequestResponseDTO> assignRequestToAgent(@PathVariable Long requestId,
                                                        @RequestParam Long agentId) {
        try {
            Request updatedRequest = requestService.assignRequestToAgent(requestId, agentId);
            return ResponseEntity.ok(convertToResponseDTO(updatedRequest));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/requester/{requestId}/update")
    public ResponseEntity<RequestResponseDTO> updateRequestByRequester(
            @PathVariable Long requestId,
            @RequestBody UpdateRequestDTO dto,
            @RequestParam Long requesterId) {
        Request updated = requestService.updateRequestByRequester(requestId, dto, requesterId);
        return ResponseEntity.ok(convertToResponseDTO(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRequest(@PathVariable Long id) {
        requestService.deleteRequest(id);
        return ResponseEntity.ok("Request deleted successfully");
    }

    @GetMapping("/agents")
    public ResponseEntity<List<User>> getAgents() {
        List<User> agents = requestService.getUsersByRole(Role.AGENT);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/assigned/{agentId}")
    public List<RequestResponseDTO> getAssignedRequests(@PathVariable Long agentId) {
        List<Request> requests = requestService.getRequestsAssignedToAgent(agentId);
        return requests.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    @PutMapping("/{requestId}/update-status")
    public RequestResponseDTO updateRequestStatus(@PathVariable Long requestId,
                                       @RequestParam Status newStatus) {
        Request request = requestService.updateRequestStatus(requestId, newStatus);
        return convertToResponseDTO(request);
    }
    
    @PutMapping("/{id}/update-note")
    public ResponseEntity<RequestResponseDTO> updateNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String note = body.get("note");
        Request updatedRequest = requestService.updateNote(id, note);
        return ResponseEntity.ok(convertToResponseDTO(updatedRequest));
    }
}
