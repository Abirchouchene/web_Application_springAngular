package com.example.callcenter.Controller;

import com.example.callcenter.Entity.CategoryRequest;
import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.QuestionType;
import com.example.callcenter.Service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions(
            @RequestParam(required = false) CategoryRequest category,
            @RequestParam(required = false) QuestionType type) {
        return ResponseEntity.ok(requestService.getQuestionsByCategoryAndType(category, type));
    }
}
