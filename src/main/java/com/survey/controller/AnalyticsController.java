package com.survey.controller;

import com.survey.dto.VoteSummaryResponseDTO;
import com.survey.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/surveys/{surveyId}/votes")
    public ResponseEntity<VoteSummaryResponseDTO> getVoteSummary(@PathVariable Long surveyId) {
        return ResponseEntity.ok(analyticsService.summarizeVotes(surveyId));
    }
}
