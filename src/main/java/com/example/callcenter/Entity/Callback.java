package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.aspectj.weaver.loadtime.Agent;

import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Callback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime scheduledDate;

    private String notes;

    @Enumerated(EnumType.STRING)
    private CallbackStatus status;

    @ManyToOne
    @JsonBackReference
    private Contact contact;

    @ManyToOne
    @JsonIgnore
    private Request request;

    @ManyToOne
    @JsonBackReference

    private User agent;}
