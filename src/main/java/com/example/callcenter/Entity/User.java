package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL)
    private Set<AgentLeave> leaves = new HashSet<>();
    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference

    private List<Callback> callbacks = new ArrayList<>();
    @OneToMany(mappedBy = "agent")
    @JsonManagedReference
    private  List<Notification>notifications;

}
