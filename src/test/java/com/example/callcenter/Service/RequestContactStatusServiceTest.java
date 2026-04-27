package com.example.callcenter.Service;

import com.example.callcenter.DTO.RequestContactStatusDTO;
import com.example.callcenter.Entity.ContactStatus;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestContactStatus;
import com.example.callcenter.Repository.RequestContactStatusRepository;
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
class RequestContactStatusServiceTest {

    @Mock private RequestContactStatusRepository repository;

    private RequestContactStatusService service;

    @BeforeEach
    void setUp() {
        service = new RequestContactStatusService(repository);
    }

    // ── getContactStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getContactStatus — trouvé → retourne DTO")
    void getContactStatus_found_returnsDTO() {
        RequestContactStatus entity = buildEntity(1L, 10L, 20L, ContactStatus.CONTACTED_AVAILABLE, "Note");
        when(repository.findByRequestIdRAndContactId(10L, 20L)).thenReturn(Optional.of(entity));

        RequestContactStatusDTO dto = service.getContactStatus(10L, 20L);

        assertThat(dto).isNotNull();
        assertThat(dto.getRequestId()).isEqualTo(10L);
        assertThat(dto.getContactId()).isEqualTo(20L);
        assertThat(dto.getStatus()).isEqualTo(ContactStatus.CONTACTED_AVAILABLE);
    }

    @Test
    @DisplayName("getContactStatus — introuvable → retourne null")
    void getContactStatus_notFound_returnsNull() {
        when(repository.findByRequestIdRAndContactId(10L, 20L)).thenReturn(Optional.empty());

        RequestContactStatusDTO dto = service.getContactStatus(10L, 20L);

        assertThat(dto).isNull();
    }

    // ── getContactStatusesByRequest ──────────────────────────────────────────

    @Test
    @DisplayName("getContactStatusesByRequest — retourne la liste")
    void getContactStatusesByRequest_returnsList() {
        List<RequestContactStatus> entities = Arrays.asList(
                buildEntity(1L, 10L, 20L, ContactStatus.CONTACTED_AVAILABLE, ""),
                buildEntity(2L, 10L, 21L, ContactStatus.NOT_CONTACTED, "")
        );
        when(repository.findByRequestIdR(10L)).thenReturn(entities);

        List<RequestContactStatusDTO> result = service.getContactStatusesByRequest(10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContactId()).isEqualTo(20L);
        assertThat(result.get(1).getStatus()).isEqualTo(ContactStatus.NOT_CONTACTED);
    }

    @Test
    @DisplayName("getContactStatusesByRequest — liste vide")
    void getContactStatusesByRequest_empty() {
        when(repository.findByRequestIdR(10L)).thenReturn(Collections.emptyList());

        List<RequestContactStatusDTO> result = service.getContactStatusesByRequest(10L);

        assertThat(result).isEmpty();
    }

    // ── getContactStatusesByContact ──────────────────────────────────────────

    @Test
    @DisplayName("getContactStatusesByContact — retourne la liste")
    void getContactStatusesByContact_returnsList() {
        List<RequestContactStatus> entities = Arrays.asList(
                buildEntity(1L, 10L, 99L, ContactStatus.CONTACTED_AVAILABLE, ""),
                buildEntity(2L, 11L, 99L, ContactStatus.NOT_CONTACTED, "")
        );
        when(repository.findByContactId(99L)).thenReturn(entities);

        List<RequestContactStatusDTO> result = service.getContactStatusesByContact(99L);

        assertThat(result).hasSize(2);
    }

    // ── updateContactStatus ──────────────────────────────────────────────────

    @Test
    @DisplayName("updateContactStatus — enregistrement existant → mise à jour")
    void updateContactStatus_existing_updates() {
        RequestContactStatus existing = buildEntity(1L, 10L, 20L, ContactStatus.NOT_CONTACTED, "");
        when(repository.findByRequestIdRAndContactId(10L, 20L)).thenReturn(Optional.of(existing));
        when(repository.save(any(RequestContactStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        RequestContactStatusDTO dto = service.updateContactStatus(10L, 20L, ContactStatus.CONTACTED_AVAILABLE, "Appel réussi");

        assertThat(dto.getStatus()).isEqualTo(ContactStatus.CONTACTED_AVAILABLE);
        assertThat(dto.getCallNote()).isEqualTo("Appel réussi");
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("updateContactStatus — introuvable → crée un nouveau")
    void updateContactStatus_notFound_createsNew() {
        when(repository.findByRequestIdRAndContactId(10L, 20L)).thenReturn(Optional.empty());
        when(repository.save(any(RequestContactStatus.class))).thenAnswer(inv -> {
            RequestContactStatus s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        RequestContactStatusDTO dto = service.updateContactStatus(10L, 20L, ContactStatus.CALL_BACK_LATER, "Boîte vocale");

        assertThat(dto).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(ContactStatus.CALL_BACK_LATER);
        verify(repository).save(any(RequestContactStatus.class));
    }

    // ── updateLastCallAttempt ────────────────────────────────────────────────

    @Test
    @DisplayName("updateLastCallAttempt — appelle le repository")
    void updateLastCallAttempt_callsRepo() {
        service.updateLastCallAttempt(10L, 20L);

        verify(repository).updateLastCallAttempt(eq(10L), eq(20L), any(LocalDateTime.class));
    }

    // ── initializeContactStatuses ────────────────────────────────────────────

    @Test
    @DisplayName("initializeContactStatuses — crée les statuts manquants, ignore les existants")
    void initializeContactStatuses_createsNewSkipsExisting() {
        when(repository.findByRequestIdRAndContactId(10L, 1L)).thenReturn(Optional.empty());
        when(repository.findByRequestIdRAndContactId(10L, 2L)).thenReturn(
                Optional.of(buildEntity(5L, 10L, 2L, ContactStatus.NOT_CONTACTED, "")));

        service.initializeContactStatuses(10L, Arrays.asList(1L, 2L));

        verify(repository, times(1)).save(any(RequestContactStatus.class)); // seulement contactId=1
        verify(repository, never()).save(argThat(s -> s.getContactId() != null && s.getContactId().equals(2L)));
    }

    // ── getContactStatusMap ──────────────────────────────────────────────────

    @Test
    @DisplayName("getContactStatusMap — retourne la map contactId → statut")
    void getContactStatusMap_returnsMap() {
        List<RequestContactStatus> entities = Arrays.asList(
                buildEntity(1L, 10L, 20L, ContactStatus.CONTACTED_AVAILABLE, ""),
                buildEntity(2L, 10L, 21L, ContactStatus.NOT_CONTACTED, "")
        );
        when(repository.findByRequestIdR(10L)).thenReturn(entities);

        Map<Long, ContactStatus> map = service.getContactStatusMap(10L);

        assertThat(map).hasSize(2);
        assertThat(map.get(20L)).isEqualTo(ContactStatus.CONTACTED_AVAILABLE);
        assertThat(map.get(21L)).isEqualTo(ContactStatus.NOT_CONTACTED);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteByRequestIdR — appelle le repository")
    void deleteByRequestIdR_callsRepo() {
        service.deleteByRequestIdR(10L);

        verify(repository).deleteByRequestIdR(10L);
    }

    @Test
    @DisplayName("deleteByContactId — appelle le repository")
    void deleteByContactId_callsRepo() {
        service.deleteByContactId(99L);

        verify(repository).deleteByContactId(99L);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private RequestContactStatus buildEntity(Long id, Long requestIdR, Long contactId,
                                              ContactStatus status, String callNote) {
        Request request = new Request();
        request.setIdR(requestIdR);
        RequestContactStatus e = new RequestContactStatus();
        e.setId(id);
        e.setRequest(request);
        e.setContactId(contactId);
        e.setStatus(status);
        e.setCallNote(callNote);
        e.setLastCallAttempt(LocalDateTime.now());
        return e;
    }
}
