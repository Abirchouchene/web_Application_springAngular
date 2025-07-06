package com.example.callcenter.Service;

import com.example.callcenter.DTO.ContactDTO;
import com.example.callcenter.DTO.ResponseDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.ResponseRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import com.example.callcenter.client.ContactClient;
import com.example.callcenter.DTO.ContactResponse;
import com.example.callcenter.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponseService {
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;
    private final SubmissionRepository submissionRepository;
    private final RequestRepository requestRepository;
    private final ContactClient contactClient;

    @Transactional
    public ResponseDTO addResponseToQuestion(Long requestId, Long questionId, Long contactId, List<String> responseValues) {
        if (responseValues == null || responseValues.isEmpty()) {
            throw new RuntimeException("Response value is required");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Find an existing submission or create a new one
        Submission submission = submissionRepository.findByRequestIdRAndContactId(requestId, contactId)
                .orElseGet(() -> {
                    Request request = requestRepository.findById(requestId)
                            .orElseThrow(() -> new RuntimeException("Request not found"));
                    Submission newSubmission = new Submission();
                    newSubmission.setRequest(request);
                    newSubmission.setContactId(contactId);
                    newSubmission.setSubmissionDate(LocalDate.now());
                    return submissionRepository.save(newSubmission);
                });

        Response response = new Response();
        response.setSubmission(submission);
        response.setQuestion(question);

        String value = responseValues.get(0); // used for most types

        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE, DROPDOWN -> {
                // ✅ Validate that the selected value is in the allowed options
                if (!question.getOptions().contains(value)) {
                    throw new RuntimeException("Selected value is not a valid option");
                }
                response.setAnswer(value);
            }

            case CHECKBOXES -> {
                // ✅ Validate all selected values are in the allowed options
                if (!new HashSet<>(question.getOptions()).containsAll(responseValues)) {
                    throw new RuntimeException("One or more selected values are not valid options");
                }
                response.setMultiAnswer(new ArrayList<>(responseValues));
            }

            case SHORT_ANSWER, PARAGRAPH -> {
                response.setAnswer(value);
            }

            case YES_OR_NO -> {
                response.setBooleanAnswer(Boolean.parseBoolean(value));
            }

            case NUMBER -> {
                try {
                    response.setNumberAnswer(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid number format");
                }
            }

            case DATE -> {
                try {
                    response.setDateAnswer(LocalDate.parse(value));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid date format (expected yyyy-MM-dd)");
                }
            }

            case TIME -> {
                try {
                    response.setTimeAnswer(LocalTime.parse(value));
                } catch (Exception e) {
                    throw new RuntimeException("Invalid time format (expected HH:mm)");
                }
            }

            default -> throw new IllegalArgumentException("Unsupported question type");
        }

        responseRepository.save(response);
        return mapToDTO(response);
    }

    private ResponseDTO mapToDTO(Response response) {
        ResponseDTO dto = new ResponseDTO();
        dto.setId(response.getId());
        dto.setAnswer(response.getAnswer());
        dto.setMultiAnswer(response.getMultiAnswer());
        dto.setBooleanAnswer(response.getBooleanAnswer());
        dto.setNumberAnswer(response.getNumberAnswer());
        dto.setDateAnswer(response.getDateAnswer());
        dto.setTimeAnswer(response.getTimeAnswer());

        // Add question information from the response's question field
        if (response.getQuestion() != null) {
            dto.setQuestionId(response.getQuestion().getId());
            dto.setQuestionText(response.getQuestion().getText());
            dto.setQuestionType(response.getQuestion().getQuestionType().name());
        }

        if (response.getSubmission() != null && response.getSubmission().getContactId() != null) {
            Long contactId = response.getSubmission().getContactId();
            dto.setContactId(contactId);

            try {
                ContactResponse contact = contactClient.getContactById(contactId);

                dto.setContactName(contact.getName());

                ContactDTO contactDTO = new ContactDTO();
                contactDTO.setName(contact.getName());
                contactDTO.setPhoneNumber(contact.getPhoneNumber());
                contactDTO.setTagIds(contact.getTagIds());

                dto.setContact(contactDTO);
            } catch (Exception e) {
                dto.setContactName("Unknown");
            }
        }

        return dto;
    }
    @Transactional
    public void deleteResponseById(Long responseId) {
        Response response = responseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Response not found"));

        if (response.getMultiAnswer() != null) {
            response.getMultiAnswer().clear();
        }

        responseRepository.delete(response);
    }

    /**
     * Get all responses for a specific contact and request
     */
    public List<ResponseDTO> getResponsesByContactAndRequest(Long contactId, Long requestId) {
        // Find the specific submission for this contact and request
        return submissionRepository.findByRequestIdRAndContactId(requestId, contactId)
                .map(submission -> {
                    // If the submission is found, get all responses for it
                    List<Response> responses = responseRepository.findBySubmission(submission);
                    return responses.stream()
                            .map(this::mapToDTO)
                            .toList();
                })
                .orElse(new ArrayList<>()); // If no submission is found, return an empty list
    }
}
