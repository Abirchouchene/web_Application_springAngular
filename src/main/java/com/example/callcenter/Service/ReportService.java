package com.example.callcenter.Service;

import com.example.callcenter.DTO.ReportDTO;
import com.example.callcenter.DTO.RequestSummaryDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ReportRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    }}