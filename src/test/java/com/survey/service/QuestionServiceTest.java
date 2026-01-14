package com.survey.service;

import com.survey.dto.PagedResponse;
import com.survey.dto.QuestionRequestDTO;
import com.survey.dto.QuestionResponseDTO;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
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
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SurveyRepository surveyRepository;

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(questionRepository, surveyRepository, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("findBySurveyId deve validar existência da pesquisa")
    void findBySurveyId_whenSurveyMissing_shouldThrow() {
        when(surveyRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> questionService.findBySurveyId(1L, PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("findAll deve devolver conteúdo paginado")
    void findAll_shouldReturnPagedResponse() {
        Survey survey = buildSurvey(1L, "Pesquisa", true);
        Question question = buildQuestion(2L, "Pergunta", 1, survey);
        when(questionRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(question)));

        PagedResponse<QuestionResponseDTO> response = questionService.findAll(PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getSurveyId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("create deve impedir duplicidade de ordem dentro da pesquisa")
    void create_withDuplicatedOrderShouldThrow() {
        QuestionRequestDTO request = new QuestionRequestDTO("Pergunta", 1, 2L);
        when(surveyRepository.findById(2L)).thenReturn(Optional.of(buildSurvey(2L, "Pesquisa", true)));
        when(questionRepository.existsBySurveyIdAndOrdem(2L, 1)).thenReturn(true);

        assertThrows(BusinessException.class, () -> questionService.create(request));
    }

    @Test
    @DisplayName("create deve persistir pergunta quando dados válidos")
    void create_withValidPayload_shouldPersist() {
        Survey survey = buildSurvey(3L, "Pesquisa", true);
        Question saved = buildQuestion(10L, "Pergunta", 1, survey);
        when(surveyRepository.findById(3L)).thenReturn(Optional.of(survey));
        when(questionRepository.existsBySurveyIdAndOrdem(3L, 1)).thenReturn(false);
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        QuestionResponseDTO response =
                questionService.create(new QuestionRequestDTO("Pergunta", 1, 3L));

        assertThat(response.getId()).isEqualTo(10L);
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("update deve impedir ordens duplicadas em outra pergunta")
    void update_withDuplicatedOrder_shouldThrow() {
        Survey survey = buildSurvey(1L, "Pesquisa", true);
        Question question = buildQuestion(5L, "Original", 1, survey);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(questionRepository.existsBySurveyIdAndOrdemAndIdNot(1L, 2, 5L)).thenReturn(true);

        QuestionRequestDTO request = new QuestionRequestDTO("Nova", 2, 1L);
        assertThrows(BusinessException.class, () -> questionService.update(5L, request));
    }

    @Test
    @DisplayName("createBatch deve validar surveyId e ordens únicas")
    void createBatch_shouldValidateInput() {
        List<QuestionRequestDTO> empty = List.of();
        assertThrows(BusinessException.class, () -> questionService.createBatch(empty));

        QuestionRequestDTO req1 = new QuestionRequestDTO("Q1", 1, 4L);
        QuestionRequestDTO req2 = new QuestionRequestDTO("Q2", 2, 5L);
        assertThrows(BusinessException.class, () -> questionService.createBatch(List.of(req1, req2)));
    }

    @Test
    @DisplayName("createBatch deve lançar quando ordens repetidas")
    void createBatch_withDuplicatedOrders_shouldThrow() {
        QuestionRequestDTO req1 = new QuestionRequestDTO("Q1", 1, 4L);
        QuestionRequestDTO req2 = new QuestionRequestDTO("Q2", 1, 4L);
        when(surveyRepository.findById(4L)).thenReturn(Optional.of(buildSurvey(4L, "Pesquisa", true)));

        assertThrows(BusinessException.class, () -> questionService.createBatch(List.of(req1, req2)));
    }

    @Test
    @DisplayName("delete deve lançar quando pergunta não existe")
    void delete_whenMissing_shouldThrow() {
        // QuestionService.delete usa findById (não existsById)
        when(questionRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> questionService.delete(123L));
    }

    @Test
    @DisplayName("delete deve marcar deletedAt no soft delete")
    void delete_shouldSoftDelete() {
        Question question = buildQuestion(10L, "Q", 1, buildSurvey(1L, "S", true));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        questionService.delete(10L);

        verify(questionRepository).save(argThat(q -> q.getDeletedAt() != null));
    }

    private Survey buildSurvey(Long id, String title, boolean active) {
        Survey survey = new Survey();
        survey.setId(id);
        survey.setTitulo(title);
        survey.setAtivo(active);
        survey.setDataValidade(LocalDateTime.now());
        return survey;
    }

    private Question buildQuestion(Long id, String text, int order, Survey survey) {
        Question question = new Question();
        question.setId(id);
        question.setTexto(text);
        question.setOrdem(order);
        question.setSurvey(survey);
        return question;
    }
}
