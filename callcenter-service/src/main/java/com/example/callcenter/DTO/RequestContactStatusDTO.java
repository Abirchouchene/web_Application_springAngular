package com.example.callcenter.DTO;

import com.example.callcenter.Entity.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestContactStatusDTO {
    private Long id;
    private Long requestId;
    private Long contactId;
    private ContactStatus status;
    private String callNote;
    private LocalDateTime lastCallAttempt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static
    class UpdateContactStatusRequest {
        private Long requestId;
        private Long contactId;
        private ContactStatus status;
        private String callNote;
    }
}

