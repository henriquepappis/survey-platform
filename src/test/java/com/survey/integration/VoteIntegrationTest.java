package com.survey.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.VoteRequestDTO;
import com.survey.dto.VoteResponseDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.ResponseSession;
import com.survey.entity.ResponseStatus;
import com.survey.entity.Survey;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.ResponseSessionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
        "app.votes.duplicate-window-minutes=5",
        "app.privacy.ip-anonymize=false"
})
class VoteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ResponseSessionRepository responseSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void cleanDatabase() {
        voteRepository.deleteAll();
        responseSessionRepository.deleteAll();
        optionRepository.deleteAll();
        questionRepository.deleteAll();
        surveyRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/votes deve persistir voto e sessão de resposta")
    void registerVote_shouldPersistVoteAndSession() throws Exception {
        Survey survey = createSurvey(true, LocalDateTime.now().plusDays(1));
        Question question = createQuestion(survey, "Pergunta 1", 1);
        Option option = createOption(question, "Opção A", true);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question.getId(),
                option.getId()
        );
        request.setCountry("BR");
        request.setDeviceType("desktop");
        request.setStatus(ResponseStatus.COMPLETED);

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit/Test")
                        .with(req -> {
                            req.setRemoteAddr("203.0.113.10");
                            return req;
                        })
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.voteId").isNumber())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.antifraudToken").isNotEmpty());

        assertThat(voteRepository.count()).isEqualTo(1);
        assertThat(responseSessionRepository.count()).isEqualTo(1);

        ResponseSession session = responseSessionRepository.findAll().get(0);
        assertThat(session.getSurvey().getId()).isEqualTo(survey.getId());
        assertThat(session.getQuestion().getId()).isEqualTo(question.getId());
        assertThat(session.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(session.getUserAgent()).isEqualTo("JUnit/Test");
        assertThat(session.getStatus()).isEqualTo(ResponseStatus.COMPLETED);
    }

    @Test
    @DisplayName("Voto deve falhar com pesquisa inativa")
    void registerVote_inactiveSurvey_shouldReturn400() throws Exception {
        Survey survey = createSurvey(false, LocalDateTime.now().plusDays(5));
        Question question = createQuestion(survey, "Pergunta 1", 1);
        Option option = createOption(question, "Opção A", true);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question.getId(),
                option.getId()
        );

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pesquisa está inativa"));
    }

    @Test
    @DisplayName("Voto deve falhar com pesquisa expirada")
    void registerVote_expiredSurvey_shouldReturn400() throws Exception {
        Survey survey = createSurvey(true, LocalDateTime.now().minusDays(1));
        Question question = createQuestion(survey, "Pergunta 1", 1);
        Option option = createOption(question, "Opção A", true);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question.getId(),
                option.getId()
        );

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pesquisa expirada"));
    }

    @Test
    @DisplayName("Voto deve falhar quando opção está inativa")
    void registerVote_inactiveOption_shouldReturn400() throws Exception {
        Survey survey = createSurvey(true, LocalDateTime.now().plusDays(3));
        Question question = createQuestion(survey, "Pergunta 1", 1);
        Option option = createOption(question, "Opção A", false);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question.getId(),
                option.getId()
        );

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Opção está inativa"));
    }

    @Test
    @DisplayName("Voto deve falhar quando opção não pertence à pergunta")
    void registerVote_optionDoesNotBelong_shouldReturn400() throws Exception {
        Survey survey = createSurvey(true, LocalDateTime.now().plusDays(4));
        Question question1 = createQuestion(survey, "Pergunta 1", 1);
        Question question2 = createQuestion(survey, "Pergunta 2", 2);
        createOption(question1, "Opção A", true);
        Option optionFromAnotherQuestion = createOption(question2, "Opção B", true);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question1.getId(),
                optionFromAnotherQuestion.getId()
        );

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Opção não pertence à pergunta"));
    }

    @Test
    @DisplayName("Voto duplicado dentro da janela deve retornar 400")
    void registerVote_duplicateWithinWindow_shouldReturn400() throws Exception {
        Survey survey = createSurvey(true, LocalDateTime.now().plusDays(2));
        Question question = createQuestion(survey, "Pergunta 1", 1);
        Option option = createOption(question, "Opção A", true);

        VoteRequestDTO request = new VoteRequestDTO(
                survey.getId(),
                question.getId(),
                option.getId()
        );

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.10");
                            req.addHeader("User-Agent", "JUnit/Test");
                            return req;
                        })
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.10");
                            req.addHeader("User-Agent", "JUnit/Test");
                            return req;
                        })
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já recebemos um voto recente deste dispositivo para esta pesquisa"));
    }

    private Survey createSurvey(boolean ativo, LocalDateTime validade) {
        Survey survey = new Survey();
        survey.setTitulo("Pesquisa Teste");
        survey.setAtivo(ativo);
        survey.setDataValidade(validade);
        return surveyRepository.save(survey);
    }

    private Question createQuestion(Survey survey, String texto, int ordem) {
        Question question = new Question();
        question.setSurvey(survey);
        question.setTexto(texto);
        question.setOrdem(ordem);
        return questionRepository.save(question);
    }

    private Option createOption(Question question, String texto, boolean ativo) {
        Option option = new Option();
        option.setQuestion(question);
        option.setTexto(texto);
        option.setAtivo(ativo);
        return optionRepository.save(option);
    }
}
