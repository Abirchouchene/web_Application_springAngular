package com.example.callcenter.Service;

import com.example.callcenter.Entity.Notification;
import com.example.callcenter.Repository.NotificationRepository;
import com.example.callcenter.Entity.NotificationType;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.client.ContactClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ContactClient contactClient;

    // Track callback IDs for which reminders/due notifications have already been sent
    private final Set<Long> sentReminderCallbackIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> sentDueCallbackIds = ConcurrentHashMap.newKeySet();

    public Notification createNotification(Long agentId, String message, NotificationType type) {
        log.info("Creating notification for agent {}: {}", agentId, message);
        
        try {
            Notification notification = new Notification();
            notification.setMessage(message);
            notification.setType(type);
            notification.setTimestamp(LocalDateTime.now());
            notification.setRead(false);
            
            var agent = userRepository.findById(agentId);
            if (agent.isEmpty()) {
                log.error("Agent not found with ID: {}", agentId);
                throw new RuntimeException("Agent not found with ID: " + agentId);
            }
            notification.setAgent(agent.get());

            notification = notificationRepository.save(notification);
            log.info("Notification saved with ID: {}", notification.getId());

            // Send via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + agentId,
                    notification
            );
            log.info("WebSocket notification sent to agent {}", agentId);
            
            return notification;
        } catch (Exception e) {
            log.error("Error creating notification for agent {}: {}", agentId, e.getMessage(), e);
            throw e;
        }
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
    
    // ===== CALLBACK SCHEDULING METHODS =====
    
    // Check every minute for upcoming callbacks
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void checkUpcomingCallbacks() {
        log.info("=== SCHEDULED METHOD RUNNING: checkUpcomingCallbacks ===");
        
        try {
            log.info("Calling contact-service for upcoming callbacks...");
            List<Map<String, Object>> upcomingCallbacks = contactClient.getUpcomingCallbacks();
            log.info("Received {} upcoming callbacks", upcomingCallbacks.size());
            
            if (upcomingCallbacks.isEmpty()) {
                log.info("No upcoming callbacks found");
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime in15Minutes = now.plus(15, ChronoUnit.MINUTES);
            log.info("Checking callbacks between {} and {}", now, in15Minutes);
            
            for (Map<String, Object> callbackObj : upcomingCallbacks) {
                log.info("Checking callback: {}", callbackObj);
                if (shouldSendReminder(callbackObj, now, in15Minutes)) {
                    log.info("Sending reminder for callback: {}", callbackObj);
                    sendCallbackReminder(callbackObj);
                }
            }
        } catch (Exception e) {
            log.error("Error checking upcoming callbacks", e);
        }
    }
    
    // Check every 5 minutes for callbacks that are due now
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkDueCallbacks() {
        log.info("Checking for due callbacks...");
        
        try {
            List<Map<String, Object>> upcomingCallbacks = contactClient.getUpcomingCallbacks();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime in5Minutes = now.plus(5, ChronoUnit.MINUTES);
            
            for (Map<String, Object> callbackObj : upcomingCallbacks) {
                if (isCallbackDue(callbackObj, now, in5Minutes)) {
                    sendCallbackDueNotification(callbackObj);
                }
            }
        } catch (Exception e) {
            log.error("Error checking due callbacks", e);
        }
    }
    
    private boolean shouldSendReminder(Map<String, Object> callbackObj, LocalDateTime now, LocalDateTime in15Minutes) {
        try {
            Long callbackId = extractCallbackId(callbackObj);
            if (callbackId != null && sentReminderCallbackIds.contains(callbackId)) {
                return false;
            }
            String scheduledDateStr = (String) callbackObj.get("scheduledDate");
            if (scheduledDateStr != null) {
                LocalDateTime scheduledDate = LocalDateTime.parse(scheduledDateStr);
                return scheduledDate.isAfter(now) && scheduledDate.isBefore(in15Minutes);
            }
        } catch (Exception e) {
            log.error("Error parsing callback date", e);
        }
        return false;
    }
    
    private boolean isCallbackDue(Map<String, Object> callbackObj, LocalDateTime now, LocalDateTime in5Minutes) {
        try {
            Long callbackId = extractCallbackId(callbackObj);
            if (callbackId != null && sentDueCallbackIds.contains(callbackId)) {
                return false;
            }
            String scheduledDateStr = (String) callbackObj.get("scheduledDate");
            if (scheduledDateStr != null) {
                LocalDateTime scheduledDate = LocalDateTime.parse(scheduledDateStr);
                return scheduledDate.isAfter(now.minus(5, ChronoUnit.MINUTES)) && 
                       scheduledDate.isBefore(in5Minutes);
            }
        } catch (Exception e) {
            log.error("Error parsing callback date", e);
        }
        return false;
    }
    
    private void sendCallbackReminder(Map<String, Object> callbackObj) {
        try {
            Long callbackId = extractCallbackId(callbackObj);
            Long agentId = extractAgentId(callbackObj);
            Long contactId = extractContactId(callbackObj);
            Long requestId = extractRequestId(callbackObj);
            
            String message = String.format(
                "Rappel prévu dans 15 minutes: Contact %d, Demande #%d",
                contactId, requestId
            );
            
            createNotification(agentId, message, NotificationType.REMINDER);
            if (callbackId != null) sentReminderCallbackIds.add(callbackId);
            log.info("Callback reminder sent for callback ID: {}", callbackId);
            
        } catch (Exception e) {
            log.error("Error sending callback reminder", e);
        }
    }
    
    private void sendCallbackDueNotification(Map<String, Object> callbackObj) {
        try {
            Long callbackId = extractCallbackId(callbackObj);
            Long agentId = extractAgentId(callbackObj);
            Long contactId = extractContactId(callbackObj);
            Long requestId = extractRequestId(callbackObj);
            
            String message = String.format(
                "Rappel dû maintenant: Contact %d, Demande #%d",
                contactId, requestId
            );
            
            createNotification(agentId, message, NotificationType.REMINDER);
            if (callbackId != null) sentDueCallbackIds.add(callbackId);
            log.info("Callback due notification sent for callback ID: {}", callbackId);
            
        } catch (Exception e) {
            log.error("Error sending callback due notification", e);
        }
    }
    
    private Long extractCallbackId(Map<String, Object> callbackObj) {
        Object id = callbackObj.get("id");
        return id != null ? Long.valueOf(id.toString()) : null;
    }
    
    private Long extractAgentId(Map<String, Object> callbackObj) {
        return Long.valueOf(callbackObj.get("agentId").toString());
    }
    
    private Long extractContactId(Map<String, Object> callbackObj) {
        return Long.valueOf(callbackObj.get("contactId").toString());
    }
    
    private Long extractRequestId(Map<String, Object> callbackObj) {
        return Long.valueOf(callbackObj.get("requestId").toString());
    }
    
    // Test method to manually create a notification
    public void createTestNotification(Long agentId) {
        log.info("Creating test notification for agent: {}", agentId);
        createNotification(agentId, "Test notification - " + LocalDateTime.now(), NotificationType.REMINDER);
    }
}