package com.example.callcenter.Service;

import com.example.callcenter.Entity.Notification;
import com.example.callcenter.Entity.NotificationType;
import com.example.callcenter.Entity.User;
import com.example.callcenter.Repository.NotificationRepository;
import com.example.callcenter.Repository.UserRepository;
import com.example.callcenter.client.ContactClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserRepository userRepository;
    @Mock private ContactClient contactClient;

    private NotificationService service;

    private User agent;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, messagingTemplate, userRepository, contactClient);

        agent = new User();
        agent.setIdUser(1L);
        agent.setFullName("Agent Test");
    }

    // ── createNotification ───────────────────────────────────────────────────

    @Test
    @DisplayName("createNotification — agent trouvé → sauvegarde et envoie WebSocket")
    void createNotification_agentFound_savesAndSendsWS() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        Notification saved = buildNotification(10L, agent, "Test message", NotificationType.REMINDER);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = service.createNotification(1L, "Test message", NotificationType.REMINDER);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/1"), eq(saved));
    }

    @Test
    @DisplayName("createNotification — agent introuvable → RuntimeException")
    void createNotification_agentNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createNotification(99L, "msg", NotificationType.REMINDER))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(notificationRepository, never()).save(any());
    }

    // ── getAgentNotifications ────────────────────────────────────────────────

    @Test
    @DisplayName("getAgentNotifications — retourne la liste triée")
    void getAgentNotifications_returnsList() {
        List<Notification> list = Arrays.asList(
                buildNotification(1L, agent, "A", NotificationType.REMINDER),
                buildNotification(2L, agent, "B", NotificationType.REMINDER)
        );
        when(notificationRepository.findByAgent_IdUserOrderByTimestampDesc(1L)).thenReturn(list);

        List<Notification> result = service.getAgentNotifications(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getAgentNotifications — retourne liste vide")
    void getAgentNotifications_empty() {
        when(notificationRepository.findByAgent_IdUserOrderByTimestampDesc(1L)).thenReturn(Collections.emptyList());

        List<Notification> result = service.getAgentNotifications(1L);

        assertThat(result).isEmpty();
    }

    // ── markAsRead ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead — trouvée → passe isRead à true")
    void markAsRead_found_setsReadTrue() {
        Notification notif = buildNotification(5L, agent, "msg", NotificationType.REMINDER);
        notif.setRead(false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAsRead(5L);

        assertThat(notif.isRead()).isTrue();
        verify(notificationRepository).save(notif);
    }

    @Test
    @DisplayName("markAsRead — introuvable → ne fait rien")
    void markAsRead_notFound_doesNothing() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        service.markAsRead(99L);

        verify(notificationRepository, never()).save(any());
    }

    // ── markAllAsRead ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAllAsRead — marque toutes les notifications comme lues")
    void markAllAsRead_marksAll() {
        Notification n1 = buildNotification(1L, agent, "A", NotificationType.REMINDER);
        Notification n2 = buildNotification(2L, agent, "B", NotificationType.REMINDER);
        n1.setRead(false);
        n2.setRead(false);
        when(notificationRepository.findByAgent_IdUserOrderByTimestampDesc(1L)).thenReturn(Arrays.asList(n1, n2));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markAllAsRead(1L);

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    // ── clearAllNotifications ────────────────────────────────────────────────

    @Test
    @DisplayName("clearAllNotifications — appelle deleteByAgent_IdUser")
    void clearAllNotifications_callsDelete() {
        service.clearAllNotifications(1L);

        verify(notificationRepository).deleteByAgent_IdUser(1L);
    }

    // ── deleteNotification ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteNotification — appelle deleteById")
    void deleteNotification_callsDeleteById() {
        service.deleteNotification(7L);

        verify(notificationRepository).deleteById(7L);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Notification buildNotification(Long id, User agent, String message, NotificationType type) {
        Notification n = new Notification();
        n.setId(id);
        n.setAgent(agent);
        n.setMessage(message);
        n.setType(type);
        n.setTimestamp(LocalDateTime.now());
        n.setRead(false);
        return n;
    }
}
