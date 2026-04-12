package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private String title;
    private String description;
    private String note;
    private String attachmentPath;
    private LocalDate deadline;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CategoryRequest categoryRequest;

    @Convert(converter = com.example.callcenter.Config.PriorityConverter.class)
    @Column(length = 20)
    private Priority priority;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"requests", "assignedRequests", "password", "role"})
    private User user;


    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})

    @JoinTable(
            name = "request_questions",
            joinColumns = @JoinColumn(name = "request_idr"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @OrderBy("id ASC")
    private List<Question> questions = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "agent_id")
    @JsonIgnoreProperties({"requests", "assignedRequests", "password", "role"})
    private User agent;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Logs> logs = new ArrayList<>();
    @OneToOne
    private Report report;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Submission> submissionList = new ArrayList<>();
}




