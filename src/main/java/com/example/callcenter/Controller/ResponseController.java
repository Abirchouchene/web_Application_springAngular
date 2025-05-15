package com.example.callcenter.Controller;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Service.RequestService;
import com.example.callcenter.Service.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/response")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:4200")
public class ResponseController {
    private final ResponseService responseService;

    @PostMapping("/question/{questionId}")
    public ResponseEntity<Question> addResponsesToQuestion(
            @PathVariable Long questionId,
            @RequestBody List<String> responseValues
    ) {
        Question updatedQuestion = responseService.addResponsesToQuestion(questionId, responseValues);
        return ResponseEntity.ok(updatedQuestion);
    }
}
