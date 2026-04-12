package com.example.callcenter.Controller;

import com.example.callcenter.DTO.NotificationRequestDTO;
import com.example.callcenter.Entity.Notification;
import com.example.callcenter.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
//@CrossOrigin("http://localhost:4200")
@RequiredArgsConstructor

public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/create")
    public Notification createNotification(@RequestBody NotificationRequestDTO request) {
        return notificationService.createNotification(
                request.getAgentId(),
                request.getMessage(),
                request.getType()
        );
    }
    @GetMapping("/agent/{agentId}")
    public List<Notification> getAgentNotifications(@PathVariable Long agentId) {
        return notificationService.getAgentNotifications(agentId);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    @PutMapping("/agent/{agentId}/read-all")
    public void markAllAsRead(@PathVariable Long agentId) {
        notificationService.markAllAsRead(agentId);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
    }

    @DeleteMapping("/agent/{agentId}")
    public void clearAllNotifications(@PathVariable Long agentId) {
        notificationService.clearAllNotifications(agentId);
    }
    
    // Test endpoint to manually create a notification
    @PostMapping("/test/{agentId}")
    public void createTestNotification(@PathVariable Long agentId) {
        notificationService.createTestNotification(agentId);
    }
}