package com.example.contactservice.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ContactDTO {
    private String name;
    private String phoneNumber;
    private Set<Long> tagIds;
} 