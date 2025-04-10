package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idR;

    private String description;
    private String note;
    private String attachmentPath;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    private CatgoryRequest catgoryRequest;

    @Enumerated(EnumType.STRING)
    private Priority priority;  // ✅ Ensure this field exists

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToMany
    private Set<Contact> contacts = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<Question> questions = new HashSet<>();
    @ManyToOne
    @JoinColumn(name = "agent_id")
    // This will store the assigned agent
    private User agent;
    @ElementCollection
    private Set<String> tags = new HashSet<>();
}


