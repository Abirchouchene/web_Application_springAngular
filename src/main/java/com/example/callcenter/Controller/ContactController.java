package com.example.callcenter.Controller;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.Tag;
import com.example.callcenter.Service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contacts")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true", 
    allowedHeaders = {"Content-Type", "Authorization", "Accept"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, 
               RequestMethod.DELETE, RequestMethod.OPTIONS})
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
            @RequestParam(required = false) String timestamp) {
        LocalDateTime dateTime;
        if (timestamp != null) {
            try {
                dateTime = ZonedDateTime.parse(timestamp).toLocalDateTime();
            } catch (Exception e) {
                dateTime = LocalDateTime.now();
            }
        } else {
            dateTime = LocalDateTime.now();
        }
        contactService.updateLastCallAttempt(contactId, dateTime);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public Contact createContact(@RequestBody Contact contact) {
        return contactService.createContact(contact);
    }

    @PutMapping("/{id}")
    public Contact updateContact(@PathVariable Long id, @RequestBody Contact contact) {
        return contactService.updateContact(id, contact);
    }

    @DeleteMapping("/{id}")
    public void deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
    }

    @PostMapping("/tags")
    public Tag createTag(@RequestBody Tag tag) {
        return contactService.createTag(tag);
    }

    @GetMapping("/tags/All")
    public List<Tag> getAllTags() {
        return contactService.getAllTags();
    }

    @PutMapping("/{contactId}/tags/{tagId}")
    public Contact assignTag(@PathVariable Long contactId, @PathVariable Long tagId) {
        return contactService.assignTagToContact(contactId, tagId);
    }

    @DeleteMapping("/{contactId}/tags/{tagId}")
    public Contact removeTag(@PathVariable Long contactId, @PathVariable Long tagId) {
        return contactService.removeTagFromContact(contactId, tagId);
    }
} 