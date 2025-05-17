package com.example.callcenter.Service.Impl;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactServiceImpl(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Override
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

    @Override
    public Contact getContactById(Long contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + contactId));
    }

    @Override
    public ContactStatus getContactCallStatus(Long contactId) {
        Contact contact = getContactById(contactId);
        return contact.getCallStatus();
    }

    @Override
    public void updateLastCallAttempt(Long contactId, LocalDateTime timestamp) {
        Contact contact = getContactById(contactId);
        contact.setLastCallAttempt(timestamp);
        contactRepository.save(contact);
    }
} 