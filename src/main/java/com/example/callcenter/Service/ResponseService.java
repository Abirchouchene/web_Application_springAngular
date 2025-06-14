package com.example.callcenter.Service;

import com.example.callcenter.DTO.ContactDTO;
import com.example.callcenter.DTO.ResponseDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.ResponseRepository;
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
    private final ContactRepository contactRepository;

    public ResponseDTO addResponseToQuestion(Long questionId, Long contactId, List<String> responseValues) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        Response response = responseRepository
                .findByQuestionAndContact(question, contact)
                .orElse(new Response());

        response.setQuestion(question);
        response.setContact(contact);

        // Process each response value
        for (String value : responseValues) {
            String[] parts = value.split(":", 2);
            if (parts.length != 2) continue;

            String type = parts[0];
            String val = parts[1];

            switch (type) {
                case "answer" -> response.setAnswer(val);
                case "multiAnswer" -> {
                    if (response.getMultiAnswer() == null) {
                        response.setMultiAnswer(new ArrayList<>());
                    }
                    response.getMultiAnswer().add(val);
                }
                case "booleanAnswer" -> response.setBooleanAnswer(Boolean.parseBoolean(val));
                case "numberAnswer" -> response.setNumberAnswer(Double.parseDouble(val));
                case "dateAnswer" -> response.setDateAnswer(LocalDate.parse(val));
                case "timeAnswer" -> response.setTimeAnswer(LocalTime.parse(val));
            }
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

        Contact contact = response.getContact();
        if (contact != null) {
            ContactDTO contactDTO = new ContactDTO();
            contactDTO.setName(contact.getName());
            contactDTO.setPhoneNumber(contact.getPhoneNumber());

            // Assuming you have a getTags() method in Contact entity returning Set<Tag>
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
