package com.survey.controller;

import com.survey.dto.PagedResponse;
import com.survey.dto.SurveyDetailsResponseDTO;
import com.survey.dto.SurveyRequestDTO;
import com.survey.dto.SurveyResponseDTO;
import com.survey.service.SurveyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;
    private final com.survey.service.SurveyExportService surveyExportService;

    @Autowired
    public SurveyController(SurveyService surveyService, com.survey.service.SurveyExportService surveyExportService) {
        this.surveyService = surveyService;
        this.surveyExportService = surveyExportService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<SurveyResponseDTO>> getAllSurveys(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        Pageable pageable = buildPageable(page, size, sort, direction);
        PagedResponse<SurveyResponseDTO> surveys;
        if (includeDeleted) {
            surveys = surveyService.findAllIncludingDeleted(pageable);
        } else if (ativo != null && ativo) {
            surveys = surveyService.findAllAtivas(pageable);
        } else {
            surveys = surveyService.findAll(pageable);
        }

        return ResponseEntity.ok(surveys);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyResponseDTO> getSurveyById(@PathVariable Long id) {
        SurveyResponseDTO survey = surveyService.findById(id);
        return ResponseEntity.ok(survey);
    }

    @GetMapping("/{id}/structure")
    public ResponseEntity<SurveyDetailsResponseDTO> getSurveyStructure(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeInactiveOptions,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(surveyService.getSurveyStructure(id, includeInactiveOptions, includeDeleted));
    }

    @PostMapping
    public ResponseEntity<SurveyResponseDTO> createSurvey(@Valid @RequestBody SurveyRequestDTO requestDTO) {
        SurveyResponseDTO createdSurvey = surveyService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSurvey);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportSurvey(@PathVariable Long id,
                                               @RequestParam(defaultValue = "false") boolean includeDeleted) {
        byte[] bytes = surveyExportService.exportSurveyAsXlsx(id, includeDeleted);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=survey-" + id + ".xlsx");
        return ResponseEntity.ok().headers(headers).body(bytes);
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

    @PatchMapping("/{id}/restore")
    public ResponseEntity<SurveyResponseDTO> restoreSurvey(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.restore(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSurvey(@PathVariable Long id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(int page, int size, String sort, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
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
