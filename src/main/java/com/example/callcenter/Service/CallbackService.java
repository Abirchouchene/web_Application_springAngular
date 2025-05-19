package com.example.callcenter.Service;


import com.example.callcenter.DTO.CallbackDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.CallbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class CallbackService {
    private final CallbackRepository callbackRepository;


    public Callback scheduleCallback(CallbackDTO dto) {
        Callback callback = new Callback();
        mapDtoToEntity(dto, callback);
        return callbackRepository.save(callback);
    }

    public List<Callback> getUpcomingCallbacks(Long agentId) {
        return callbackRepository.findByAgent_IdUserAndScheduledDateAfter(agentId, LocalDateTime.now());
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
        // Set only the ID for relationships (without loading full objects)
        Contact contact = new Contact();
        contact.setIdC(dto.getContactId());
        callback.setContact(contact);

        Request request = new Request();
        request.setIdR(dto.getRequestId());
        callback.setRequest(request);

        User agent = new User();
        agent.setIdUser(dto.getAgentId());
        callback.setAgent(agent);

        callback.setScheduledDate(dto.getScheduledDate());
        callback.setNotes(dto.getNotes());
        callback.setStatus(dto.getStatus());
    }

    public CallbackDTO mapEntityToDto(Callback callback) {
        CallbackDTO dto = new CallbackDTO();
        dto.setId(callback.getId());
        dto.setContactId(callback.getContact() != null ? callback.getContact().getIdC() : null);
        dto.setRequestId(callback.getRequest() != null ? callback.getRequest().getIdR() : null);
        dto.setAgentId(callback.getAgent() != null ? callback.getAgent().getIdUser() : null);
        dto.setScheduledDate(callback.getScheduledDate());
        dto.setNotes(callback.getNotes());
        dto.setStatus(callback.getStatus());
        return dto;
    }


}