package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.VoteRequestDTO;
import com.survey.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VoteControllerTest {

    private MockMvc mockMvc;
    private VoteService voteService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        voteService = mock(VoteService.class);
        VoteController controller = new VoteController(voteService);
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/votes deve registrar voto retornando 201")
    void registerVote_shouldReturn201() throws Exception {
        doNothing().when(voteService).registerVote(any(VoteRequestDTO.class), any(), any());

        VoteRequestDTO request = new VoteRequestDTO(1L, 2L, 3L);

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
