package com.example.callcenter.Service;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Response;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResponseService {
    private final QuestionRepository questionRepository;


    private final ResponseRepository responseRepository;

    public Question addResponsesToQuestion(Long questionId, List<String> responseValues) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Set<Response> newResponses = new HashSet<>();

        for (String value : responseValues) {
            Response response = new Response();
            response.getQuestions().add(question); // ensure bidirectional relation

            switch (question.getQuestionType()) {
                case  YES_OR_NO  -> response.setResponseText(value);
                case NUMBER -> {
                    try {
                        Double numericValue = Double.parseDouble(value);
                        response.setResponseNumber(numericValue);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format: " + value);
                    }
                }
                default -> throw new IllegalStateException("Unsupported question type: " + question.getQuestionType());
            }

            newResponses.add(responseRepository.save(response));
        }

        question.getResponses().addAll(newResponses);
        return questionRepository.save(question);
    }

}
