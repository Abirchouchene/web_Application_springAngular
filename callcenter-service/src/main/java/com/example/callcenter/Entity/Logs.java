package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Logs implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    @JsonIgnoreProperties({"logs", "submissionList", "report"})
    private Request request;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LogAction logAction;

    @Column(length = 100)
    private String actionDescription;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status newStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority oldPriority;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority newPriority;

    @Column(length = 100)
    private String oldAssignedAgent;

    @Column(length = 100)
    private String newAssignedAgent;

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;
}
