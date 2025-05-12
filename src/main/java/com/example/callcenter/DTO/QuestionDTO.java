package com.example.callcenter.DTO;

import com.example.callcenter.Entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private String text;
    private QuestionType type;
}