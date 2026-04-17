package com.example.contactservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c")
    private Long idC;
    private String name;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private ContactStatus callStatus = ContactStatus.NOT_CONTACTED;

    private String callNote;
    private LocalDateTime lastCallAttempt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tag_contacts",
            joinColumns = @JoinColumn(name = "contacts_id_c"),
            inverseJoinColumns = @JoinColumn(name = "tags_id")
    )
    private Set<Tag> tags = new HashSet<>();
    
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Callback> callbacks = new ArrayList<>();
} 