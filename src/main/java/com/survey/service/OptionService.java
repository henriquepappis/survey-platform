package com.survey.service;

import com.survey.dto.OptionRequestDTO;
import com.survey.dto.PagedResponse;
import com.survey.dto.OptionResponseDTO;
import com.survey.entity.Option;
import com.survey.entity.Question;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.OptionRepository;
import com.survey.repository.QuestionRepository;
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
public class OptionService {

    private static final int MAX_ACTIVE_OPTIONS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionService.class);

    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final Counter optionCreatedCounter;
    private final Counter optionUpdatedCounter;
    private final Counter optionDeletedCounter;

    public OptionService(OptionRepository optionRepository, QuestionRepository questionRepository) {
        this(optionRepository, questionRepository, Metrics.globalRegistry);
    }

    @Autowired
    public OptionService(OptionRepository optionRepository,
                         QuestionRepository questionRepository,
                         MeterRegistry meterRegistry) {
        this.optionRepository = optionRepository;
        this.questionRepository = questionRepository;
        this.optionCreatedCounter = meterRegistry.counter("option.operations", "type", "create");
        this.optionUpdatedCounter = meterRegistry.counter("option.operations", "type", "update");
        this.optionDeletedCounter = meterRegistry.counter("option.operations", "type", "delete");
    }

    public PagedResponse<OptionResponseDTO> findAll(Pageable pageable) {
        Page<Option> page = optionRepository.findAll(pageable);
        return buildPagedResponse(page, pageable);
    }

    public PagedResponse<OptionResponseDTO> findByQuestionId(Long questionId, Boolean ativo, Pageable pageable) {
        if (!questionRepository.existsById(questionId)) {
            throw new ResourceNotFoundException("Pergunta não encontrada com id: " + questionId);
        }

        Page<Option> options = (ativo != null && ativo)
                ? optionRepository.findByQuestionIdAndAtivoTrue(questionId, pageable)
                : optionRepository.findByQuestionId(questionId, pageable);

        return buildPagedResponse(options, pageable);
    }

    public OptionResponseDTO findById(Long id) {
        Option option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada com id: " + id));
        return convertToDTO(option);
    }

    public OptionResponseDTO create(OptionRequestDTO requestDTO) {
        Question question = questionRepository.findById(requestDTO.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada com id: " + requestDTO.getQuestionId()));

        // Validação: máximo 5 opções ativas por pergunta
        if (requestDTO.getAtivo() && optionRepository.countByQuestionIdAndAtivoTrue(requestDTO.getQuestionId()) >= MAX_ACTIVE_OPTIONS) {
            throw new BusinessException("Não é possível ter mais de " + MAX_ACTIVE_OPTIONS + " opções ativas por pergunta");
        }

        Option option = convertToEntity(requestDTO, question);
        Option savedOption = optionRepository.save(option);
        optionCreatedCounter.increment();
        LOGGER.info("Option created {} {}",
                StructuredArguments.kv("optionId", savedOption.getId()),
                StructuredArguments.kv("questionId", savedOption.getQuestion().getId()));
        return convertToDTO(savedOption);
    }

    public OptionResponseDTO update(Long id, OptionRequestDTO requestDTO) {
        Option option = optionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opção não encontrada com id: " + id));

        Question question = questionRepository.findById(requestDTO.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada com id: " + requestDTO.getQuestionId()));

        // Validação: máximo 5 opções ativas por pergunta
        // Se está tentando ativar uma opção que estava inativa, verifica o limite
        if (requestDTO.getAtivo() && !option.getAtivo()) {
            long activeCount = optionRepository.countByQuestionIdAndAtivoTrue(requestDTO.getQuestionId());
            if (activeCount >= MAX_ACTIVE_OPTIONS) {
                throw new BusinessException("Não é possível ter mais de " + MAX_ACTIVE_OPTIONS + " opções ativas por pergunta");
            }
        }

        option.setTexto(requestDTO.getTexto());
        option.setAtivo(requestDTO.getAtivo());
        option.setQuestion(question);

        Option updatedOption = optionRepository.save(option);
        optionUpdatedCounter.increment();
        LOGGER.info("Option updated {} {}",
                StructuredArguments.kv("optionId", updatedOption.getId()),
                StructuredArguments.kv("questionId", updatedOption.getQuestion().getId()));
        return convertToDTO(updatedOption);
    }

    public List<OptionResponseDTO> createBatch(List<OptionRequestDTO> requestDTOs) {
        if (requestDTOs == null || requestDTOs.isEmpty()) {
            throw new BusinessException("Lista de opções não pode estar vazia");
        }

        // Verifica se todas as opções são da mesma pergunta
        Long questionId = requestDTOs.get(0).getQuestionId();
        if (questionId == null) {
            throw new BusinessException("ID da pergunta é obrigatório");
        }

        for (OptionRequestDTO dto : requestDTOs) {
            if (!questionId.equals(dto.getQuestionId())) {
                throw new BusinessException("Todas as opções devem pertencer à mesma pergunta");
            }
        }

        // Verifica se a pergunta existe
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pergunta não encontrada com id: " + questionId));

        // Conta quantas opções ativas já existem
        long existingActiveCount = optionRepository.countByQuestionIdAndAtivoTrue(questionId);

        // Conta quantas opções ativas estão sendo criadas
        long newActiveCount = requestDTOs.stream()
                .filter(OptionRequestDTO::getAtivo)
                .count();

        // Validação: máximo 5 opções ativas por pergunta
        if (existingActiveCount + newActiveCount > MAX_ACTIVE_OPTIONS) {
            throw new BusinessException(
                    String.format("Não é possível criar %d opções ativas. Já existem %d opções ativas. Máximo permitido: %d",
                            newActiveCount, existingActiveCount, MAX_ACTIVE_OPTIONS)
            );
        }

        // Cria todas as opções
        List<Option> options = requestDTOs.stream()
                .map(dto -> convertToEntity(dto, question))
                .collect(Collectors.toList());

        List<Option> savedOptions = optionRepository.saveAll(options);

        return savedOptions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!optionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Opção não encontrada com id: " + id);
        }
        optionRepository.deleteById(id);
        optionDeletedCounter.increment();
        LOGGER.info("Option deleted {}", StructuredArguments.kv("optionId", id));
    }

    private Option convertToEntity(OptionRequestDTO dto, Question question) {
        Option option = new Option();
        option.setTexto(dto.getTexto());
        option.setAtivo(dto.getAtivo());
        option.setQuestion(question);
        return option;
    }

    private OptionResponseDTO convertToDTO(Option option) {
        return new OptionResponseDTO(
                option.getId(),
                option.getTexto(),
                option.getAtivo(),
                option.getQuestion().getId(),
                option.getQuestion().getTexto(),
                option.getCreatedAt(),
                option.getUpdatedAt()
        );
    }

    private PagedResponse<OptionResponseDTO> buildPagedResponse(Page<Option> entityPage, Pageable pageable) {
        List<OptionResponseDTO> content = entityPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Page<OptionResponseDTO> dtoPage = new PageImpl<>(content, pageable, entityPage.getTotalElements());
        return PagedResponse.from(dtoPage);
    }
}
