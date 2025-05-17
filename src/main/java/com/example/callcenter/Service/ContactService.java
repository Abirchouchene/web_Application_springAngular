package com.example.callcenter.Service;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    public Contact updateContactCallStatus(Long contactId, ContactStatus status, String note) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            contact.setCallStatus(status);
            contact.setCallNote(note);
            contact.setLastCallAttempt(LocalDateTime.now());
            return contactRepository.save(contact);
        }
        throw new RuntimeException("Contact not found with id: " + contactId);
    }

    public Contact getContactById(Long contactId) {
        Optional<Contact> contact = contactRepository.findById(contactId);
        if (contact.isPresent()) {
            return contact.get();
        }
        throw new RuntimeException("Contact not found with id: " + contactId);
    }

    public ContactStatus getContactCallStatus(Long contactId) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isPresent()) {
            return contactOpt.get().getCallStatus();
        }
        throw new RuntimeException("Contact not found with id: " + contactId);
    }

    public void updateLastCallAttempt(Long contactId, LocalDateTime timestamp) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (contactOpt.isPresent()) {
            Contact contact = contactOpt.get();
            contact.setLastCallAttempt(timestamp);
            contactRepository.save(contact);
        } else {
            throw new RuntimeException("Contact not found with id: " + contactId);
        }
    }
} 