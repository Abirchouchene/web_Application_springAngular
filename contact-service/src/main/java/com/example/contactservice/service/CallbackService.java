package com.example.contactservice.service;

import com.example.contactservice.dto.CallbackDTO;
import com.example.contactservice.entity.Callback;
import com.example.contactservice.entity.CallbackStatus;
import com.example.contactservice.entity.Contact;
import com.example.contactservice.repository.CallbackRepository;
import com.example.contactservice.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallbackService {
    private final CallbackRepository callbackRepository;
    private final ContactRepository contactRepository;

    public Callback scheduleCallback(CallbackDTO dto) {
        Callback callback = new Callback();
        mapDtoToEntity(dto, callback);
        return callbackRepository.save(callback);
    }

    public List<Callback> getUpcomingCallbacks(Long agentId) {
        return callbackRepository.findByAgentIdAndScheduledDateAfter(agentId, LocalDateTime.now());
    }
    
    public List<Callback> getAllUpcomingCallbacks() {
        return callbackRepository.findByStatusAndScheduledDateAfter(
            CallbackStatus.SCHEDULED,
            LocalDateTime.now()
        );
    }

    public List<Callback> getAllCallbacks() {
        return callbackRepository.findAll();
    }

    public Callback updateCallbackStatus(Long id, CallbackStatus status) {
        Callback callback = callbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Callback not found with id: " + id));
        callback.setStatus(status);
        return callbackRepository.save(callback);
    }

    public void deleteCallback(Long id) {
        callbackRepository.deleteById(id);
    }

    private void mapDtoToEntity(CallbackDTO dto, Callback callback) {
        Contact contact = contactRepository.findById(dto.getContactId())
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + dto.getContactId()));
        callback.setContact(contact);
        callback.setRequestId(dto.getRequestId());
        callback.setAgentId(dto.getAgentId());
        callback.setScheduledDate(dto.getScheduledDate());
        callback.setNotes(dto.getNotes());
        callback.setStatus(dto.getStatus());
    }

    public CallbackDTO mapEntityToDto(Callback callback) {
        CallbackDTO dto = new CallbackDTO();
        dto.setId(callback.getId());
        dto.setContactId(callback.getContact().getIdC());
        dto.setRequestId(callback.getRequestId());
        dto.setAgentId(callback.getAgentId());
        dto.setScheduledDate(callback.getScheduledDate());
        dto.setNotes(callback.getNotes());
        dto.setStatus(callback.getStatus());
        return dto;
    }
} 