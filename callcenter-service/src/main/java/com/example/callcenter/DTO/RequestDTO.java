package com.example.callcenter.DTO;

import com.example.callcenter.Entity.CategoryRequest;
import com.example.callcenter.Entity.Priority;
import com.example.callcenter.Entity.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDTO {
    private Long userId;
    private String title;
    private RequestType requestType;
    private List<Long> contactIds;
    private String description;
    private CategoryRequest category;
    private List<Long> questionIds; // Optional: Existing questions
    private List<QuestionDTO> newQuestions; // New questions
    private Priority priorityLevel;
    private LocalDate deadline;
}
