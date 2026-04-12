package com.example.contactservice.controller;

import com.example.contactservice.dto.ContactDTO;
import com.example.contactservice.entity.Contact;
import com.example.contactservice.entity.ContactStatus;
import com.example.contactservice.entity.Tag;
import com.example.contactservice.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactService.getAllContacts();
    }


    @GetMapping("/{contactId}")
    public ResponseEntity<Contact> getContact(@PathVariable Long contactId) {
        return ResponseEntity.ok(contactService.getContactById(contactId));
    }

    @GetMapping("/{contactId}/status")
    public ResponseEntity<ContactStatus> getContactStatus(@PathVariable Long contactId) {
        return ResponseEntity.ok(contactService.getContactCallStatus(contactId));
    }

    @PutMapping("/{contactId}/status")
    public ResponseEntity<Contact> updateContactStatus(
            @PathVariable Long contactId,
            @RequestParam ContactStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(contactService.updateContactCallStatus(contactId, status, note));
    }

    @PutMapping("/{contactId}/last-call")
    public ResponseEntity<Void> updateLastCallAttempt(
            @PathVariable Long contactId,
            @RequestParam(required = false) LocalDateTime timestamp) {
        contactService.updateLastCallAttempt(contactId, timestamp != null ? timestamp : LocalDateTime.now());
        return ResponseEntity.ok().build();
    }
    @PostMapping
    public Contact createContact(@RequestBody ContactDTO dto) {
        return contactService.createContact(dto);
    }
    @PutMapping("/{id}")
    public Contact updateContact(@PathVariable Long id, @RequestBody ContactDTO dto) {
        return contactService.updateContact(id, dto);
    }
    @DeleteMapping("/{id}")
    public void deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
    }

    @PostMapping("/tags")
    public Tag createTag(@RequestBody Tag tag) {
        return contactService.createTag(tag);
    }

    // Get all tags
    @GetMapping("/tags/All")
    public List<Tag> getAllTags() {
        return contactService.getAllTags();
    }

    // Assign tag to contact
    @PutMapping("/{contactId}/tags/{tagId}")
    public Contact assignTag(@PathVariable Long contactId, @PathVariable Long tagId) {
        return contactService.assignTagToContact(contactId, tagId);
    }

    // Remove tag from contact
    @DeleteMapping("/{contactId}/tags/{tagId}")
    public Contact removeTag(@PathVariable Long contactId, @PathVariable Long tagId) {
        return contactService.removeTagFromContact(contactId, tagId);
    }
    @GetMapping("/tags/searchByTag")
    public List<Contact> getContactsByTag(@RequestParam String tag) {
        return contactService.getContactsByTag(tag);
    }

}

