package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CallCopilotService {

    private final WebClient webClient;
    private final DataAnonymizationService anonymizationService;
    private final RequestRepository requestRepository;
    private final SubmissionRepository submissionRepository;
    private final ResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-4}")
    private String model;

    @Value("${openai.enabled:false}")
    private boolean enabled;

    public CallCopilotService(@Value("${openai.api-key:}") String apiKey,
                              @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
                              DataAnonymizationService anonymizationService,
                              RequestRepository requestRepository,
                              SubmissionRepository submissionRepository,
                              ResponseRepository responseRepository,
                              QuestionRepository questionRepository,
                              ObjectMapper objectMapper) {
        this.anonymizationService = anonymizationService;
        this.requestRepository = requestRepository;
        this.submissionRepository = submissionRepository;
        this.responseRepository = responseRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Analyze a live response during a call — suggest next question, detect urgency, reformulate if vague.
     */
    public Map<String, Object> analyzeLiveResponse(Long requestId, Long contactId, Long questionId, String answer) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        Question currentQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Gather existing responses for context
        List<Map<String, String>> previousResponses = getPreviousResponses(requestId, contactId);
        List<Map<String, String>> remainingQuestions = getRemainingQuestions(request, requestId, contactId, questionId);

        if (!enabled) {
            return generateFallbackAnalysis(currentQuestion, answer, previousResponses, remainingQuestions, request);
        }

        try {
            String context = buildAnalysisContext(request, currentQuestion, answer, previousResponses, remainingQuestions);
            String aiResponse = callOpenAi(context, getAnalysisSystemPrompt());
            return parseAnalysisResponse(aiResponse, currentQuestion, answer, remainingQuestions);
        } catch (Exception e) {
            log.error("OpenAI call failed for live analysis, using fallback", e);
            return generateFallbackAnalysis(currentQuestion, answer, previousResponses, remainingQuestions, request);
        }
    }

    /**
     * Generate an auto-summary of the call with a contact — 2-3 lines summary + 3-5 tags.
     */
    public Map<String, Object> generateCallSummary(Long requestId, Long contactId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        List<Map<String, String>> allResponses = getPreviousResponses(requestId, contactId);

        if (allResponses.isEmpty()) {
            return Map.of(
                    "summary", "Aucune réponse collectée pour ce contact.",
                    "tags", List.of(),
                    "sentiment", "NEUTRAL",
                    "keyPoints", List.of(),
                    "completionRate", 0
            );
        }

        int totalQuestions = request.getQuestions() != null ? request.getQuestions().size() : 0;
        int answeredQuestions = allResponses.size();
        int completionRate = totalQuestions > 0 ? (answeredQuestions * 100) / totalQuestions : 0;

        if (!enabled) {
            return generateFallbackSummary(request, allResponses, completionRate);
        }

        try {
            String context = buildSummaryContext(request, allResponses, completionRate);
            String aiResponse = callOpenAi(context, getSummarySystemPrompt());
            Map<String, Object> result = parseSummaryResponse(aiResponse);
            result.put("completionRate", completionRate);
            return result;
        } catch (Exception e) {
            log.error("OpenAI call failed for summary generation, using fallback", e);
            return generateFallbackSummary(request, allResponses, completionRate);
        }
    }

    // ======================== PRIVATE HELPERS ========================

    private List<Map<String, String>> getPreviousResponses(Long requestId, Long contactId) {
        List<Submission> submissions = submissionRepository.findAllByRequestIdRAndContactId(requestId, contactId);
        if (submissions.isEmpty()) return new ArrayList<>();

        List<Response> responses = responseRepository.findBySubmission(submissions.get(0));
        return responses.stream().map(r -> {
            Map<String, String> map = new LinkedHashMap<>();
            if (r.getQuestion() != null) {
                map.put("question", r.getQuestion().getText());
                map.put("type", r.getQuestion().getQuestionType().name());
            }
            map.put("answer", extractAnswerText(r));
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, String>> getRemainingQuestions(Request request, Long requestId, Long contactId, Long currentQuestionId) {
        if (request.getQuestions() == null) return new ArrayList<>();

        Set<Long> answeredIds = new HashSet<>();
        List<Submission> submissions = submissionRepository.findAllByRequestIdRAndContactId(requestId, contactId);
        if (!submissions.isEmpty()) {
            List<Response> responses = responseRepository.findBySubmission(submissions.get(0));
            for (Response r : responses) {
                if (r.getQuestion() != null) answeredIds.add(r.getQuestion().getId());
            }
        }
        answeredIds.add(currentQuestionId); // currently being answered

        return request.getQuestions().stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .map(q -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("id", String.valueOf(q.getId()));
                    map.put("text", q.getText());
                    map.put("type", q.getQuestionType().name());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String extractAnswerText(Response r) {
        if (r.getAnswer() != null) return r.getAnswer();
        if (r.getMultiAnswer() != null && !r.getMultiAnswer().isEmpty()) return String.join(", ", r.getMultiAnswer());
        if (r.getBooleanAnswer() != null) return r.getBooleanAnswer() ? "Oui" : "Non";
        if (r.getNumberAnswer() != null) return String.valueOf(r.getNumberAnswer());
        if (r.getDateAnswer() != null) return r.getDateAnswer().toString();
        if (r.getTimeAnswer() != null) return r.getTimeAnswer().toString();
        return "";
    }

    private String callOpenAi(String userContent, String systemPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.4,
                "max_tokens", 1500
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("Empty response from OpenAI");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in OpenAI response");

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String getAnalysisSystemPrompt() {
        return """
                Tu es un assistant IA expert en centre d'appel. Pendant qu'un agent mène un sondage téléphonique,
                analyse la réponse du contact et fournis des suggestions utiles.
                
                Retourne un JSON avec cette structure exacte :
                {
                  "suggestion": "Suggestion pour la prochaine question ou reformulation",
                  "reformulation": "Si la réponse est vague, propose une reformulation de la question. Sinon null",
                  "detectedPoints": [
                    { "type": "URGENCY|PROBLEM|DISSATISFACTION|POSITIVE|INFO", "label": "Court libellé", "detail": "Détail" }
                  ],
                  "nextQuestionHint": "Conseil pour aborder la prochaine question",
                  "sentiment": "POSITIVE|NEUTRAL|NEGATIVE|MIXED",
                  "confidence": 0.0-1.0
                }
                Retourne UNIQUEMENT du JSON valide, pas de markdown.
                """;
    }

    private String getSummarySystemPrompt() {
        return """
                Tu es un assistant IA expert en centre d'appel. Génère un résumé automatique d'un appel téléphonique
                basé sur les réponses collectées pendant le sondage.
                
                Retourne un JSON avec cette structure exacte :
                {
                  "summary": "Résumé concis de l'appel en 2-3 phrases",
                  "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
                  "sentiment": "POSITIVE|NEUTRAL|NEGATIVE|MIXED",
                  "keyPoints": [
                    { "type": "IMPORTANT|WARNING|POSITIVE|NEUTRAL", "text": "Point clé identifié" }
                  ]
                }
                Les tags doivent être 3 à 5 mots-clés pertinents. Retourne UNIQUEMENT du JSON valide.
                """;
    }

    private String buildAnalysisContext(Request request, Question currentQ, String answer,
                                        List<Map<String, String>> previousResponses,
                                        List<Map<String, String>> remainingQuestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Contexte de l'enquête: ").append(request.getTitle()).append("\n");
        sb.append("Catégorie: ").append(request.getCategoryRequest()).append("\n\n");

        if (!previousResponses.isEmpty()) {
            sb.append("Réponses précédentes:\n");
            for (Map<String, String> r : previousResponses) {
                sb.append("- Q: ").append(r.get("question")).append(" → R: ").append(r.get("answer")).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Question actuelle: ").append(currentQ.getText()).append("\n");
        sb.append("Type: ").append(currentQ.getQuestionType().name()).append("\n");
        sb.append("Réponse donnée: ").append(answer).append("\n\n");

        if (!remainingQuestions.isEmpty()) {
            sb.append("Questions restantes:\n");
            for (Map<String, String> q : remainingQuestions) {
                sb.append("- ").append(q.get("text")).append(" (").append(q.get("type")).append(")\n");
            }
        }

        return anonymizationService.anonymizeText(sb.toString());
    }

    private String buildSummaryContext(Request request, List<Map<String, String>> allResponses, int completionRate) {
        StringBuilder sb = new StringBuilder();
        sb.append("Enquête: ").append(request.getTitle()).append("\n");
        sb.append("Type: ").append(request.getRequestType()).append("\n");
        sb.append("Catégorie: ").append(request.getCategoryRequest()).append("\n");
        sb.append("Taux de complétion: ").append(completionRate).append("%\n\n");
        sb.append("Réponses collectées:\n");
        for (Map<String, String> r : allResponses) {
            sb.append("- Q: ").append(r.get("question")).append(" → R: ").append(r.get("answer")).append("\n");
        }
        return anonymizationService.anonymizeText(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalysisResponse(String aiResponse, Question currentQ, String answer,
                                                       List<Map<String, String>> remainingQuestions) {
        try {
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            Map<String, Object> result = objectMapper.readValue(cleaned, new TypeReference<>() {});
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AI analysis response, returning fallback");
            return generateFallbackAnalysis(currentQ, answer, List.of(), remainingQuestions, null);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSummaryResponse(String aiResponse) {
        try {
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse AI summary response, returning raw text");
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("summary", aiResponse.length() > 300 ? aiResponse.substring(0, 300) : aiResponse);
            fallback.put("tags", List.of("appel", "sondage"));
            fallback.put("sentiment", "NEUTRAL");
            fallback.put("keyPoints", List.of());
            return fallback;
        }
    }

    // ======================== FALLBACK (rule-based) ========================

    private Map<String, Object> generateFallbackAnalysis(Question currentQ, String answer,
                                                          List<Map<String, String>> previousResponses,
                                                          List<Map<String, String>> remainingQuestions,
                                                          Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> detectedPoints = new ArrayList<>();
        String sentiment = "NEUTRAL";
        String suggestion = "";
        String reformulation = null;
        double confidence = 0.7;

        // Detect vague/short answers for text-based questions
        if (currentQ != null && answer != null) {
            String type = currentQ.getQuestionType().name();
            boolean isTextAnswer = type.equals("SHORT_ANSWER") || type.equals("PARAGRAPH");

            if (isTextAnswer && answer.trim().length() < 10) {
                reformulation = "La réponse semble courte. Essayez : « Pourriez-vous préciser votre réponse concernant : " 
                        + currentQ.getText() + " ? »";
                confidence = 0.5;
            }

            // Detect negative keywords
            String lowerAnswer = answer.toLowerCase();
            if (containsAny(lowerAnswer, "problème", "erreur", "bug", "panne", "dysfonctionnement", "défaut")) {
                detectedPoints.add(Map.of("type", "PROBLEM", "label", "Problème détecté", "detail", "Le contact mentionne un problème technique."));
                sentiment = "NEGATIVE";
            }
            if (containsAny(lowerAnswer, "urgent", "immédiat", "critique", "vite", "rapidement", "asap")) {
                detectedPoints.add(Map.of("type", "URGENCY", "label", "Urgence signalée", "detail", "Le contact exprime un besoin urgent."));
            }
            if (containsAny(lowerAnswer, "mécontent", "insatisfait", "déçu", "mauvais", "nul", "inadmissible", "inacceptable")) {
                detectedPoints.add(Map.of("type", "DISSATISFACTION", "label", "Insatisfaction", "detail", "Le contact exprime de l'insatisfaction."));
                sentiment = "NEGATIVE";
            }
            if (containsAny(lowerAnswer, "satisfait", "content", "bien", "excellent", "parfait", "merci", "super", "génial")) {
                detectedPoints.add(Map.of("type", "POSITIVE", "label", "Satisfaction", "detail", "Le contact exprime de la satisfaction."));
                sentiment = "POSITIVE";
            }

            // YES_OR_NO negative
            if (type.equals("YES_OR_NO") && "false".equalsIgnoreCase(answer)) {
                detectedPoints.add(Map.of("type", "INFO", "label", "Réponse négative", "detail", "Le contact a répondu Non à : " + currentQ.getText()));
            }
        }

        // Suggest next question
        if (!remainingQuestions.isEmpty()) {
            Map<String, String> next = remainingQuestions.get(0);
            suggestion = "Prochaine question recommandée : « " + next.get("text") + " » (" + formatQuestionType(next.get("type")) + ")";
        } else {
            suggestion = "Toutes les questions ont été posées. Vous pouvez générer le résumé de l'appel.";
        }

        String nextQuestionHint = "";
        if (!remainingQuestions.isEmpty()) {
            String nextType = remainingQuestions.get(0).get("type");
            nextQuestionHint = switch (nextType) {
                case "YES_OR_NO" -> "Question fermée — attendez une réponse claire Oui/Non.";
                case "PARAGRAPH", "SHORT_ANSWER" -> "Question ouverte — laissez le contact s'exprimer librement.";
                case "NUMBER" -> "Question numérique — demandez une valeur précise.";
                case "MULTIPLE_CHOICE", "DROPDOWN" -> "Question à choix — lisez les options au contact.";
                default -> "Posez la question naturellement et notez la réponse.";
            };
        }

        result.put("suggestion", suggestion);
        result.put("reformulation", reformulation);
        result.put("detectedPoints", detectedPoints);
        result.put("nextQuestionHint", nextQuestionHint);
        result.put("sentiment", sentiment);
        result.put("confidence", confidence);
        return result;
    }

    private Map<String, Object> generateFallbackSummary(Request request, List<Map<String, String>> allResponses, int completionRate) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Build summary from responses
        int positiveCount = 0, negativeCount = 0;
        List<String> tags = new ArrayList<>();
        List<Map<String, String>> keyPoints = new ArrayList<>();

        for (Map<String, String> r : allResponses) {
            String ans = (r.get("answer") != null ? r.get("answer") : "").toLowerCase();
            if (containsAny(ans, "satisfait", "bien", "excellent", "oui", "true")) positiveCount++;
            if (containsAny(ans, "problème", "mécontent", "non", "false", "mauvais")) negativeCount++;
        }

        // Overall sentiment
        String sentiment;
        if (positiveCount > negativeCount * 2) sentiment = "POSITIVE";
        else if (negativeCount > positiveCount * 2) sentiment = "NEGATIVE";
        else if (positiveCount > 0 && negativeCount > 0) sentiment = "MIXED";
        else sentiment = "NEUTRAL";

        // Tags from category and request type
        if (request.getCategoryRequest() != null) tags.add(formatCategory(request.getCategoryRequest().name()));
        if (request.getRequestType() != null) tags.add(request.getRequestType().name().equals("STATISTICS") ? "Statistique" : "Information");
        tags.add("Appel");
        if (completionRate == 100) tags.add("Complet");
        else tags.add("Partiel");
        if (sentiment.equals("NEGATIVE")) tags.add("À suivre");

        // Key points
        if (completionRate < 50) {
            keyPoints.add(Map.of("type", "WARNING", "text", "Moins de 50% des questions ont été répondues."));
        }
        if (negativeCount > 0) {
            keyPoints.add(Map.of("type", "WARNING", "text", negativeCount + " réponse(s) négative(s) détectée(s)."));
        }
        if (positiveCount > 0) {
            keyPoints.add(Map.of("type", "POSITIVE", "text", positiveCount + " réponse(s) positive(s) détectée(s)."));
        }
        keyPoints.add(Map.of("type", "NEUTRAL", "text", allResponses.size() + " réponses collectées sur " +
                (request.getQuestions() != null ? request.getQuestions().size() : "?") + " questions."));

        // Build summary text
        String summary = String.format("Appel concernant « %s » (%s). %d/%d questions répondues (%d%% de complétion). " +
                        "Sentiment général : %s.",
                request.getTitle(),
                formatCategory(request.getCategoryRequest() != null ? request.getCategoryRequest().name() : ""),
                allResponses.size(),
                request.getQuestions() != null ? request.getQuestions().size() : 0,
                completionRate,
                formatSentiment(sentiment));

        result.put("summary", summary);
        result.put("tags", tags);
        result.put("sentiment", sentiment);
        result.put("keyPoints", keyPoints);
        result.put("completionRate", completionRate);
        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String formatQuestionType(String type) {
        return switch (type) {
            case "YES_OR_NO" -> "Oui/Non";
            case "MULTIPLE_CHOICE" -> "Choix multiple";
            case "DROPDOWN" -> "Menu déroulant";
            case "SHORT_ANSWER" -> "Réponse courte";
            case "PARAGRAPH" -> "Paragraphe";
            case "NUMBER" -> "Nombre";
            case "DATE" -> "Date";
            case "TIME" -> "Heure";
            case "CHECKBOXES" -> "Cases à cocher";
            default -> type;
        };
    }

    private String formatCategory(String cat) {
        return switch (cat) {
            case "PRODUCT_SATISFACTION" -> "Satisfaction Produit";
            case "SERVICE_FEEDBACK" -> "Retour Service";
            case "MARKET_RESEARCH" -> "Étude de Marché";
            case "CUSTOMER_NEEDS" -> "Besoins Client";
            case "GENERAL_INQUIRY" -> "Demande Générale";
            case "RECLAMATION" -> "Réclamation";
            case "COMMANDE" -> "Commande";
            case "DEVIS" -> "Devis";
            case "INTERVENTION" -> "Intervention";
            default -> cat != null ? cat : "";
        };
    }

    private String formatSentiment(String sentiment) {
        return switch (sentiment) {
            case "POSITIVE" -> "Positif";
            case "NEGATIVE" -> "Négatif";
            case "MIXED" -> "Mixte";
            default -> "Neutre";
        };
    }
}
