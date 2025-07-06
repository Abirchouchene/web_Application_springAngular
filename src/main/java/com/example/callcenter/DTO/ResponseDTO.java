package com.example.callcenter.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO {
    private Long id;
    private String answer;
    private List<String> multiAnswer;
    private Boolean booleanAnswer;
    private Double numberAnswer;
    private LocalDate dateAnswer;
    private LocalTime timeAnswer;
    private Long contactId;
    private String contactName;
    private ContactDTO contact;
    private Long questionId;
    private String questionText;
    private String questionType;
}
