package com.survey.controller;

import com.survey.dto.PagedResponse;
import com.survey.dto.QuestionRequestDTO;
import com.survey.dto.QuestionResponseDTO;
import com.survey.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    @Autowired
    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<QuestionResponseDTO>> getAllQuestions(
            @RequestParam(required = false) Long surveyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        Pageable pageable = buildPageable(page, size, sort, direction);
        PagedResponse<QuestionResponseDTO> questions =
                surveyId != null
                        ? questionService.findBySurveyId(surveyId, pageable)
                        : questionService.findAll(pageable);

        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponseDTO> getQuestionById(@PathVariable Long id) {
        QuestionResponseDTO question = questionService.findById(id);
        return ResponseEntity.ok(question);
    }

    @PostMapping
    public ResponseEntity<QuestionResponseDTO> createQuestion(@Valid @RequestBody QuestionRequestDTO requestDTO) {
        QuestionResponseDTO createdQuestion = questionService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<QuestionResponseDTO>> createQuestionsBatch(@Valid @RequestBody List<QuestionRequestDTO> requestDTOs) {
        List<QuestionResponseDTO> createdQuestions = questionService.createBatch(requestDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponseDTO> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequestDTO requestDTO) {
        QuestionResponseDTO updatedQuestion = questionService.update(id, requestDTO);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(int page, int size, String sort, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            dir = Sort.Direction.ASC;
        }
        Sort sortSpec = Sort.by(dir, sort == null || sort.isBlank() ? "id" : sort);
        return PageRequest.of(safePage, safeSize, sortSpec);
    }
}
