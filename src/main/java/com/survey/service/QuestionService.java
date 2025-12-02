package com.survey.service;

import com.survey.dto.PagedResponse;
import com.survey.dto.QuestionRequestDTO;
import com.survey.dto.QuestionResponseDTO;
import com.survey.entity.Question;
import com.survey.entity.Survey;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.QuestionRepository;
import com.survey.repository.ResponseSessionRepository;
import com.survey.repository.SurveyRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionService.class);

    private final QuestionRepository questionRepository;
    private final SurveyRepository surveyRepository;
    private final ResponseSessionRepository responseSessionRepository;
    private final Counter questionCreatedCounter;
    private final Counter questionUpdatedCounter;
    private final Counter questionDeletedCounter;

    public QuestionService(QuestionRepository questionRepository,
                           SurveyRepository surveyRepository,
                           ResponseSessionRepository responseSessionRepository) {
        this(questionRepository, surveyRepository, responseSessionRepository, Metrics.globalRegistry);
    }

    @Autowired
    public QuestionService(QuestionRepository questionRepository,
                           SurveyRepository surveyRepository,
                           ResponseSessionRepository responseSessionRepository,
                           MeterRegistry meterRegistry) {
        this.questionRepository = questionRepository;
        this.surveyRepository = surveyRepository;
        this.responseSessionRepository = responseSessionRepository;
        this.questionCreatedCounter = meterRegistry.counter("question.operations", "type", "create");
        this.questionUpdatedCounter = meterRegistry.counter("question.operations", "type", "update");
        this.questionDeletedCounter = meterRegistry.counter("question.operations", "type", "delete");
    }

    public PagedResponse<QuestionResponseDTO> findAll(Pageable pageable) {
        Page<Question> page = questionRepository.findAll(pageable);
        return buildPagedResponse(page, pageable);
    }

    public PagedResponse<QuestionResponseDTO> findBySurveyId(Long surveyId, Pageable pageable) {
        if (!surveyRepository.existsById(surveyId)) {
            throw new ResourceNotFoundException("Pesquisa não encontrada com id: " + surveyId);
        }

        Page<Question> page = questionRepository.findBySurveyId(surveyId, pageable);
        return buildPagedResponse(page, pageable);
    }

    public QuestionResponseDTO findById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada com id: " + id));
        return convertToDTO(question);
    }

    public QuestionResponseDTO create(QuestionRequestDTO requestDTO) {
        Survey survey = surveyRepository.findById(requestDTO.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + requestDTO.getSurveyId()));

        if (questionRepository.existsBySurveyIdAndOrdem(requestDTO.getSurveyId(), requestDTO.getOrdem())) {
            throw new BusinessException("Já existe uma pergunta com esta ordem nesta pesquisa");
        }

        Question question = convertToEntity(requestDTO, survey);
        Question savedQuestion = questionRepository.save(question);
        questionCreatedCounter.increment();
        LOGGER.info("Question created {} {}",
                StructuredArguments.kv("questionId", savedQuestion.getId()),
                StructuredArguments.kv("surveyId", savedQuestion.getSurvey().getId()));
        return convertToDTO(savedQuestion);
    }

    public QuestionResponseDTO update(Long id, QuestionRequestDTO requestDTO) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada com id: " + id));

        Survey survey = surveyRepository.findById(requestDTO.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + requestDTO.getSurveyId()));

        if (questionRepository.existsBySurveyIdAndOrdemAndIdNot(requestDTO.getSurveyId(), requestDTO.getOrdem(), id)) {
            throw new BusinessException("Já existe outra pergunta com esta ordem nesta pesquisa");
        }

        question.setTexto(requestDTO.getTexto());
        question.setOrdem(requestDTO.getOrdem());
        question.setSurvey(survey);

        Question updatedQuestion = questionRepository.save(question);
        questionUpdatedCounter.increment();
        LOGGER.info("Question updated {} {}",
                StructuredArguments.kv("questionId", updatedQuestion.getId()),
                StructuredArguments.kv("surveyId", updatedQuestion.getSurvey().getId()));
        return convertToDTO(updatedQuestion);
    }

    public List<QuestionResponseDTO> createBatch(List<QuestionRequestDTO> requestDTOs) {
        if (requestDTOs == null || requestDTOs.isEmpty()) {
            throw new BusinessException("Lista de perguntas não pode estar vazia");
        }

        // Verifica se todas as perguntas são da mesma pesquisa
        Long surveyId = requestDTOs.get(0).getSurveyId();
        if (surveyId == null) {
            throw new BusinessException("ID da pesquisa é obrigatório");
        }

        for (QuestionRequestDTO dto : requestDTOs) {
            if (!surveyId.equals(dto.getSurveyId())) {
                throw new BusinessException("Todas as perguntas devem pertencer à mesma pesquisa");
            }
        }

        // Verifica se a pesquisa existe
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesquisa não encontrada com id: " + surveyId));

        // Valida ordens únicas dentro da lista
        long uniqueOrders = requestDTOs.stream()
                .map(QuestionRequestDTO::getOrdem)
                .distinct()
                .count();

        if (uniqueOrders != requestDTOs.size()) {
            throw new BusinessException("Não é possível criar perguntas com ordens duplicadas na mesma requisição");
        }

        // Valida se alguma ordem já existe no banco para esta pesquisa
        for (QuestionRequestDTO dto : requestDTOs) {
            if (questionRepository.existsBySurveyIdAndOrdem(dto.getSurveyId(), dto.getOrdem())) {
                throw new BusinessException("Já existe uma pergunta com a ordem " + dto.getOrdem() + " nesta pesquisa");
            }
        }

        // Cria todas as perguntas
        List<Question> questions = requestDTOs.stream()
                .map(dto -> convertToEntity(dto, survey))
                .collect(Collectors.toList());

        List<Question> savedQuestions = questionRepository.saveAll(questions);

        return savedQuestions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pergunta não encontrada com id: " + id);
        }
        responseSessionRepository.deleteByQuestionId(id);
        questionRepository.deleteById(id);
        questionDeletedCounter.increment();
        LOGGER.info("Question deleted {}", StructuredArguments.kv("questionId", id));
    }

    private Question convertToEntity(QuestionRequestDTO dto, Survey survey) {
        Question question = new Question();
        question.setTexto(dto.getTexto());
        question.setOrdem(dto.getOrdem());
        question.setSurvey(survey);
        return question;
    }

    private QuestionResponseDTO convertToDTO(Question question) {
        return new QuestionResponseDTO(
                question.getId(),
                question.getTexto(),
                question.getOrdem(),
                question.getSurvey().getId(),
                question.getSurvey().getTitulo(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private PagedResponse<QuestionResponseDTO> buildPagedResponse(Page<Question> entityPage, Pageable pageable) {
        List<QuestionResponseDTO> content = entityPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Page<QuestionResponseDTO> dtoPage = new PageImpl<>(content, pageable, entityPage.getTotalElements());
        return PagedResponse.from(dtoPage);
    }
}
