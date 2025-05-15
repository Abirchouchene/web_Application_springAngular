package com.example.callcenter.Service;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.Tag;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class ContactService {

    private final  TagRepository tagRepository;
    private final ContactRepository contactRepository;
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Tag createTag(Tag tag) {
        return tagRepository.save(tag);
    }

    public Contact assignTagToContact(Long contactId, Long tagId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        contact.getTags().add(tag);
        return contactRepository.save(contact);
    }
    public Contact removeTagFromContact(Long contactId, Long tagId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        contact.getTags().remove(tag);
        return contactRepository.save(contact);
    }
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }


    public Contact createContact(Contact contact) {
        return contactRepository.save(contact);
    }

    public Contact updateContact(Long id, Contact updatedContact) {
        return contactRepository.findById(id)
                .map(contact -> {
                    contact.setName(updatedContact.getName());
                    contact.setPhoneNumber(updatedContact.getPhoneNumber());
                    return contactRepository.save(contact);
                })
                .orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    public void deleteContact(Long id) {
        contactRepository.deleteById(id);
    }

    public Contact updateContactCallStatus(Long contactId, ContactStatus status, String note) {
        Contact contact = getContactById(contactId);
        if (contact != null) {
            contact.setCallStatus(status);
            contact.setCallNote(note);
            contact.setLastCallAttempt(LocalDateTime.now());
            return contactRepository.save(contact);
        }
        throw new RuntimeException("Contact not found with id: " + contactId);
    }

    public Contact getContactById(Long contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + contactId));
    }


    public ContactStatus getContactCallStatus(Long contactId) {
        Contact contact = getContactById(contactId);
        return contact.getCallStatus();
    }


    public void updateLastCallAttempt(Long contactId, LocalDateTime timestamp) {
        Contact contact = getContactById(contactId);
        contact.setLastCallAttempt(timestamp);
        contactRepository.save(contact);
    }
}




