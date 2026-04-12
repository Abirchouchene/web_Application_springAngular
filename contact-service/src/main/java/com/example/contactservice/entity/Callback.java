package com.example.contactservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Callback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    private Long requestId;
    private Long agentId;
    private LocalDateTime scheduledDate;
    private String notes;

    @Enumerated(EnumType.STRING)
    private CallbackStatus status;
    
    @Column(name = "notification_sent", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean notificationSent = false;
    
    @ManyToOne
    @JoinColumn(name = "contact_id")
    @JsonBackReference
    private Contact contact;
} 