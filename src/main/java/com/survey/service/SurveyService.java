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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SurveyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyService.class);

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final Counter surveyCreatedCounter;
    private final Counter surveyUpdatedCounter;
    private final Counter surveyDeletedCounter;

    @Autowired
    public SurveyService(SurveyRepository surveyRepository,
                         QuestionRepository questionRepository,
                         OptionRepository optionRepository,
                         MeterRegistry meterRegistry) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.surveyCreatedCounter = meterRegistry.counter("survey.operations", "type", "create");
        this.surveyUpdatedCounter = meterRegistry.counter("survey.operations", "type", "update");
        this.surveyDeletedCounter = meterRegistry.counter("survey.operations", "type", "delete");
    }

    public PagedResponse<SurveyResponseDTO> findAll(Pageable pageable) {
        Page<Survey> page = surveyRepository.findAll(pageable);
        return buildPagedResponse(page, pageable);
    }

    public PagedResponse<SurveyResponseDTO> findAllIncludingDeleted(Pageable pageable) {
        Page<Survey> page = surveyRepository.findAllIncludingDeleted(pageable);
        return buildPagedResponse(page, pageable);
    }

    public PagedResponse<SurveyResponseDTO> findAllAtivas(Pageable pageable) {
        Page<Survey> page = surveyRepository.findByAtivoTrue(pageable);
        return buildPagedResponse(page, pageable);
    }

    public SurveyResponseDTO findById(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id));
        return convertToDTO(survey);
    }

    public SurveyDetailsResponseDTO getSurveyStructure(Long id, boolean includeInactiveOptions, boolean includeDeleted) {
        Survey survey = includeDeleted
                ? surveyRepository.findByIdIncludingDeleted(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id))
                : surveyRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id));

        List<Question> questions = includeDeleted
                ? questionRepository.findBySurveyIdIncludingDeleted(id)
                : questionRepository.findBySurveyIdOrderByOrdemAsc(id);
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();

        Map<Long, List<Option>> optionsGrouped = questionIds.isEmpty()
                ? Collections.emptyMap()
                : (includeDeleted
                    ? optionRepository.findByQuestionIdInIncludingDeleted(questionIds)
                    : (includeInactiveOptions
                        ? optionRepository.findByQuestionIdIn(questionIds)
                        : optionRepository.findByQuestionIdInAndAtivoTrue(questionIds)))
                .stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

        List<SurveyDetailsResponseDTO.QuestionDetails> questionDetails = questions.stream()
                .map(question -> new SurveyDetailsResponseDTO.QuestionDetails(
                        question.getId(),
                        question.getTexto(),
                        question.getOrdem(),
                        optionsGrouped.getOrDefault(question.getId(), Collections.emptyList()).stream()
                                .sorted(Comparator.comparing(Option::getId))
                                .map(option -> new SurveyDetailsResponseDTO.OptionDetails(
                                        option.getId(),
                                        option.getTexto(),
                                        option.getAtivo()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new SurveyDetailsResponseDTO(
                survey.getId(),
                survey.getTitulo(),
                survey.getDescricao(),
                survey.getAtivo(),
                survey.getDataValidade(),
                survey.getCreatedAt(),
                survey.getUpdatedAt(),
                questionDetails
        );
    }

    public SurveyResponseDTO create(SurveyRequestDTO requestDTO) {
        if (surveyRepository.existsByTitulo(requestDTO.getTitulo())) {
            throw new BusinessException("Já existe uma pesquisa com este título");
        }

        Survey survey = convertToEntity(requestDTO);
        Survey savedSurvey = surveyRepository.save(survey);
        surveyCreatedCounter.increment();
        LOGGER.info("Survey created {} {}",
                StructuredArguments.kv("surveyId", savedSurvey.getId()),
                StructuredArguments.kv("title", savedSurvey.getTitulo()));
        return convertToDTO(savedSurvey);
    }

    public SurveyResponseDTO update(Long id, SurveyRequestDTO requestDTO) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id));

        if (surveyRepository.existsByTituloAndIdNot(requestDTO.getTitulo(), id)) {
            throw new BusinessException("Já existe outra pesquisa com este título");
        }

        survey.setTitulo(requestDTO.getTitulo());
        survey.setDescricao(requestDTO.getDescricao());
        survey.setAtivo(requestDTO.getAtivo());
        survey.setDataValidade(requestDTO.getDataValidade());

        Survey updatedSurvey = surveyRepository.save(survey);
        surveyUpdatedCounter.increment();
        LOGGER.info("Survey updated {} {}",
                StructuredArguments.kv("surveyId", updatedSurvey.getId()),
                StructuredArguments.kv("title", updatedSurvey.getTitulo()));
        return convertToDTO(updatedSurvey);
    }

    public List<SurveyResponseDTO> createBatch(List<SurveyRequestDTO> requestDTOs) {
        if (requestDTOs == null || requestDTOs.isEmpty()) {
            throw new BusinessException("Lista de pesquisas não pode estar vazia");
        }

        // Valida títulos únicos dentro da lista
        long uniqueTitles = requestDTOs.stream()
                .map(SurveyRequestDTO::getTitulo)
                .distinct()
                .count();

        if (uniqueTitles != requestDTOs.size()) {
            throw new BusinessException("Não é possível criar pesquisas com títulos duplicados na mesma requisição");
        }

        // Valida se algum título já existe no banco
        for (SurveyRequestDTO dto : requestDTOs) {
            if (surveyRepository.existsByTitulo(dto.getTitulo())) {
                throw new BusinessException("Já existe uma pesquisa com o título: " + dto.getTitulo());
            }
        }

        // Cria todas as pesquisas
        List<Survey> surveys = requestDTOs.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());

        List<Survey> savedSurveys = surveyRepository.saveAll(surveys);

        return savedSurveys.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id));

        LocalDateTime now = LocalDateTime.now();
        survey.setAtivo(false);
        survey.setDeletedAt(now);
        surveyRepository.save(survey);

        List<Question> questions = questionRepository.findBySurveyIdOrderByOrdemAsc(id);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();

        List<Option> options = questionIds.isEmpty()
                ? List.of()
                : optionRepository.findByQuestionIdIn(questionIds);

        options.forEach(option -> {
            option.setAtivo(false);
            option.setDeletedAt(now);
        });
        if (!options.isEmpty()) {
            optionRepository.saveAll(options);
        }

        questions.forEach(question -> question.setDeletedAt(now));
        if (!questions.isEmpty()) {
            questionRepository.saveAll(questions);
        }

        surveyDeletedCounter.increment();
        LOGGER.info("Survey soft-deleted {}", StructuredArguments.kv("surveyId", id));
    }

    public SurveyResponseDTO restore(Long id) {
        Survey survey = surveyRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + id));

        LocalDateTime now = LocalDateTime.now();
        survey.setDeletedAt(null);
        survey.setAtivo(true);
        survey.setUpdatedAt(now);

        List<Question> questions = questionRepository.findBySurveyIdIncludingDeleted(id);
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        List<Option> options = questionIds.isEmpty()
                ? List.of()
                : optionRepository.findByQuestionIdInIncludingDeleted(questionIds);

        options.forEach(option -> {
            option.setDeletedAt(null);
            option.setAtivo(true);
        });
        if (!options.isEmpty()) {
            optionRepository.saveAll(options);
        }

        questions.forEach(question -> question.setDeletedAt(null));
        if (!questions.isEmpty()) {
            questionRepository.saveAll(questions);
        }

        Survey restored = surveyRepository.save(survey);
        return convertToDTO(restored);
    }

    private Survey convertToEntity(SurveyRequestDTO dto) {
        Survey survey = new Survey();
        survey.setTitulo(dto.getTitulo());
        survey.setDescricao(dto.getDescricao());
        survey.setAtivo(dto.getAtivo());
        survey.setDataValidade(dto.getDataValidade());
        return survey;
    }

    private SurveyResponseDTO convertToDTO(Survey survey) {
        return new SurveyResponseDTO(
                survey.getId(),
                survey.getTitulo(),
                survey.getDescricao(),
                survey.getAtivo(),
                survey.getDataValidade(),
                survey.getCreatedAt(),
                survey.getUpdatedAt(),
                survey.getDeletedAt()
        );
    }

    private PagedResponse<SurveyResponseDTO> buildPagedResponse(Page<Survey> entityPage, Pageable pageable) {
        List<SurveyResponseDTO> content = entityPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Page<SurveyResponseDTO> dtoPage = new PageImpl<>(
                content,
                pageable,
                entityPage.getTotalElements()
        );
        return PagedResponse.from(dtoPage);
    }
}
