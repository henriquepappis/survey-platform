package com.survey.controller;

import com.survey.dto.VoteRequestDTO;
import com.survey.dto.VoteResponseDTO;
import com.survey.service.VoteService;
import com.survey.service.VoteRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;
    private final VoteRateLimiter voteRateLimiter;

    public VoteController(VoteService voteService, VoteRateLimiter voteRateLimiter) {
        this.voteService = voteService;
        this.voteRateLimiter = voteRateLimiter;
    }

    @PostMapping
    @Operation(
            summary = "Registrar voto",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = VoteRequestDTO.class),
                            examples = {
                                    @ExampleObject(name = "Voto simples",
                                            value = """
                                                    {
                                                      "surveyId": 1,
                                                      "questionId": 10,
                                                      "optionId": 100
                                                    }
                                                    """),
                                    @ExampleObject(name = "Voto com audiência",
                                            value = """
                                                    {
                                                      "surveyId": 1,
                                                      "questionId": 10,
                                                      "optionId": 100,
                                                      "deviceType": "mobile",
                                                      "operatingSystem": "Android",
                                                      "browser": "Chrome",
                                                      "source": "paid-ads",
                                                      "country": "BR",
                                                      "state": "SP",
                                                      "city": "São Paulo",
                                                      "status": "COMPLETED",
                                                      "startedAt": "2025-12-01T18:00:00",
                                                      "completedAt": "2025-12-01T18:00:05"
                                                    }
                                                    """)
                            })
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Voto registrado",
                            content = @Content(schema = @Schema(implementation = VoteResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Validação/negócio violado")
            }
    )
    public ResponseEntity<VoteResponseDTO> registerVote(@Valid @RequestBody VoteRequestDTO requestDTO,
                                                        HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        if (!voteRateLimiter.allow(ip)) {
            return ResponseEntity.status(429).build();
        }
        VoteResponseDTO response = voteService.registerVote(requestDTO, ip, userAgent);
        URI location = URI.create(String.format("/api/votes/%d", response.getVoteId()));
        return ResponseEntity.created(location).body(response);
    }
}
