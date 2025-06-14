package com.example.callcenter.Controller;

import com.example.callcenter.DTO.ResponseDTO;
import com.example.callcenter.Entity.Question;
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

    @PostMapping("/question/{questionId}/contact/{contactId}")
    public ResponseEntity<ResponseDTO> addResponsesToQuestion(
            @PathVariable Long questionId,
            @PathVariable Long contactId,
            @RequestBody List<String> responseValues
    ) {
        ResponseDTO responseDTO = responseService.addResponseToQuestion(questionId, contactId, responseValues);
        return ResponseEntity.ok(responseDTO);
    }
}