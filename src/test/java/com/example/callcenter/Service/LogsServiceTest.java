package com.example.callcenter.Service;

import com.example.callcenter.DTO.LogDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.LogsRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogsServiceTest {

    @Mock private LogsRepository logsRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private UserRepository userRepository;

    private LogsService service;

    private Request request;
    private User user;

    @BeforeEach
    void setUp() {
        service = new LogsService(logsRepository, requestRepository, userRepository);

        request = new Request();
        request.setIdR(1L);
        request.setTitle("Demande test");
        request.setDescription("Description");
        request.setStatus(Status.IN_PROGRESS);
        request.setPriority(Priority.MEDIUM);

        user = new User();
        user.setIdUser(42L);
        user.setFullName("Agent Dupont");
    }

    // ── createLogFromDTO ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createLogFromDTO — sans requestId → sauvegarde sans FK")
    void createLogFromDTO_noRequestId_savesWithoutRequest() {
        LogDTO dto = buildBaseDTO(null, LogAction.REQUEST_CREATED, "Créé");
        Logs saved = new Logs();
        saved.setId(1L);
        when(logsRepository.save(any(Logs.class))).thenReturn(saved);

        Logs result = service.createLogFromDTO(dto);

        assertThat(result.getId()).isEqualTo(1L);
        verify(requestRepository, never()).findById(any());
        verify(logsRepository).save(any(Logs.class));
    }

    @Test
    @DisplayName("createLogFromDTO — avec requestId → charge la requête et sauvegarde")
    void createLogFromDTO_withRequestId_loadsAndSaves() {
        LogDTO dto = buildBaseDTO(1L, LogAction.STATUS_CHANGED, "Statut changé");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        Logs saved = new Logs();
        saved.setId(5L);
        when(logsRepository.save(any(Logs.class))).thenReturn(saved);

        Logs result = service.createLogFromDTO(dto);

        assertThat(result.getId()).isEqualTo(5L);
        verify(requestRepository).findById(1L);
        verify(logsRepository).save(any(Logs.class));
    }

    @Test
    @DisplayName("createLogFromDTO — requestId introuvable → RuntimeException")
    void createLogFromDTO_requestNotFound_throws() {
        LogDTO dto = buildBaseDTO(99L, LogAction.REQUEST_CREATED, "X");
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLogFromDTO(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ── logRequestCreated ────────────────────────────────────────────────────

    @Test
    @DisplayName("logRequestCreated — sauvegarde avec action REQUEST_CREATED")
    void logRequestCreated_savesCorrectAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        Logs result = service.logRequestCreated(request, user, "127.0.0.1", "Mozilla");

        assertThat(result.getLogAction()).isEqualTo(LogAction.REQUEST_CREATED);
        assertThat(result.getPerformedByUserId()).isEqualTo(42L);
    }

    // ── logStatusChange ──────────────────────────────────────────────────────

    @Test
    @DisplayName("logStatusChange — sauvegarde avec action STATUS_CHANGED et anciens/nouveaux statuts")
    void logStatusChange_savesCorrectFields() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(2L);
            return l;
        });

        Logs result = service.logStatusChange(request, user, Status.IN_PROGRESS, Status.CLOSED,
                "192.168.1.1", "Chrome", "Clôturé");

        assertThat(result.getLogAction()).isEqualTo(LogAction.STATUS_CHANGED);
        assertThat(result.getOldStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(result.getNewStatus()).isEqualTo(Status.CLOSED);
    }

    // ── logPriorityChange ────────────────────────────────────────────────────

    @Test
    @DisplayName("logPriorityChange — sauvegarde avec action PRIORITY_CHANGED")
    void logPriorityChange_savesCorrectAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(3L);
            return l;
        });

        Logs result = service.logPriorityChange(request, user, Priority.LOW, Priority.HIGH,
                "127.0.0.1", "Firefox", "Escalade");

        assertThat(result.getLogAction()).isEqualTo(LogAction.PRIORITY_CHANGED);
        assertThat(result.getOldPriority()).isEqualTo(Priority.LOW);
        assertThat(result.getNewPriority()).isEqualTo(Priority.HIGH);
    }

    // ── logAgentAssignment ───────────────────────────────────────────────────

    @Test
    @DisplayName("logAgentAssignment — avec ancien/nouveau agent → sauvegarde AGENT_ASSIGNED")
    void logAgentAssignment_withAgents_savesCorrectly() {
        User oldAgent = buildUser(10L, "Ancien Agent");
        User newAgent = buildUser(11L, "Nouvel Agent");
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(4L);
            return l;
        });

        Logs result = service.logAgentAssignment(request, user, oldAgent, newAgent,
                "127.0.0.1", "Safari", "Réassignation");

        assertThat(result.getLogAction()).isEqualTo(LogAction.AGENT_ASSIGNED);
        assertThat(result.getOldAssignedAgent()).isEqualTo("Ancien Agent");
        assertThat(result.getNewAssignedAgent()).isEqualTo("Nouvel Agent");
    }

    @Test
    @DisplayName("logAgentAssignment — ancien/nouveau agent null → valeur 'Aucun'")
    void logAgentAssignment_nullAgents_usesAucun() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(5L);
            return l;
        });

        Logs result = service.logAgentAssignment(request, user, null, null,
                "127.0.0.1", "Edge", "Initial");

        assertThat(result.getOldAssignedAgent()).isEqualTo("Aucun");
        assertThat(result.getNewAssignedAgent()).isEqualTo("Aucun");
    }

    // ── logRequestApproval ───────────────────────────────────────────────────

    @Test
    @DisplayName("logRequestApproval — approuvé → action REQUEST_APPROVED")
    void logRequestApproval_approved_correctAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(6L);
            return l;
        });

        Logs result = service.logRequestApproval(request, user, true, "OK",
                "127.0.0.1", "Chrome");

        assertThat(result.getLogAction()).isEqualTo(LogAction.REQUEST_APPROVED);
    }

    @Test
    @DisplayName("logRequestApproval — rejeté → action REQUEST_REJECTED")
    void logRequestApproval_rejected_correctAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(7L);
            return l;
        });

        Logs result = service.logRequestApproval(request, user, false, "Données manquantes",
                "127.0.0.1", "Firefox");

        assertThat(result.getLogAction()).isEqualTo(LogAction.REQUEST_REJECTED);
    }

    // ── logRequestUpdate / logRequestDeleted ─────────────────────────────────

    @Test
    @DisplayName("logRequestUpdate — sauvegarde avec action REQUEST_UPDATED")
    void logRequestUpdate_savesCorrectAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(8L);
            return l;
        });

        Logs result = service.logRequestUpdate(request, user, "Titre modifié", "127.0.0.1", "IE");

        assertThat(result.getLogAction()).isEqualTo(LogAction.REQUEST_UPDATED);
    }

    @Test
    @DisplayName("logRequestDeleted — sauvegarde avec action REQUEST_DELETED")
    void logRequestDeleted_savesCorrectAction() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(9L);
            return l;
        });

        Logs result = service.logRequestDeleted(request, user, "Doublon", "127.0.0.1", "Chrome");

        assertThat(result.getLogAction()).isEqualTo(LogAction.REQUEST_DELETED);
    }

    // ── createLog ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createLog — définit le timestamp et sauvegarde")
    void createLog_setsTimestampAndSaves() {
        Logs log = new Logs();
        when(logsRepository.save(any(Logs.class))).thenAnswer(inv -> {
            Logs l = inv.getArgument(0);
            l.setId(99L);
            return l;
        });

        Logs result = service.createLog(log);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(log.getTimestamp()).isNotNull();
        verify(logsRepository).save(log);
    }

    // ── query methods ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLogsByRequestId — appelle le repository")
    void getLogsByRequestId_callsRepo() {
        when(logsRepository.findByRequest_IdROrderByTimestampDesc(1L)).thenReturn(Collections.emptyList());

        List<Logs> result = service.getLogsByRequestId(1L);

        assertThat(result).isEmpty();
        verify(logsRepository).findByRequest_IdROrderByTimestampDesc(1L);
    }

    @Test
    @DisplayName("getRecentLogs — appelle findRecentLogs avec date 24h avant")
    void getRecentLogs_callsRepo() {
        when(logsRepository.findRecentLogs(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        List<Logs> result = service.getRecentLogs();

        assertThat(result).isEmpty();
        verify(logsRepository).findRecentLogs(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getStatusChangeLogs — appelle findStatusChangeLogs")
    void getStatusChangeLogs_callsRepo() {
        when(logsRepository.findStatusChangeLogs()).thenReturn(Collections.emptyList());

        List<Logs> result = service.getStatusChangeLogs();

        assertThat(result).isEmpty();
        verify(logsRepository).findStatusChangeLogs();
    }

    @Test
    @DisplayName("getLogsByAction — appelle findByLogActionOrderByTimestampDesc")
    void getLogsByAction_callsRepo() {
        when(logsRepository.findByLogActionOrderByTimestampDesc(LogAction.STATUS_CHANGED))
                .thenReturn(Collections.emptyList());

        List<Logs> result = service.getLogsByAction(LogAction.STATUS_CHANGED);

        assertThat(result).isEmpty();
        verify(logsRepository).findByLogActionOrderByTimestampDesc(LogAction.STATUS_CHANGED);
    }

    // ── getLogStatistics ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getLogStatistics — retourne une map avec les 4 clés attendues")
    void getLogStatistics_returnsFourKeys() {
        when(logsRepository.count()).thenReturn(100L);
        when(logsRepository.countByTimestampAfter(any(LocalDateTime.class))).thenReturn(10L);
        when(logsRepository.findStatusChangeLogs()).thenReturn(Arrays.asList(new Logs(), new Logs()));
        when(logsRepository.findAgentAssignmentLogs()).thenReturn(Collections.singletonList(new Logs()));

        Map<String, Object> stats = service.getLogStatistics();

        assertThat(stats).containsKeys("totalLogs", "todayLogs", "statusChanges", "agentAssignments");
        assertThat(stats.get("totalLogs")).isEqualTo(100L);
        assertThat(stats.get("statusChanges")).isEqualTo(2);
        assertThat(stats.get("agentAssignments")).isEqualTo(1);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LogDTO buildBaseDTO(Long requestId, LogAction action, String description) {
        LogDTO dto = new LogDTO();
        dto.setRequestId(requestId);
        dto.setLogAction(action);
        dto.setActionDescription(description);
        dto.setTimestamp(LocalDateTime.now());
        dto.setUserId(42L);
        dto.setUserFullName("Agent Dupont");
        return dto;
    }

    private User buildUser(Long id, String fullName) {
        User u = new User();
        u.setIdUser(id);
        u.setFullName(fullName);
        return u;
    }
}
