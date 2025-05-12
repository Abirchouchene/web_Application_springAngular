package com.example.callcenter.Controller;


import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.Tag;
import com.example.callcenter.Service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/api/contacts")
@CrossOrigin(origins = "*")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactService.getAllContacts();
    }

    @GetMapping("/{id}")
    public Contact getContactById(@PathVariable Long id) {
        return contactService.getContactById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
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
}

