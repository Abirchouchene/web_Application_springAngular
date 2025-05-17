package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idC;
    private String name;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private ContactStatus callStatus = ContactStatus.NOT_CONTACTED;
    
    private String callNote;
    private LocalDateTime lastCallAttempt;

    @ManyToMany(mappedBy = "contacts")
    @JsonBackReference
    private Set<Request> requests;

    @ManyToMany(mappedBy = "contacts")
    private Set<Tag> tags = new HashSet<>();
} 