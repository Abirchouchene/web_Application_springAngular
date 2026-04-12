package com.example.callcenter.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {
    private Long id;
    private String sub;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private List<String> realmRoles;
    private List<String> groups;
}
