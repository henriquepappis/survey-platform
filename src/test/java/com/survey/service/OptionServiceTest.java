package com.survey.service;

import com.survey.dto.OptionRequestDTO;
import com.survey.dto.OptionResponseDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
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
class OptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private QuestionRepository questionRepository;

    private OptionService optionService;

    @BeforeEach
    void setUp() {
        optionService = new OptionService(optionRepository, questionRepository, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("findByQuestionId deve validar existência da pergunta")
    void findByQuestionId_whenQuestionMissing_shouldThrow() {
        when(questionRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> optionService.findByQuestionId(1L, null, PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("create deve lançar BusinessException quando exceder limite de opções ativas")
    void create_whenLimitExceeded_shouldThrow() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(buildQuestion(1L)));
        when(optionRepository.countByQuestionIdAndAtivoTrue(1L)).thenReturn(5L);
        OptionRequestDTO request = new OptionRequestDTO("Texto", true, 1L);

        assertThrows(BusinessException.class, () -> optionService.create(request));
    }

    @Test
    @DisplayName("create deve persistir opção quando dados válidos")
    void create_withValidData_shouldPersist() {
        Question question = buildQuestion(2L);
        Option saved = buildOption(10L, question, true);
        when(questionRepository.findById(2L)).thenReturn(Optional.of(question));
        when(optionRepository.countByQuestionIdAndAtivoTrue(2L)).thenReturn(0L);
        when(optionRepository.save(any(Option.class))).thenReturn(saved);

        OptionResponseDTO response =
                optionService.create(new OptionRequestDTO("Opção", true, 2L));

        assertThat(response.getId()).isEqualTo(10L);
        verify(optionRepository).save(any(Option.class));
    }

    @Test
    @DisplayName("update deve respeitar limite ao ativar opção")
    void update_whenTryingToActivateBeyondLimit_shouldThrow() {
        Question question = buildQuestion(3L);
        Option option = buildOption(5L, question, false);
        when(optionRepository.findById(5L)).thenReturn(Optional.of(option));
        when(questionRepository.findById(3L)).thenReturn(Optional.of(question));
        when(optionRepository.countByQuestionIdAndAtivoTrue(3L)).thenReturn(5L);

        OptionRequestDTO request = new OptionRequestDTO("Ativar", true, 3L);
        assertThrows(BusinessException.class, () -> optionService.update(5L, request));
    }

    @Test
    @DisplayName("createBatch deve rejeitar listas vazias ou IDs diferentes")
    void createBatch_withInvalidList_shouldThrow() {
        assertThrows(BusinessException.class, () -> optionService.createBatch(List.of()));

        OptionRequestDTO r1 = new OptionRequestDTO("A", true, 1L);
        OptionRequestDTO r2 = new OptionRequestDTO("B", true, 2L);
        assertThrows(BusinessException.class, () -> optionService.createBatch(List.of(r1, r2)));
    }

    @Test
    @DisplayName("createBatch deve respeitar limite de ativos")
    void createBatch_shouldValidateActiveLimit() {
        OptionRequestDTO r1 = new OptionRequestDTO("A", true, 9L);
        OptionRequestDTO r2 = new OptionRequestDTO("B", true, 9L);
        when(questionRepository.findById(9L)).thenReturn(Optional.of(buildQuestion(9L)));
        when(optionRepository.countByQuestionIdAndAtivoTrue(9L)).thenReturn(4L);

        assertThrows(BusinessException.class, () -> optionService.createBatch(List.of(r1, r2)));
    }

    @Test
    @DisplayName("createBatch deve salvar quando regras válidas")
    void createBatch_withValidPayload_shouldPersistAll() {
        OptionRequestDTO r1 = new OptionRequestDTO("A", true, 4L);
        OptionRequestDTO r2 = new OptionRequestDTO("B", false, 4L);
        Question question = buildQuestion(4L);
        Option o1 = buildOption(1L, question, true);
        Option o2 = buildOption(2L, question, false);

        when(questionRepository.findById(4L)).thenReturn(Optional.of(question));
        when(optionRepository.countByQuestionIdAndAtivoTrue(4L)).thenReturn(0L);
        when(optionRepository.saveAll(anyList())).thenReturn(List.of(o1, o2));

        List<OptionResponseDTO> responses = optionService.createBatch(List.of(r1, r2));
        assertThat(responses).hasSize(2);
        verify(optionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("delete deve validar existência antes de remover")
    void delete_whenMissing_shouldThrow() {
        when(optionRepository.existsById(11L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> optionService.delete(11L));
    }

    private Question buildQuestion(Long id) {
        Question question = new Question();
        question.setId(id);
        question.setTexto("Pergunta");
        question.setOrdem(1);
        question.setSurvey(buildSurvey(1L, "Pesquisa", true));
        return question;
    }

    private Option buildOption(Long id, Question question, boolean active) {
        Option option = new Option();
        option.setId(id);
        option.setTexto("Opção " + id);
        option.setAtivo(active);
        option.setQuestion(question);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        return option;
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
