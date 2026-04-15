package com.example.callcenter.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LastCallUpdateRequest {
    private LocalDateTime timestamp;
} 