package com.example.contactservice.service;

import com.example.contactservice.dto.ContactDTO;
import com.example.contactservice.entity.Contact;
import com.example.contactservice.entity.ContactStatus;
import com.example.contactservice.entity.Tag;
import com.example.contactservice.repository.ContactRepository;
import com.example.contactservice.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {
    private final TagRepository tagRepository;
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

    @Transactional
    public Contact createContact(ContactDTO dto) {
        Contact contact = new Contact();
        contact.setName(dto.getName());
        contact.setPhoneNumber(dto.getPhoneNumber());
        contact.setCallStatus(ContactStatus.NOT_CONTACTED);
        contact.setLastCallAttempt(null);

        contact = contactRepository.save(contact);

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            Set<Tag> tags = dto.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tagId)))
                    .collect(Collectors.toSet());
            contact.setTags(tags);
            contact = contactRepository.save(contact);
        }

        return contact;
    }

    @Transactional
    public Contact updateContact(Long id, ContactDTO dto) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contact.setName(dto.getName());
        contact.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getTagIds() != null) {
            Set<Tag> tags = dto.getTagIds().stream()
                    .map(tagId -> tagRepository.findById(tagId)
                            .orElseThrow(() -> new RuntimeException("Tag not found with id: " + tagId)))
                    .collect(Collectors.toSet());
            contact.getTags().clear();
            contact.getTags().addAll(tags);
        }

        return contactRepository.save(contact);
    }

    @Transactional
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

    public List<Contact> getContactsByTag(String tagName) {
        return contactRepository.findByTagNameLike(tagName);
    }

    public void updateLastCallAttempt(Long contactId, LocalDateTime timestamp) {
        Contact contact = getContactById(contactId);
        contact.setLastCallAttempt(timestamp);
        contactRepository.save(contact);
    }
} 