package com.example.callcenter.Service;

import com.example.callcenter.Entity.Notification;
import com.example.callcenter.Repository.NotificationRepository;
import com.example.callcenter.Entity.NotificationType;
import com.example.callcenter.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public Notification createNotification(Long agentId, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setType(type);
        notification.setTimestamp(LocalDateTime.now());
        notification.setRead(false);
        notification.setAgent(userRepository.findById(agentId).orElseThrow());

        notification = notificationRepository.save(notification);

        // Send via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + agentId,
                notification
        );
        return notification;
    }

    public List<Notification> getAgentNotifications(Long agentId) {
        return notificationRepository.findByAgent_IdUserOrderByTimestampDesc(agentId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    public void markAllAsRead(Long agentId) {
        notificationRepository.findByAgent_IdUserOrderByTimestampDesc(agentId)
                .forEach(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                });
    }

    public void clearAllNotifications(Long agentId) {
        notificationRepository.deleteByAgent_IdUser(agentId);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}