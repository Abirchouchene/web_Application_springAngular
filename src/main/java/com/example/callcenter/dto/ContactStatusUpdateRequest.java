package com.example.callcenter.dto;

import com.example.callcenter.Entity.ContactStatus;
import lombok.Data;

@Data
public class ContactStatusUpdateRequest {
    private ContactStatus status;
    private String note;
} 