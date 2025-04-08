package com.example.callcenter.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Question implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String question;

    @Enumerated(EnumType.STRING)

    private QuestionType questionType;
    @ManyToMany
    private Set<Request>requests;
    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    public Question(String s, QuestionType questionType) {
    }
}
