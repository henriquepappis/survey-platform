package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.VoteSummaryResponseDTO;
import com.survey.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsControllerTest {

    private MockMvc mockMvc;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        AnalyticsController controller = new AnalyticsController(analyticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/analytics/surveys/{id}/votes deve retornar resumo")
    void getSummary_shouldReturnResponse() throws Exception {
        VoteSummaryResponseDTO response = new VoteSummaryResponseDTO(1L, List.of(
                new VoteSummaryResponseDTO.QuestionVotes(10L, "Pergunta", List.of(
                        new VoteSummaryResponseDTO.OptionVotes(20L, "Opção", 5L)
                ))
        ));
        when(analyticsService.summarizeVotes(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/analytics/surveys/{surveyId}/votes", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surveyId").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].total").value(5));
    }
}
