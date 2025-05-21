package com.example.callcenter.DTO;


import com.example.callcenter.Entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDTO {
    private String message;
    private NotificationType type;
    private Long agentId;
}