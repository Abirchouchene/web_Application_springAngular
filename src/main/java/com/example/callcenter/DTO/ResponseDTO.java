package com.example.callcenter.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    /**
     * Returns a unified list of response values regardless of question type.
     */
    public List<String> getResponseValues() {
        List<String> values = new ArrayList<>();
        if (answer != null) {
            values.add(answer);
        } else if (booleanAnswer != null) {
            values.add(booleanAnswer.toString());
        } else if (numberAnswer != null) {
            values.add(numberAnswer.toString());
        } else if (dateAnswer != null) {
            values.add(dateAnswer.toString());
        } else if (timeAnswer != null) {
            values.add(timeAnswer.toString());
        } else if (multiAnswer != null && !multiAnswer.isEmpty()) {
            values.addAll(multiAnswer);
        }
        return values;
    }
}
