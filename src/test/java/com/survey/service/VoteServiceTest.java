package com.survey.service;

import com.survey.dto.VoteRequestDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.ResponseSession;
import com.survey.entity.Survey;
import com.survey.entity.Vote;
import com.survey.exception.BusinessException;
import com.survey.repository.ResponseSessionRepository;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import com.survey.repository.VoteRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private OptionRepository optionRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ResponseSessionRepository responseSessionRepository;

    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(
                surveyRepository,
                questionRepository,
                optionRepository,
                voteRepository,
                responseSessionRepository,
                0L, // janela desabilitada para testes unitários
                true,
                true,
                new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("registerVote deve validar relacionamentos pertencentes")
    void registerVote_shouldValidateRelationships() {
        Survey survey = buildSurvey(1L, true);
        Question question = buildQuestion(2L, survey);
        Option option = buildOption(3L, question);

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(question));
        when(optionRepository.findById(3L)).thenReturn(Optional.of(option));
        when(responseSessionRepository.save(any())).thenAnswer(invocation -> {
            ResponseSession saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            return saved;
        });
        when(voteRepository.save(any())).thenAnswer(invocation -> {
            Vote saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        VoteRequestDTO request = new VoteRequestDTO(1L, 2L, 3L);

        voteService.registerVote(request, "127.0.0.1", "JUnit");

        verify(responseSessionRepository).save(any());
        verify(voteRepository).save(any());
    }

    @Test
    @DisplayName("registerVote deve lançar quando pesquisa estiver inativa")
    void registerVote_whenSurveyInactive_shouldThrow() {
        Survey survey = buildSurvey(1L, false);
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));

        VoteRequestDTO request = new VoteRequestDTO(1L, 2L, 3L);

        assertThrows(BusinessException.class,
                () -> voteService.registerVote(request, "127.0.0.1", "UA"));
    }

    private Survey buildSurvey(Long id, boolean active) {
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitulo("Pesquisa");
        survey.setAtivo(active);
        survey.setDataValidade(LocalDateTime.now().plusDays(1));
        return survey;
    }

    private Question buildQuestion(Long id, Survey survey) {
        Question question = new Question();
        question.setId(id);
        question.setTexto("Pergunta");
        question.setSurvey(survey);
        return question;
    }

    private Option buildOption(Long id, Question question) {
        Option option = new Option();
        option.setId(id);
        option.setTexto("Opção");
        option.setQuestion(question);
        option.setAtivo(true);
        return option;
    }
}
