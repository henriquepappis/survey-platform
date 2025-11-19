package com.survey.service;

import com.survey.dto.PagedResponse;
import com.survey.dto.SurveyDetailsResponseDTO;
import com.survey.dto.SurveyRequestDTO;
import com.survey.dto.SurveyResponseDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
import com.survey.repository.SurveyRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private OptionRepository optionRepository;

    private SurveyService surveyService;

    @BeforeEach
    void setUp() {
        surveyService = new SurveyService(
                surveyRepository,
                questionRepository,
                optionRepository,
                new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("findAll deve converter entidades para DTOs mantendo metadados da página")
    void findAll_shouldReturnPagedResponse() {
        Survey survey = buildSurvey(1L, "Pesquisa 1", true);
        Page<Survey> page = new PageImpl<>(List.of(survey), PageRequest.of(0, 10), 1);
        when(surveyRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedResponse<SurveyResponseDTO> result = surveyService.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitulo()).isEqualTo("Pesquisa 1");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllAtivas deve delegar para repository específico")
    void findAllAtivas_shouldDelegateToRepository() {
        Survey survey = buildSurvey(2L, "Ativa", true);
        when(surveyRepository.findByAtivoTrue(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(survey)));

        PagedResponse<SurveyResponseDTO> result =
                surveyService.findAllAtivas(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("create deve lançar BusinessException quando título já existe")
    void create_withDuplicatedTitle_shouldThrowBusinessException() {
        SurveyRequestDTO request = new SurveyRequestDTO("Duplicada", true, null);
        when(surveyRepository.existsByTitulo("Duplicada")).thenReturn(true);

        assertThrows(BusinessException.class, () -> surveyService.create(request));
    }

    @Test
    @DisplayName("create deve salvar pesquisa quando dados válidos")
    void create_withValidData_shouldPersistSurvey() {
        SurveyRequestDTO request = new SurveyRequestDTO("Nova", true, LocalDateTime.now());
        Survey saved = buildSurvey(10L, "Nova", true);
        when(surveyRepository.existsByTitulo("Nova")).thenReturn(false);
        when(surveyRepository.save(any(Survey.class))).thenReturn(saved);

        SurveyResponseDTO response = surveyService.create(request);

        assertThat(response.getId()).isEqualTo(10L);
        verify(surveyRepository).save(any(Survey.class));
    }

    @Test
    @DisplayName("update deve atualizar pesquisa existente")
    void update_shouldPersistChanges() {
        Survey existing = buildSurvey(5L, "Atual", false);
        SurveyRequestDTO request = new SurveyRequestDTO("Atualizada", true, null);
        when(surveyRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(surveyRepository.existsByTituloAndIdNot("Atualizada", 5L)).thenReturn(false);
        when(surveyRepository.save(existing)).thenReturn(existing);

        SurveyResponseDTO response = surveyService.update(5L, request);

        assertThat(response.getTitulo()).isEqualTo("Atualizada");
        assertThat(existing.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("update deve lançar ResourceNotFound quando id inexistente")
    void update_whenNotFound_shouldThrow() {
        when(surveyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> surveyService.update(99L, new SurveyRequestDTO("t", true, null)));
    }

    @Test
    @DisplayName("delete deve lançar ResourceNotFound quando id não existe")
    void delete_whenSurveyMissing_shouldThrow() {
        when(surveyRepository.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> surveyService.delete(7L));
    }

    @Test
    @DisplayName("getSurveyStructure deve montar perguntas e opções filtrando ativas por padrão")
    void getSurveyStructure_shouldAssembleTree() {
        Survey survey = buildSurvey(1L, "Pesquisa", true);
        Question question = new Question();
        question.setId(10L);
        question.setTexto("Pergunta 1");
        question.setOrdem(1);
        question.setSurvey(survey);
        Option option = new Option();
        option.setId(100L);
        option.setTexto("Sim");
        option.setAtivo(true);
        option.setQuestion(question);

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(questionRepository.findBySurveyIdOrderByOrdemAsc(1L))
                .thenReturn(List.of(question));
        when(optionRepository.findByQuestionIdInAndAtivoTrue(List.of(10L)))
                .thenReturn(List.of(option));

        SurveyDetailsResponseDTO response = surveyService.getSurveyStructure(1L, false);

        assertThat(response.getQuestions()).hasSize(1);
        assertThat(response.getQuestions().get(0).getOptions()).hasSize(1);
        verify(optionRepository).findByQuestionIdInAndAtivoTrue(List.of(10L));
        verify(optionRepository, never()).findByQuestionIdIn(anyList());
    }

    private Survey buildSurvey(Long id, String titulo, boolean ativo) {
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitulo(titulo);
        survey.setAtivo(ativo);
        survey.setDataValidade(LocalDateTime.now());
        return survey;
    }
}
