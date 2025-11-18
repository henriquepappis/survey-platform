package com.survey.controller;

import com.survey.dto.PagedResponse;
import com.survey.dto.SurveyRequestDTO;
import com.survey.dto.SurveyResponseDTO;
import com.survey.service.SurveyService;
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
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    @Autowired
    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<SurveyResponseDTO>> getAllSurveys(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        Pageable pageable = buildPageable(page, size, sort, direction);
        PagedResponse<SurveyResponseDTO> surveys =
                (ativo != null && ativo)
                        ? surveyService.findAllAtivas(pageable)
                        : surveyService.findAll(pageable);

        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyResponseDTO> getSurveyById(@PathVariable Long id) {
        SurveyResponseDTO survey = surveyService.findById(id);
        return ResponseEntity.ok(survey);
    }

    @PostMapping
    public ResponseEntity<SurveyResponseDTO> createSurvey(@Valid @RequestBody SurveyRequestDTO requestDTO) {
        SurveyResponseDTO createdSurvey = surveyService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSurvey);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SurveyResponseDTO>> createSurveysBatch(@Valid @RequestBody List<SurveyRequestDTO> requestDTOs) {
        List<SurveyResponseDTO> createdSurveys = surveyService.createBatch(requestDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSurveys);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SurveyResponseDTO> updateSurvey(
            @PathVariable Long id,
            @Valid @RequestBody SurveyRequestDTO requestDTO) {
        SurveyResponseDTO updatedSurvey = surveyService.update(id, requestDTO);
        return ResponseEntity.ok(updatedSurvey);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        surveyService.delete(id);
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
