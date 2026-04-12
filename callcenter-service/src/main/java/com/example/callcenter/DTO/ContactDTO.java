package com.example.callcenter.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {
    private String name;
    private String phoneNumber;
    private Set<Long> tagIds;
}
