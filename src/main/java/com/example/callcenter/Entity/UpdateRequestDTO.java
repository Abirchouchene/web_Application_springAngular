package com.example.callcenter.Entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
@Data

public class UpdateRequestDTO {
    private String description;
    private Priority priority;
    private CatgoryRequest catgoryRequest;
    private LocalDate deadline;
    private List<Long> contactIds;
    private List<Long> questionIds;
}
