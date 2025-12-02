package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.VoteRequestDTO;
import com.survey.dto.VoteResponseDTO;
import com.survey.service.VoteService;
import com.survey.service.VoteRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VoteControllerTest {

    private MockMvc mockMvc;
    private VoteService voteService;
    private VoteRateLimiter voteRateLimiter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        voteRateLimiter = mock(VoteRateLimiter.class);
        when(voteRateLimiter.allow(any())).thenReturn(true);
        VoteController controller = new VoteController(voteService, voteRateLimiter);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/votes deve registrar voto retornando 201")
    void registerVote_shouldReturn201() throws Exception {
        when(voteService.registerVote(any(VoteRequestDTO.class), any(), any()))
                .thenReturn(new VoteResponseDTO(10L, 20L, "antifraud-token"));

        VoteRequestDTO request = new VoteRequestDTO(1L, 2L, 3L);

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/votes/10"));
    }
}
