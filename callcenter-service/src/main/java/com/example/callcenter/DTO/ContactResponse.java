package com.example.callcenter.DTO;

import lombok.Data;
import java.util.Set;

@Data
public class ContactResponse {
    private Long idC;
    private String name;
    private String phoneNumber;
    private String callStatus;
    private String callNote;
    private String lastCallAttempt;
    private Set<Long> tagIds;
} 