package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallCopilotServiceTest {

    @Mock private DataAnonymizationService anonymizationService;
    @Mock private RequestRepository requestRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ResponseRepository responseRepository;
    @Mock private QuestionRepository questionRepository;

    private CallCopilotService service;

    private Request request;
    private Question question;

    @BeforeEach
    void setUp() {
        service = new CallCopilotService(
                "",
                "http://localhost",
                anonymizationService,
                requestRepository,
                submissionRepository,
                responseRepository,
                questionRepository,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "model", "gpt-4");
        ReflectionTestUtils.setField(service, "analysisPrompt", "");
        ReflectionTestUtils.setField(service, "summaryPrompt", "");

        request = new Request();
        request.setIdR(1L);
        request.setTitle("Enquête satisfaction");
        request.setQuestions(new ArrayList<>());

        question = new Question("Êtes-vous satisfait ?", QuestionType.YES_OR_NO);
        question.setId(10L);
    }

    // ── analyzeLiveResponse ──────────────────────────────────────────────────

    @Test
    @DisplayName("analyzeLiveResponse — request introuvable → RuntimeException")
    void analyzeLiveResponse_requestNotFound_throws() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyzeLiveResponse(99L, 1L, 10L, "Oui"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    @Test
    @DisplayName("analyzeLiveResponse — question introuvable → RuntimeException")
    void analyzeLiveResponse_questionNotFound_throws() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyzeLiveResponse(1L, 1L, 99L, "Oui"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Question not found");
    }

    @Test
    @DisplayName("analyzeLiveResponse — AI désactivée → retourne fallback avec les clés attendues")
    void analyzeLiveResponse_aiDisabled_returnsFallback() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(submissionRepository.findAllByRequestIdRAndContactId(1L, 5L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.analyzeLiveResponse(1L, 5L, 10L, "C'est correct");

        assertThat(result).containsKeys("suggestion", "detectedPoints", "sentiment", "confidence");
        assertThat(result.get("sentiment")).isEqualTo("NEUTRAL");
    }

    @Test
    @DisplayName("analyzeLiveResponse — réponse négative → sentiment NEGATIVE")
    void analyzeLiveResponse_negativeAnswer_negativeSentiment() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        Question textQ = new Question("Décrivez le problème", QuestionType.SHORT_ANSWER);
        textQ.setId(20L);
        when(questionRepository.findById(20L)).thenReturn(Optional.of(textQ));
        when(submissionRepository.findAllByRequestIdRAndContactId(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.analyzeLiveResponse(1L, 5L, 20L,
                "Il y a un problème critique avec votre service");

        assertThat(result.get("sentiment")).isEqualTo("NEGATIVE");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> points = (List<Map<String, String>>) result.get("detectedPoints");
        assertThat(points).isNotEmpty();
    }

    @Test
    @DisplayName("analyzeLiveResponse — réponse positive → sentiment POSITIVE")
    void analyzeLiveResponse_positiveAnswer_positiveSentiment() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        Question textQ = new Question("Votre avis ?", QuestionType.PARAGRAPH);
        textQ.setId(30L);
        when(questionRepository.findById(30L)).thenReturn(Optional.of(textQ));
        when(submissionRepository.findAllByRequestIdRAndContactId(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.analyzeLiveResponse(1L, 5L, 30L,
                "Tout est excellent et parfait, merci !");

        assertThat(result.get("sentiment")).isEqualTo("POSITIVE");
    }

    @Test
    @DisplayName("analyzeLiveResponse — réponse courte sur question texte → reformulation proposée")
    void analyzeLiveResponse_shortTextAnswer_proposesReformulation() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        Question textQ = new Question("Décrivez votre problème", QuestionType.SHORT_ANSWER);
        textQ.setId(40L);
        when(questionRepository.findById(40L)).thenReturn(Optional.of(textQ));
        when(submissionRepository.findAllByRequestIdRAndContactId(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.analyzeLiveResponse(1L, 5L, 40L, "OK");

        assertThat(result.get("reformulation")).isNotNull();
        assertThat((double) result.get("confidence")).isLessThan(0.7);
    }

    @Test
    @DisplayName("analyzeLiveResponse — questions restantes → suggestion de prochaine question")
    void analyzeLiveResponse_remainingQuestions_suggestsNext() {
        Question q2 = new Question("Recommanderiez-vous notre service ?", QuestionType.YES_OR_NO);
        q2.setId(50L);
        request.getQuestions().add(question);
        request.getQuestions().add(q2);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(submissionRepository.findAllByRequestIdRAndContactId(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.analyzeLiveResponse(1L, 5L, 10L, "Oui");

        String suggestion = (String) result.get("suggestion");
        assertThat(suggestion).contains("Recommanderiez-vous");
    }

    // ── generateCallSummary ──────────────────────────────────────────────────

    @Test
    @DisplayName("generateCallSummary — request introuvable → RuntimeException")
    void generateCallSummary_requestNotFound_throws() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateCallSummary(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request not found");
    }

    @Test
    @DisplayName("generateCallSummary — aucune réponse → retourne message 'Aucune réponse'")
    void generateCallSummary_noResponses_returnsNoDataMessage() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(submissionRepository.findAllByRequestIdRAndContactId(1L, 5L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.generateCallSummary(1L, 5L);

        assertThat(result.get("summary").toString()).contains("Aucune réponse");
        assertThat(result.get("completionRate")).isEqualTo(0);
    }

    @Test
    @DisplayName("generateCallSummary — avec réponses, AI désactivée → retourne fallback summary")
    void generateCallSummary_withResponses_aiDisabled_returnsFallback() {
        request.getQuestions().add(question);

        Submission submission = new Submission();
        submission.setId(1L);

        Response resp = new Response();
        resp.setQuestion(question);
        resp.setAnswer("Oui");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(submissionRepository.findAllByRequestIdRAndContactId(1L, 5L))
                .thenReturn(Collections.singletonList(submission));
        when(responseRepository.findBySubmission(submission))
                .thenReturn(Collections.singletonList(resp));

        Map<String, Object> result = service.generateCallSummary(1L, 5L);

        assertThat(result).containsKeys("summary", "tags", "sentiment", "completionRate");
        assertThat((int) result.get("completionRate")).isEqualTo(100);
    }
}
