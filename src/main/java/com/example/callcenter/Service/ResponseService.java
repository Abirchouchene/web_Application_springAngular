package com.example.callcenter.Service;

import com.example.callcenter.DTO.ContactDTO;
import com.example.callcenter.DTO.ResponseDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.ResponseRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponseService {
    private final QuestionRepository questionRepository;
    private final ResponseRepository responseRepository;

    private final SubmissionRepository submissionRepository;

    public ResponseDTO addResponseToQuestion(Long questionId, Long contactId, List<String> responseValues) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));


        Submission submission = submissionRepository.findByContactIdAndQuestion(contactId, question)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Response response = responseRepository.findByQuestionAndSubmission(question, submission)
                .orElse(new Response());

        response.setQuestion(question);
        response.setSubmission(submission);

        if (responseValues.isEmpty()) {
            throw new RuntimeException("Response value is required");
        }

        String value = responseValues.get(0); // assuming one response per question
        switch (question.getQuestionType()) {
            case SHORT_ANSWER, PARAGRAPH, MULTIPLE_CHOICE, DROPDOWN ->
                    response.setAnswer(value);

            case CHECKBOXES -> {
                if (response.getMultiAnswer() == null) {
                    response.setMultiAnswer(new ArrayList<>());
                }
                response.getMultiAnswer().add(value);
            }

            case YES_OR_NO -> response.setBooleanAnswer(Boolean.parseBoolean(value));

            case NUMBER -> response.setNumberAnswer(Double.parseDouble(value));

            case DATE -> response.setDateAnswer(LocalDate.parse(value));

            case TIME -> response.setTimeAnswer(LocalTime.parse(value));


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

        // Get contact through submission
        if (response.getSubmission() != null && response.getSubmission().getContact() != null) {
            Contact contact = response.getSubmission().getContact();

            dto.setContactName(contact.getName());

            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setName(contact.getName());
            contactDTO.setPhoneNumber(contact.getPhoneNumber());

            if (contact.getTags() != null) {
                Set<Long> tagIds = contact.getTags()
                        .stream()
                        .map(Tag::getId)
                        .collect(Collectors.toSet());
                contactDTO.setTagIds(tagIds);
            }

            dto.setContact(contactDTO);
        }

        return dto;
    }
}
