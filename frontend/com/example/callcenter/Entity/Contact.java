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
    private ContactStatus callStatus = ContactStatus.NOT_CONTACTED;  // Default status
    
    private String callNote;  // Note about the call
    private LocalDateTime lastCallAttempt;  // Timestamp of last call attempt

    @ManyToMany(mappedBy = "contacts")
    @JsonBackReference
    private Set<Request> requests;

    @ManyToMany(mappedBy = "contacts")
    private Set<Tag> tags = new HashSet<>();
}

// Add this enum in a separate file named ContactStatus.java
@Getter
public enum ContactStatus {
    NOT_CONTACTED("Not Contacted"),
    CONTACTED_AVAILABLE("Contacted - Available"),
    CONTACTED_UNAVAILABLE("Contacted - Unavailable"),
    NO_ANSWER("No Answer"),
    WRONG_NUMBER("Wrong Number"),
    CALL_BACK_LATER("Call Back Later");

    private final String label;

    ContactStatus(String label) {
        this.label = label;
    }
} 