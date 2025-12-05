package com.survey.controller;

import com.survey.dto.DashboardOverviewResponse;
import com.survey.dto.SurveyAudienceResponse;
import com.survey.dto.SurveyDashboardResponse;
import com.survey.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        return ResponseEntity.ok(dashboardService.getOverview());
    }

    @GetMapping("/surveys/{id}")
    @Operation(summary = "Dashboard da pesquisa",
            description = "Retorna métricas, séries temporais e estatísticas de opções para a pesquisa.",
            parameters = {
                    @Parameter(name = "from", description = "Início do intervalo ISO (ex.: 2025-12-01T00:00:00)"),
                    @Parameter(name = "to", description = "Fim do intervalo ISO (ex.: 2025-12-31T23:59:59)")
            })
    public ResponseEntity<SurveyDashboardResponse> getSurveyDashboard(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(dashboardService.getSurveyDashboard(id, from, to, includeDeleted));
    }

    @GetMapping("/surveys/{id}/audience")
    @Operation(summary = "Audiência da pesquisa",
            description = "Distribuição por device/OS/browser/origem/geo e horários de pico.",
            parameters = {
                    @Parameter(name = "from", description = "Início do intervalo ISO"),
                    @Parameter(name = "to", description = "Fim do intervalo ISO")
            })
    public ResponseEntity<SurveyAudienceResponse> getSurveyAudience(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(dashboardService.getSurveyAudience(id, from, to, includeDeleted));
    }
}
