package com.example.callcenter.DTO;

import com.example.callcenter.Entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String text;
    private QuestionType type;
    private List<String> options;
}