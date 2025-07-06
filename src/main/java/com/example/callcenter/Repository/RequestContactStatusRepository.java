package com.example.callcenter.Repository;

import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.RequestContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestContactStatusRepository extends JpaRepository<RequestContactStatus, Long> {
    
    // Find all statuses for a specific contact across all requests
    List<RequestContactStatus> findByContactId(Long contactId);
    
    // Find by request idR and contactId
    Optional<RequestContactStatus> findByRequestIdRAndContactId(Long requestIdR, Long contactId);
    
    // Find all statuses for a specific request by idR
    List<RequestContactStatus> findByRequestIdR(Long requestIdR);
    
    // Find by request ID and status
    List<RequestContactStatus> findByRequestIdRAndStatus(Long requestIdR, ContactStatus status);
    
    // Update status for a specific request-contact pair
    @Modifying
    @Query("UPDATE RequestContactStatus rcs SET rcs.status = :status, rcs.callNote = :callNote, rcs.lastCallAttempt = :lastCallAttempt WHERE rcs.request.idR = :requestIdR AND rcs.contactId = :contactId")
    void updateStatus(@Param("requestIdR") Long requestIdR, 
                     @Param("contactId") Long contactId, 
                     @Param("status") ContactStatus status, 
                     @Param("callNote") String callNote,
                     @Param("lastCallAttempt") LocalDateTime lastCallAttempt);
    
    // Update last call attempt
    @Modifying
    @Query("UPDATE RequestContactStatus rcs SET rcs.lastCallAttempt = :lastCallAttempt WHERE rcs.request.idR = :requestIdR AND rcs.contactId = :contactId")
    void updateLastCallAttempt(@Param("requestIdR") Long requestIdR, 
                              @Param("contactId") Long contactId, 
                              @Param("lastCallAttempt") LocalDateTime lastCallAttempt);
    
    // Delete all statuses for a specific request
    void deleteByRequestIdR(Long requestIdR);
    
    // Delete all statuses for a specific contact
    void deleteByContactId(Long contactId);
} 