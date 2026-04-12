package com.example.callcenter.DTO;

import com.example.callcenter.Entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserDTO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private Role role;
}
