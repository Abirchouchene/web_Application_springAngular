package com.example.callcenter.DTO;

import com.example.callcenter.Entity.CategoryRequest;
import com.example.callcenter.Entity.Priority;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
@Data

public class UpdateRequestDTO {
    private String description;
    private Priority priority;
    private CategoryRequest categoryRequest;
    private LocalDate deadline;
    private List<Long> contactIds;
    private List<Long> questionIds;
}
