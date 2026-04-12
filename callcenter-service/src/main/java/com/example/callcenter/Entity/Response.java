package com.example.callcenter.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Response implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String answer; // For SHORT_ANSWER, PARAGRAPH, MULTIPLE_CHOICE, DROPDOWN

    @ElementCollection
    private List<String> multiAnswer; // For CHECKBOXES

    private Boolean booleanAnswer; // For YES_OR_NO

    private Double numberAnswer; // For NUMBER

    private LocalDate dateAnswer; // For DATE

    private LocalTime timeAnswer; // For TIME



    @ManyToOne
    @JsonBackReference
    private  Submission submission;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

}
