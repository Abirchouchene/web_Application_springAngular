package com.example.callcenter.Service;

import com.example.callcenter.DTO.RequestContactStatusDTO;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestContactStatus;
import com.example.callcenter.Repository.RequestContactStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestContactStatusService {
    
    private final RequestContactStatusRepository requestContactStatusRepository;
    
    /**
     * Get contact status for a specific request-contact pair
     */
    public RequestContactStatusDTO getContactStatus(Long requestIdR, Long contactId) {
        return requestContactStatusRepository.findByRequestIdRAndContactId(requestIdR, contactId)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    /**
     * Get all contact statuses for a specific request
     */
    public List<RequestContactStatusDTO> getContactStatusesByRequest(Long requestIdR) {
        return requestContactStatusRepository.findByRequestIdR(requestIdR)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all contact statuses for a specific contact across all requests
     */
    public List<RequestContactStatusDTO> getContactStatusesByContact(Long contactId) {
        return requestContactStatusRepository.findByContactId(contactId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Update or create contact status for a request-contact pair
     */
    @Transactional
    public RequestContactStatusDTO updateContactStatus(Long requestIdR, Long contactId, ContactStatus status, String callNote) {
        RequestContactStatus existingStatus = requestContactStatusRepository.findByRequestIdRAndContactId(requestIdR, contactId)
                .orElse(null);
        
        if (existingStatus != null) {
            // Update existing status
            existingStatus.setStatus(status);
            existingStatus.setCallNote(callNote);
            existingStatus.setLastCallAttempt(LocalDateTime.now());
            return convertToDTO(requestContactStatusRepository.save(existingStatus));
        } else {
            // Create new status
            RequestContactStatus newStatus = new RequestContactStatus();
            newStatus.setRequest(new Request()); // Set request reference
            newStatus.getRequest().setIdR(requestIdR);
            newStatus.setContactId(contactId);
            newStatus.setStatus(status);
            newStatus.setCallNote(callNote);
            newStatus.setLastCallAttempt(LocalDateTime.now());
            return convertToDTO(requestContactStatusRepository.save(newStatus));
        }
    }
    
    /**
     * Update last call attempt for a request-contact pair
     */
    @Transactional
    public void updateLastCallAttempt(Long requestIdR, Long contactId) {
        requestContactStatusRepository.updateLastCallAttempt(requestIdR, contactId, LocalDateTime.now());
    }
    
    /**
     * Initialize contact statuses for a request with all its contacts
     */
    @Transactional
    public void initializeContactStatuses(Long requestIdR, List<Long> contactIds) {
        for (Long contactId : contactIds) {
            // Only create if doesn't exist
            if (!requestContactStatusRepository.findByRequestIdRAndContactId(requestIdR, contactId).isPresent()) {
                RequestContactStatus status = new RequestContactStatus();
                status.setRequest(new Request());
                status.getRequest().setIdR(requestIdR);
                status.setContactId(contactId);
                status.setStatus(ContactStatus.NOT_CONTACTED);
                status.setCallNote("");
                requestContactStatusRepository.save(status);
            }
        }
    }
    
    /**
     * Get contact statuses as a map for easy lookup
     */
    public Map<Long, ContactStatus> getContactStatusMap(Long requestIdR) {
        return requestContactStatusRepository.findByRequestIdR(requestIdR)
                .stream()
                .collect(Collectors.toMap(
                    RequestContactStatus::getContactId,
                    RequestContactStatus::getStatus
                ));
    }
    
    /**
     * Delete all contact statuses for a request
     */
    @Transactional
    public void deleteByRequestIdR(Long requestIdR) {
        requestContactStatusRepository.deleteByRequestIdR(requestIdR);
    }
    
    /**
     * Delete all contact statuses for a contact
     */
    @Transactional
    public void deleteByContactId(Long contactId) {
        requestContactStatusRepository.deleteByContactId(contactId);
    }
    
    /**
     * Convert entity to DTO
     */
    private RequestContactStatusDTO convertToDTO(RequestContactStatus entity) {
        return new RequestContactStatusDTO(
            entity.getId(),
            entity.getRequest().getIdR(),
            entity.getContactId(),
            entity.getStatus(),
            entity.getCallNote(),
            entity.getLastCallAttempt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
} 