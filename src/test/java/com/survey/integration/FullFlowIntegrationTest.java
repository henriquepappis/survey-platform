package com.survey.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.QuestionRequestDTO;
import com.survey.dto.SurveyRequestDTO;
import com.survey.dto.VoteRequestDTO;
import com.survey.entity.UserAccount;
import com.survey.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.votes.duplicate-window-minutes=5"
})
class FullFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void resetData() {
        userRepository.deleteAll();
        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        userRepository.save(admin);
    }

    @Test
    @DisplayName("Fluxo completo: login -> criar survey -> criar question/option -> votar -> analytics/dashboard")
    void fullFlow_shouldWork() throws Exception {
        String token = loginAndGetToken();

        // Cria survey
        SurveyRequestDTO surveyRequest = new SurveyRequestDTO(
                "Pesquisa Flow",
                true,
                LocalDateTime.now().plusDays(10)
        );
        String surveyResponse = mockMvc.perform(post("/api/surveys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(surveyRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long surveyId = objectMapper.readTree(surveyResponse).get("id").asLong();

        // Cria question
        QuestionRequestDTO questionRequest = new QuestionRequestDTO("Pergunta Flow", 1, surveyId);
        String questionResponse = mockMvc.perform(post("/api/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long questionId = objectMapper.readTree(questionResponse).get("id").asLong();

        // Cria option
        String optionPayload = """
                {"texto":"Opção Flow","ativo":true,"questionId":%d}
                """.formatted(questionId);
        String optionResponse = mockMvc.perform(post("/api/options")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(optionPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long optionId = objectMapper.readTree(optionResponse).get("id").asLong();

        // Vote
        VoteRequestDTO voteRequest = new VoteRequestDTO(surveyId, questionId, optionId);
        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated());

        // Analytics (requires auth)
        String analyticsResponse = mockMvc.perform(get("/api/analytics/surveys/{id}/votes", surveyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode analytics = objectMapper.readTree(analyticsResponse);
        assertThat(analytics.get("surveyId").asLong()).isEqualTo(surveyId);
        assertThat(analytics.get("questions").get(0).get("options").get(0).get("total").asLong()).isEqualTo(1L);

        // Dashboard overview (ADMIN)
        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.totalSurveys").isNumber());
    }

    private String loginAndGetToken() throws Exception {
        String loginPayload = """
                {"username":"admin","password":"admin123"}
                """;
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("accessToken").asText();
    }
}
