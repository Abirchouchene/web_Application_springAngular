package com.example.callcenter.Service;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.Tag;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private TagRepository tagRepository;

    // ===== CONTACT =====

    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    public Contact updateContact(Long id, Contact updated) {
        Contact existing = getContactById(id);
        existing.setName(updated.getName());
        existing.setPhoneNumber(updated.getPhoneNumber());
        return contactRepository.save(existing);
    }

    public void deleteContact(Long id) {
        contactRepository.deleteById(id);
    }

    public Contact updateContactCallStatus(Long contactId, ContactStatus status, String note) {
        Contact contact = getContactById(contactId);
        contact.setCallStatus(status);
        contact.setCallNote(note);
        contact.setLastCallAttempt(LocalDateTime.now());
        return contactRepository.save(contact);
    }

    public Contact getContactById(Long contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + contactId));
    }

    public ContactStatus getContactCallStatus(Long contactId) {
        return getContactById(contactId).getCallStatus();
    }

    public void updateLastCallAttempt(Long contactId, LocalDateTime timestamp) {
        Contact contact = getContactById(contactId);
        contact.setLastCallAttempt(timestamp);
        contactRepository.save(contact);
    }

    // ===== TAG =====

    public Tag createTag(Tag tag) {
        return tagRepository.findByName(tag.getName())
                .orElseGet(() -> tagRepository.save(tag));
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tagId));
        // Detach from all contacts before deleting
        for (Contact contact : tag.getContacts()) {
            contact.getTags().remove(tag);
            contactRepository.save(contact);
        }
        tagRepository.delete(tag);
    }

    public Contact assignTagToContact(Long contactId, Long tagId) {
        Contact contact = getContactById(contactId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tagId));
        contact.getTags().add(tag);
        return contactRepository.save(contact);
    }

    public Contact removeTagFromContact(Long contactId, Long tagId) {
        Contact contact = getContactById(contactId);
        contact.getTags().removeIf(t -> t.getId().equals(tagId));
        return contactRepository.save(contact);
    }
}
