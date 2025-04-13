package com.example.callcenter.Controller;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
public class RequestController {

    private final RequestService requestService;

    @PostMapping("/submit")

    public ResponseEntity<Request> submitRequest(
            @RequestParam Long userId,
            @RequestParam RequestType requestType,
            @RequestParam List<Long> contactIds,
            @RequestParam String description,
            @RequestParam CatgoryRequest category,
            @RequestParam(required = false) List<Long> questionIds,
            @RequestParam(required = false) List<String> newQuestions,
            @RequestParam Priority priorityLevel, @RequestParam LocalDate deadline,
            @RequestParam (required = false)QuestionType defaultQuestionType
               , @RequestParam(value = "file",required = false) MultipartFile file
    ) {
        Request request = requestService.submitRequest(userId, requestType, contactIds, description,
                        category, questionIds, newQuestions, priorityLevel,
                defaultQuestionType, deadline, file);
        return ResponseEntity.ok(request);
    }
    @GetMapping("/All")
    public List<Request> getAllRequests() {
        return requestService.getAllRequests();
    }

    @GetMapping("/{id}")
    public Request getRequestById(@PathVariable Long id) {
        return requestService.getRequestById(id);
    }

    @GetMapping("/type/{type}")
    public List<Request> getRequestsByType(@PathVariable RequestType type) {
        return requestService.getRequestsByType(type);
    }
    @PostMapping("/{contactId}/addTag")
    public ResponseEntity<Contact> addTagToContact(@PathVariable Long contactId, @RequestParam String tag) {
        Contact contact = requestService.addTagToContact(contactId, tag);
        if (contact != null) {
            return ResponseEntity.ok(contact);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/searchByTag")
    public ResponseEntity<List<Contact>> searchContactsByTag(@RequestParam String tag) {
        List<Contact> contacts = requestService.searchContactsByTag(tag);
        return ResponseEntity.ok(contacts);
    }
    // Endpoint to approve or reject a request
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<Request> approveRequest(@PathVariable Long requestId,
                                                  @RequestParam Status status) {
        try {
            Request updatedRequest = requestService.approveRequest(requestId, status);
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // Bad request if status is invalid
        }
    }

    // Endpoint to assign a request to an agent
    @PutMapping("/{requestId}/assign")
    public ResponseEntity<Request> assignRequestToAgent(@PathVariable Long requestId,
                                                        @RequestParam Long agentId) {
        try {
            Request updatedRequest = requestService.assignRequestToAgent(requestId, agentId);
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // If request isn't approved yet
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); // Not found for agent or request
        }
    }
    @PutMapping("/requester/{requestId}/update")
    public ResponseEntity<Request> updateRequestByRequester(
            @PathVariable Long requestId,
            @RequestBody UpdateRequestDTO dto,
            @RequestParam Long requesterId) {

        Request updated = requestService.updateRequestByRequester(requestId, dto, requesterId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/Contacts")
    public  List<Contact>getAllContacts(){
        return requestService.getAllContacts();
    }
    @GetMapping("/agents")
    public ResponseEntity<List<User>> getAgents() {
        List<User> agents = requestService.getUsersByRole(Role.AGENT);  // Fetching users with AGENT role
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/assigned/{agentId}")
    public List<Request> getAssignedRequests(@PathVariable Long agentId) {
        return requestService.getRequestsAssignedToAgent(agentId);
    }
    @PutMapping("/{requestId}/update-status")
    public Request updateRequestStatus(@PathVariable Long requestId
,                                       @RequestParam Status newStatus) {
        return requestService.updateRequestStatus(requestId, newStatus);
    }
    /*@PutMapping("/{requestId}/update")
    public ResponseEntity<Request> updateRequestStatus(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> updateData) {

        String status = updateData.get("status");
        String note = updateData.get("note");

        Request request = requestService.updateRequestStatus(requestId, status, note);
        return ResponseEntity.ok(request);
    }
*/
}
