/*package com.example.callcenter.Service;

import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @InjectMocks
    private RequestService requestService;

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AgentLeaveRepository agentLeaveRepository;

    private User user;
    private Contact contact;
    private Question question;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setIdUser(1L);

        contact = new Contact();
        contact.setIdC(10L);

        question = new Question();
        question.setId(100L);
        question.setText("Is the product working?");
        question.setQuestionType(QuestionType.YES_OR_NO);
    }

    @Test
    void testSubmitRequest_success() {
        RequestDTO dto = new RequestDTO();
        dto.setUserId(1L);
        dto.setRequestType(RequestType.STATISTICS);
        dto.setDescription("Test request");
        dto.setPriorityLevel(Priority.URGENT);
        dto.setCategory(CategoryRequest.PRODUIT);
        dto.setDeadline(LocalDate.now().plusDays(5));
        dto.setContactIds(List.of(10L));
        dto.setQuestionIds(List.of(100L));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(contactRepository.findAllById(List.of(10L))).thenReturn(List.of(contact));
        when(questionRepository.findAllById(List.of(100L))).thenReturn(List.of(question));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        Request result = requestService.submitRequest(dto);

        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        //assertThat(result.getContacts()).contains(contact);
        assertThat(result.getQuestions()).contains(question);
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
    }
}
*/