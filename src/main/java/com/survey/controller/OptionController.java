package com.survey.controller;

import com.survey.dto.OptionRequestDTO;
import com.survey.dto.OptionResponseDTO;
import com.survey.dto.PagedResponse;
import com.survey.service.OptionService;
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
@RequestMapping("/api/options")
public class OptionController {

    private final OptionService optionService;

    @Autowired
    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OptionResponseDTO>> getAllOptions(
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        Pageable pageable = buildPageable(page, size, sort, direction);
        PagedResponse<OptionResponseDTO> options =
                questionId != null
                        ? optionService.findByQuestionId(questionId, ativo, pageable)
                        : optionService.findAll(pageable);

        return ResponseEntity.ok(options);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OptionResponseDTO> getOptionById(@PathVariable Long id) {
        OptionResponseDTO option = optionService.findById(id);
        return ResponseEntity.ok(option);
    }

    @PostMapping
    public ResponseEntity<OptionResponseDTO> createOption(@Valid @RequestBody OptionRequestDTO requestDTO) {
        OptionResponseDTO createdOption = optionService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOption);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<OptionResponseDTO>> createOptionsBatch(@Valid @RequestBody List<OptionRequestDTO> requestDTOs) {
        List<OptionResponseDTO> createdOptions = optionService.createBatch(requestDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOptions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OptionResponseDTO> updateOption(
            @PathVariable Long id,
            @Valid @RequestBody OptionRequestDTO requestDTO) {
        OptionResponseDTO updatedOption = optionService.update(id, requestDTO);
        return ResponseEntity.ok(updatedOption);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {
        optionService.delete(id);
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
