package com.example.callcenter.Service;

import com.example.callcenter.DTO.ReportDTO;
import com.example.callcenter.DTO.RequestSummaryDTO;
import com.example.callcenter.DTO.AiInsightDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ReportRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final RequestRepository requestRepository;
    private final SubmissionRepository submissionRepository;
    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final MinioStorageService minioStorageService;

    public Report generateReport(Long requestId) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

        List<Submission> submissions = submissionRepository.findByRequest(request);
        int totalContacts = submissions.size();
        int contactedContacts = (int) submissions.stream()
            .filter(sub -> sub.getResponses() != null && !sub.getResponses().isEmpty())
            .count();
        double contactRate = totalContacts > 0 ? (contactedContacts * 100.0) / totalContacts : 0.0;

        Map<String, Object> statistics = aggregateStatistics(request, submissions);

        String statisticsData;
        try {
            statisticsData = objectMapper.writeValueAsString(statistics);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize statistics", e);
        }

        // Check if a report already exists for this request
        Optional<Report> existing = reportRepository.findByRequest(request);
        Report report = existing.orElse(new Report());
        report.setRequest(request);
        report.setRequestTitle(request.getTitle());
        report.setRequestType(request.getRequestType());
        report.setGeneratedDate(LocalDateTime.now());
        report.setStatus(ReportStatus.PENDING_APPROVAL);
        report.setTotalContacts(totalContacts);
        report.setContactedContacts(contactedContacts);
        report.setContactRate(contactRate);
        report.setStatisticsData(statisticsData);

        return reportRepository.save(report);
    }

    public Report getReportByRequest(Long requestId) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        return reportRepository.findByRequest(request)
            .orElseThrow(() -> new RuntimeException("Report not found for request"));
    }

    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public void approveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(ReportStatus.APPROVED);
        report.setApprovedDate(LocalDateTime.now());
        reportRepository.save(report);
    }

    public void rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(ReportStatus.REJECTED);
        reportRepository.save(report);
    }

    private Map<String, Object> aggregateStatistics(Request request, List<Submission> submissions) {
        List<Question> questionList = new ArrayList<>(request.getQuestions());
        List<Map<String, Object>> summaryByQuestion = buildSummaryByQuestion(questionList, submissions);
        List<Map<String, Object>> byContact = buildByContact(questionList, submissions);

        int totalContacts = submissions.size();
        int contactedContacts = (int) submissions.stream()
            .filter(sub -> sub.getResponses() != null && !sub.getResponses().isEmpty())
            .count();
        double contactRate = totalContacts > 0 ? (contactedContacts * 100.0) / totalContacts : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("summaryByQuestion", summaryByQuestion);
        result.put("byContact", byContact);
        result.put("totalContacts", totalContacts);
        result.put("contactedContacts", contactedContacts);
        result.put("contactRate", contactRate);
        return result;
    }

    private List<Response> getAllResponsesForQuestion(List<Submission> submissions, Long questionId) {
        List<Response> allResponses = new ArrayList<>();
        for (Submission submission : submissions) {
            List<Response> responses = submission.getResponses();
            if (responses != null) {
                for (Response response : responses) {
                    if (response.getQuestion() != null && response.getQuestion().getId().equals(questionId)) {
                        allResponses.add(response);
                    }
                }
            }
        }
        return allResponses;
    }

    private List<Map<String, Object>> buildSummaryByQuestion(List<Question> questionList, List<Submission> submissions) {
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Question question : questionList) {
            List<Response> allResponses = getAllResponsesForQuestion(submissions, question.getId());
            summary.add(buildQuestionSummary(question, allResponses));
        }
        return summary;
    }

    private Map<String, Object> buildQuestionSummary(Question question, List<Response> allResponses) {
        Map<String, Object> questionSummary = new HashMap<>();
        questionSummary.put("questionId", question.getId());
        questionSummary.put("questionText", question.getText());
        questionSummary.put("type", question.getQuestionType().name());

        switch (question.getQuestionType()) {
            case SHORT_ANSWER, PARAGRAPH -> questionSummary.put("responses", getTextAnswers(allResponses));
            case NUMBER -> addNumberStats(questionSummary, allResponses);
            case MULTIPLE_CHOICE, DROPDOWN -> questionSummary.put("optionCounts", getOptionCounts(allResponses));
            case CHECKBOXES -> questionSummary.put("optionCounts", getCheckboxCounts(allResponses));
            case YES_OR_NO -> questionSummary.put("optionCounts", getYesNoCounts(allResponses));
            case DATE -> questionSummary.put("responses", getDateAnswers(allResponses));
            case TIME -> questionSummary.put("responses", getTimeAnswers(allResponses));
        }
        return questionSummary;
    }

    private List<String> getTextAnswers(List<Response> responses) {
        return responses.stream()
                .map(Response::getAnswer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void addNumberStats(Map<String, Object> summary, List<Response> responses) {
        List<Double> numbers = responses.stream()
                .map(Response::getNumberAnswer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        summary.put("responses", numbers);
        if (!numbers.isEmpty()) {
            double avg = numbers.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double min = numbers.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = numbers.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            summary.put("stats", Map.of("average", avg, "min", min, "max", max));
        }
    }

    private Map<String, Long> getOptionCounts(List<Response> responses) {
        return responses.stream()
                .map(Response::getAnswer)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ans -> ans, Collectors.counting()));
    }

    private Map<String, Long> getCheckboxCounts(List<Response> responses) {
        return responses.stream()
                .flatMap(r -> r.getMultiAnswer() != null ? r.getMultiAnswer().stream() : Stream.empty())
                .collect(Collectors.groupingBy(ans -> ans, Collectors.counting()));
    }

    private Map<String, Long> getYesNoCounts(List<Response> responses) {
        return responses.stream()
                .map(r -> r.getBooleanAnswer() != null && r.getBooleanAnswer() ? "Yes" : "No")
                .collect(Collectors.groupingBy(ans -> ans, Collectors.counting()));
    }

    private List<String> getDateAnswers(List<Response> responses) {
        return responses.stream()
                .map(Response::getDateAnswer)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private List<String> getTimeAnswers(List<Response> responses) {
        return responses.stream()
                .map(Response::getTimeAnswer)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildByContact(List<Question> questionList, List<Submission> submissions) {
        List<Map<String, Object>> byContact = new ArrayList<>();
        for (Submission submission : submissions) {
            Map<String, Object> contactEntry = new HashMap<>();
            contactEntry.put("contactId", submission.getContactId());
            contactEntry.put("submissionDate", submission.getSubmissionDate());
            contactEntry.put("answers", buildAnswersForContact(submission, questionList));
            byContact.add(contactEntry);
        }
        return byContact;
    }

    private List<Map<String, Object>> buildAnswersForContact(Submission submission, List<Question> questionList) {
        List<Map<String, Object>> answers = new ArrayList<>();
        List<Response> responses = submission.getResponses();
        for (Question question : questionList) {
            Map<String, Object> answerEntry = new HashMap<>();
            answerEntry.put("questionId", question.getId());
            answerEntry.put("questionText", question.getText());
            // Find the response for this question
            Response response = (responses != null)
                ? responses.stream()
                    .filter(r -> r.getQuestion() != null && r.getQuestion().getId().equals(question.getId()))
                    .findFirst().orElse(null)
                : null;
            if (response != null) {
                answerEntry.put("answer", response.getAnswer());
                answerEntry.put("multiAnswer", response.getMultiAnswer());
                answerEntry.put("booleanAnswer", response.getBooleanAnswer());
                answerEntry.put("numberAnswer", response.getNumberAnswer());
                answerEntry.put("dateAnswer", response.getDateAnswer());
                answerEntry.put("timeAnswer", response.getTimeAnswer());
            } else {
                // Always include empty/null fields for missing responses
                answerEntry.put("answer", null);
                answerEntry.put("multiAnswer", null);
                answerEntry.put("booleanAnswer", null);
                answerEntry.put("numberAnswer", null);
                answerEntry.put("dateAnswer", null);
                answerEntry.put("timeAnswer", null);
            }
            answers.add(answerEntry);
        }
        return answers;
    }
    public ReportDTO toReportDTO(Report report) {
        if (report == null) {
            return null;
        }

        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setRequestTitle(report.getRequestTitle());
        dto.setRequestType(report.getRequestType());
        dto.setGeneratedDate(report.getGeneratedDate());
        dto.setStatus(report.getStatus());
        dto.setApprovedDate(report.getApprovedDate());
        dto.setSentDate(report.getSentDate());
        dto.setTotalContacts(report.getTotalContacts());
        dto.setContactedContacts(report.getContactedContacts());
        dto.setContactRate(report.getContactRate());
        dto.setStatisticsData(report.getStatisticsData());
        dto.setAiInsightsData(report.getAiInsightsData());
        dto.setAiGeneratedDate(report.getAiGeneratedDate());

        // Set the request summary if available
        if (report.getRequest() != null) {
            Request req = report.getRequest();
            RequestSummaryDTO requestSummary = new RequestSummaryDTO();
            requestSummary.setIdR(req.getIdR());
            requestSummary.setTitle(req.getTitle());
            requestSummary.setDescription(req.getDescription());
            requestSummary.setDeadline(req.getDeadline());
            requestSummary.setStatus(req.getStatus());
            requestSummary.setRequestType(req.getRequestType());
            requestSummary.setCategoryRequest(req.getCategoryRequest());
            requestSummary.setPriority(req.getPriority());
            requestSummary.setCreatedAt(req.getCreatedAt());
            requestSummary.setUpdatedAt(req.getUpdatedAt());
            
            // Set requester name
            if (req.getUser() != null) {
                requestSummary.setRequesterName(req.getUser().getFullName());
            } else {
                requestSummary.setRequesterName("N/A");
            }
            
            // Set agent name
            if (req.getAgent() != null) {
                requestSummary.setAgentName(req.getAgent().getFullName());
            } else {
                requestSummary.setAgentName("Non assigné");
            }
            
            dto.setRequest(requestSummary);
        } else {
            dto.setRequest(null);
        }

        return dto;
    }

    // ====== PDF Generation ======

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 37, 41));
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(52, 73, 94));
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(100, 100, 100));
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generatePdf(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // If PDF already exists in MinIO, download and return it
        if (report.getPdfPath() != null && minioStorageService.fileExists(report.getPdfPath())) {
            try (var is = minioStorageService.downloadFile(report.getPdfPath())) {
                return is.readAllBytes();
            } catch (Exception e) {
                // PDF not found in MinIO, regenerate
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Paragraph title = new Paragraph("Rapport d'enquête", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            Paragraph subtitle = new Paragraph(report.getRequestTitle(), SECTION_FONT);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Separator
            document.add(new Paragraph(" "));

            // Info section
            addSection(document, "Informations générales");
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{35, 65});
            infoTable.setSpacingAfter(15);

            addInfoRow(infoTable, "Type d'enquête", report.getRequestType() != null ? report.getRequestType().name() : "N/A");
            addInfoRow(infoTable, "Statut", report.getStatus() != null ? report.getStatus().name() : "N/A");
            addInfoRow(infoTable, "Date de génération", report.getGeneratedDate() != null ? report.getGeneratedDate().format(DATE_FMT) : "N/A");
            addInfoRow(infoTable, "Date d'approbation", report.getApprovedDate() != null ? report.getApprovedDate().format(DATE_FMT) : "—");
            addInfoRow(infoTable, "Contacts totaux", String.valueOf(report.getTotalContacts()));
            addInfoRow(infoTable, "Contacts enquêtés", String.valueOf(report.getContactedContacts()));
            addInfoRow(infoTable, "Taux de réponse", String.format("%.1f%%", report.getContactRate()));

            // Add requester / agent info if request is available
            if (report.getRequest() != null) {
                Request req = report.getRequest();
                if (req.getUser() != null) {
                    addInfoRow(infoTable, "Demandeur", req.getUser().getFullName());
                }
                if (req.getAgent() != null) {
                    addInfoRow(infoTable, "Agent", req.getAgent().getFullName());
                }
            }
            document.add(infoTable);

            // Statistics section
            if (report.getStatisticsData() != null && !report.getStatisticsData().isEmpty()) {
                addSection(document, "Résultats par question");
                addStatisticsSection(document, report.getStatisticsData());
            }

            // AI Insights section
            if (report.getAiInsightsData() != null && !report.getAiInsightsData().isEmpty()) {
                addAiInsightsSection(document, report.getAiInsightsData());
            }

            document.close();

            byte[] pdfBytes = baos.toByteArray();

            // Store PDF in MinIO
            String objectName = "reports/rapport-" + reportId + ".pdf";
            minioStorageService.uploadFile(objectName, pdfBytes, "application/pdf");

            // Save the MinIO path in the report entity
            report.setPdfPath(objectName);
            reportRepository.save(report);

            return pdfBytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addSection(Document document, String text) throws DocumentException {
        Paragraph section = new Paragraph(text, SECTION_FONT);
        section.setSpacingBefore(10);
        section.setSpacingAfter(10);
        document.add(section);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(6);
        table.addCell(valueCell);
    }

    @SuppressWarnings("unchecked")
    private void addStatisticsSection(Document document, String statisticsJson) throws Exception {
        Map<String, Object> stats = objectMapper.readValue(statisticsJson, new TypeReference<>() {});
        List<Map<String, Object>> summaryByQuestion = (List<Map<String, Object>>) stats.get("summaryByQuestion");
        if (summaryByQuestion == null) return;

        int qNum = 1;
        for (Map<String, Object> questionData : summaryByQuestion) {
            String questionText = (String) questionData.getOrDefault("questionText", "Question");
            String type = (String) questionData.getOrDefault("type", "");

            Paragraph qTitle = new Paragraph("Q" + qNum + ". " + questionText, LABEL_FONT);
            qTitle.setSpacingBefore(12);
            qTitle.setSpacingAfter(6);
            document.add(qTitle);

            switch (type) {
                case "MULTIPLE_CHOICE", "DROPDOWN", "CHECKBOXES", "YES_OR_NO" -> {
                    Map<String, Object> optionCounts = (Map<String, Object>) questionData.get("optionCounts");
                    if (optionCounts != null && !optionCounts.isEmpty()) {
                        PdfPTable optTable = new PdfPTable(2);
                        optTable.setWidthPercentage(80);
                        optTable.setWidths(new float[]{60, 40});
                        addTableHeader(optTable, "Option", "Nombre");
                        for (Map.Entry<String, Object> entry : optionCounts.entrySet()) {
                            addTableRow(optTable, entry.getKey(), String.valueOf(entry.getValue()));
                        }
                        optTable.setSpacingAfter(8);
                        document.add(optTable);
                    }
                }
                case "NUMBER" -> {
                    Map<String, Object> numStats = (Map<String, Object>) questionData.get("stats");
                    List<?> responses = (List<?>) questionData.get("responses");
                    if (numStats != null) {
                        PdfPTable numTable = new PdfPTable(2);
                        numTable.setWidthPercentage(60);
                        addTableHeader(numTable, "Statistique", "Valeur");
                        addTableRow(numTable, "Moyenne", String.format("%.2f", toDouble(numStats.get("average"))));
                        addTableRow(numTable, "Min", String.format("%.2f", toDouble(numStats.get("min"))));
                        addTableRow(numTable, "Max", String.format("%.2f", toDouble(numStats.get("max"))));
                        addTableRow(numTable, "Réponses", responses != null ? String.valueOf(responses.size()) : "0");
                        numTable.setSpacingAfter(8);
                        document.add(numTable);
                    }
                }
                default -> {
                    // TEXT, DATE, TIME - show list of responses
                    List<?> responses = (List<?>) questionData.get("responses");
                    if (responses != null && !responses.isEmpty()) {
                        for (Object r : responses) {
                            Paragraph p = new Paragraph("  • " + String.valueOf(r), VALUE_FONT);
                            document.add(p);
                        }
                        document.add(new Paragraph(" "));
                    } else {
                        document.add(new Paragraph("  Aucune réponse", VALUE_FONT));
                    }
                }
            }
            qNum++;
        }
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(new Color(52, 73, 94));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String col1, String col2) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, VALUE_FONT));
        c1.setPadding(5);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(col2, VALUE_FONT));
        c2.setPadding(5);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c2);
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    // ====== AI Insights PDF Section ======

    private static final Font AI_TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(41, 128, 185));
    private static final Font AI_SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(52, 73, 94));
    private static final Font AI_PRIORITY_HIGH = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(231, 76, 60));
    private static final Font AI_PRIORITY_MED = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(243, 156, 18));
    private static final Font AI_PRIORITY_LOW = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(39, 174, 96));

    private void addAiInsightsSection(Document document, String aiInsightsJson) throws Exception {
        AiInsightDTO insights = objectMapper.readValue(aiInsightsJson, AiInsightDTO.class);

        document.newPage();

        Paragraph aiTitle = new Paragraph("Analyse IA & Recommandations", AI_TITLE_FONT);
        aiTitle.setAlignment(Element.ALIGN_CENTER);
        aiTitle.setSpacingAfter(15);
        document.add(aiTitle);

        // Executive summary
        if (insights.getSummary() != null) {
            addSection(document, "Résumé exécutif");
            Paragraph summaryPara = new Paragraph(insights.getSummary(), VALUE_FONT);
            summaryPara.setSpacingAfter(12);
            document.add(summaryPara);
        }

        // Key Findings
        if (insights.getKeyFindings() != null && !insights.getKeyFindings().isEmpty()) {
            addSection(document, "Observations clés");
            PdfPTable findingsTable = new PdfPTable(3);
            findingsTable.setWidthPercentage(100);
            findingsTable.setWidths(new float[]{40, 40, 20});
            addTableHeader(findingsTable, "Observation", "Évidence", "Impact");

            for (AiInsightDTO.KeyFindingDTO finding : insights.getKeyFindings()) {
                addTableRow3(findingsTable,
                        finding.getFinding() != null ? finding.getFinding() : "",
                        finding.getEvidence() != null ? finding.getEvidence() : "",
                        finding.getImpact() != null ? finding.getImpact() : "");
            }
            findingsTable.setSpacingAfter(12);
            document.add(findingsTable);
        }

        // Recommendations
        if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
            addSection(document, "Recommandations");
            int recNum = 1;
            for (AiInsightDTO.RecommendationDTO rec : insights.getRecommendations()) {
                Font priorityFont = switch (rec.getPriority() != null ? rec.getPriority() : "") {
                    case "HIGH" -> AI_PRIORITY_HIGH;
                    case "MEDIUM" -> AI_PRIORITY_MED;
                    default -> AI_PRIORITY_LOW;
                };

                Paragraph recTitle = new Paragraph();
                recTitle.add(new Phrase(recNum + ". " + (rec.getTitle() != null ? rec.getTitle() : ""), AI_SUBTITLE_FONT));
                recTitle.add(new Phrase("  [" + (rec.getPriority() != null ? rec.getPriority() : "N/A") + "]", priorityFont));
                recTitle.setSpacingBefore(8);
                document.add(recTitle);

                if (rec.getDescription() != null) {
                    document.add(new Paragraph(rec.getDescription(), VALUE_FONT));
                }
                if (rec.getCategory() != null) {
                    document.add(new Paragraph("Catégorie: " + rec.getCategory(), LABEL_FONT));
                }
                if (rec.getActionable() != null) {
                    Paragraph action = new Paragraph("→ Action: " + rec.getActionable(), VALUE_FONT);
                    action.setSpacingAfter(6);
                    document.add(action);
                }
                recNum++;
            }
        }

        // Sentiment Analysis
        if (insights.getSentimentAnalysis() != null) {
            addSection(document, "Analyse de sentiment");
            AiInsightDTO.SentimentAnalysisDTO sentiment = insights.getSentimentAnalysis();

            PdfPTable sentTable = new PdfPTable(2);
            sentTable.setWidthPercentage(60);
            addTableHeader(sentTable, "Catégorie", "Pourcentage");
            addTableRow(sentTable, "Positif", String.format("%.1f%%", sentiment.getPositivePercent()));
            addTableRow(sentTable, "Neutre", String.format("%.1f%%", sentiment.getNeutralPercent()));
            addTableRow(sentTable, "Négatif", String.format("%.1f%%", sentiment.getNegativePercent()));
            addTableRow(sentTable, "Sentiment global", sentiment.getOverallSentiment() != null ? sentiment.getOverallSentiment() : "N/A");
            sentTable.setSpacingAfter(12);
            document.add(sentTable);
        }
    }

    private void addTableRow3(PdfPTable table, String col1, String col2, String col3) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, VALUE_FONT));
        c1.setPadding(5);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(col2, VALUE_FONT));
        c2.setPadding(5);
        table.addCell(c2);
        PdfPCell c3 = new PdfPCell(new Phrase(col3, VALUE_FONT));
        c3.setPadding(5);
        c3.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c3);
    }
}