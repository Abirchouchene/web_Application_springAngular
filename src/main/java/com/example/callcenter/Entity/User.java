package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter

public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long idUser;
    private String fullName;
    @Enumerated(EnumType.STRING)
    private Role role;
    @OneToMany(mappedBy = "user")

    private Set<Request>requests;
    @OneToMany(mappedBy = "agent")
    @JsonBackReference
    private Set<Request> assignedRequests;
}
