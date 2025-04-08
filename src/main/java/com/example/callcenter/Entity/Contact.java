package com.example.callcenter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
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
    private String tag; // Example: "RESELLER", "USER", "PRODUCT"

    private String reference; // For reclamations (Product Reference from SAV)

    @ManyToMany
    private Set<Request>requests;
    @ElementCollection
    private Set<String> tags = new HashSet<>();
}
