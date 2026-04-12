package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "request_contact_status")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RequestContactStatus implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "request_id")
    @JsonBackReference
    private Request request;
    
    @Column(name = "contact_id", nullable = false)
    private Long contactId;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ContactStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String callNote;
    
    @Column(name = "last_call_attempt")
    private LocalDateTime lastCallAttempt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 