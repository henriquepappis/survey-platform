package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.PagedResponse;
import com.survey.dto.QuestionRequestDTO;
import com.survey.dto.QuestionResponseDTO;
import com.survey.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerTest {

    private MockMvc mockMvc;
    private TestQuestionService questionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        questionService = new TestQuestionService();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        QuestionController controller = new QuestionController(questionService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    private QuestionResponseDTO buildQuestionResponse(Long id, String texto, int ordem, Long surveyId) {
        QuestionResponseDTO dto = new QuestionResponseDTO();
        dto.setId(id);
        dto.setTexto(texto);
        dto.setOrdem(ordem);
        dto.setSurveyId(surveyId);
        dto.setSurveyTitulo("Survey " + surveyId);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private QuestionRequestDTO buildQuestionRequest(String texto, Integer ordem, Long surveyId) {
        return new QuestionRequestDTO(texto, ordem, surveyId);
    }

    @Test
    @DisplayName("GET /api/questions deve retornar todas as perguntas")
    void getAllQuestions_shouldReturnAll() throws Exception {
        questionService.setFindAllResult(List.of(
                buildQuestionResponse(1L, "Pergunta 1", 1, 10L),
                buildQuestionResponse(2L, "Pergunta 2", 2, 11L)
        ));

        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].texto", is("Pergunta 1")))
                .andExpect(jsonPath("$.content[1].texto", is("Pergunta 2")))
                .andExpect(jsonPath("$.totalElements", is(2)));

        assertTrue(questionService.wasFindAllCalled());
        assertFalse(questionService.wasFindBySurveyCalled());
    }

    @Test
    @DisplayName("GET /api/questions?surveyId=XX deve filtrar pelas perguntas da pesquisa")
    void getAllQuestions_withSurveyId_shouldFilter() throws Exception {
        questionService.setFindBySurveyIdResult(List.of(
                buildQuestionResponse(3L, "Pergunta filtrada", 1, 50L)
        ));

        mockMvc.perform(get("/api/questions")
                        .param("surveyId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].surveyId", is(50)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        assertTrue(questionService.wasFindBySurveyCalled());
        assertEquals(50L, questionService.getLastSurveyFilterId());
    }

    @Test
    @DisplayName("GET /api/questions/{id} deve retornar pergunta por ID")
    void getQuestionById_shouldReturnQuestion() throws Exception {
        questionService.setFindByIdResult(buildQuestionResponse(7L, "Pergunta única", 3, 100L));

        mockMvc.perform(get("/api/questions/{id}", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.texto", is("Pergunta única")));

        assertEquals(7L, questionService.getLastFindByIdId());
    }

    @Test
    @DisplayName("POST /api/questions deve criar pergunta e retornar 201")
    void createQuestion_shouldReturnCreated() throws Exception {
        QuestionRequestDTO request = buildQuestionRequest("Pergunta nova", 1, 33L);
        questionService.setCreateResult(buildQuestionResponse(20L, "Pergunta nova", 1, 33L));

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(20)))
                .andExpect(jsonPath("$.texto", is("Pergunta nova")));

        assertTrue(questionService.wasCreateCalled());
        assertEquals("Pergunta nova", questionService.getLastCreateRequest().getTexto());
    }

    @Test
    @DisplayName("POST /api/questions com payload inválido deve retornar 400")
    void createQuestion_invalid_shouldReturn400() throws Exception {
        QuestionRequestDTO invalid = buildQuestionRequest("", null, null);

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        assertFalse(questionService.wasCreateCalled());
    }

    @Test
    @DisplayName("POST /api/questions/batch deve criar perguntas em lote")
    void createQuestionsBatch_shouldReturnCreatedList() throws Exception {
        QuestionRequestDTO r1 = buildQuestionRequest("Pergunta Batch 1", 1, 77L);
        QuestionRequestDTO r2 = buildQuestionRequest("Pergunta Batch 2", 2, 77L);
        questionService.setCreateBatchResult(List.of(
                buildQuestionResponse(1L, "Pergunta Batch 1", 1, 77L),
                buildQuestionResponse(2L, "Pergunta Batch 2", 2, 77L)
        ));

        mockMvc.perform(post("/api/questions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(r1, r2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        assertEquals(2, questionService.getLastBatchRequest().size());
    }

    @Test
    @DisplayName("PUT /api/questions/{id} deve atualizar pergunta existente")
    void updateQuestion_shouldReturnUpdated() throws Exception {
        QuestionRequestDTO request = buildQuestionRequest("Atualizada", 3, 12L);
        questionService.setUpdateResult(buildQuestionResponse(5L, "Atualizada", 3, 12L));

        mockMvc.perform(put("/api/questions/{id}", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto", is("Atualizada")))
                .andExpect(jsonPath("$.ordem", is(3)));

        assertEquals(5L, questionService.getLastUpdateId());
        assertEquals(3, questionService.getLastUpdateRequest().getOrdem());
    }

    @Test
    @DisplayName("DELETE /api/questions/{id} deve retornar 204")
    void deleteQuestion_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/questions/{id}", 66))
                .andExpect(status().isNoContent());

        assertTrue(questionService.wasDeleteCalled());
        assertEquals(66L, questionService.getLastDeleteId());
    }

    static class TestQuestionService extends QuestionService {

        private List<QuestionResponseDTO> findAllResult = List.of();
        private List<QuestionResponseDTO> findBySurveyIdResult = List.of();
        private QuestionResponseDTO findByIdResult;
        private QuestionResponseDTO createResult;
        private List<QuestionResponseDTO> createBatchResult = List.of();
        private QuestionResponseDTO updateResult;

        private boolean findAllCalled;
        private boolean findBySurveyCalled;
        private Long lastSurveyFilterId;
        private Long lastFindByIdId;
        private boolean createCalled;
        private QuestionRequestDTO lastCreateRequest;
        private List<QuestionRequestDTO> lastBatchRequest;
        private Long lastUpdateId;
        private QuestionRequestDTO lastUpdateRequest;
        private boolean deleteCalled;
        private Long lastDeleteId;

        TestQuestionService() {
            super(null, null);
        }

        void setFindAllResult(List<QuestionResponseDTO> result) {
            this.findAllResult = result;
        }

        void setFindBySurveyIdResult(List<QuestionResponseDTO> result) {
            this.findBySurveyIdResult = result;
        }

        void setFindByIdResult(QuestionResponseDTO result) {
            this.findByIdResult = result;
        }

        void setCreateResult(QuestionResponseDTO result) {
            this.createResult = result;
        }

        void setCreateBatchResult(List<QuestionResponseDTO> result) {
            this.createBatchResult = result;
        }

        void setUpdateResult(QuestionResponseDTO result) {
            this.updateResult = result;
        }

        boolean wasFindAllCalled() {
            return findAllCalled;
        }

        boolean wasFindBySurveyCalled() {
            return findBySurveyCalled;
        }

        Long getLastSurveyFilterId() {
            return lastSurveyFilterId;
        }

        Long getLastFindByIdId() {
            return lastFindByIdId;
        }

        boolean wasCreateCalled() {
            return createCalled;
        }

        QuestionRequestDTO getLastCreateRequest() {
            return lastCreateRequest;
        }

        List<QuestionRequestDTO> getLastBatchRequest() {
            return lastBatchRequest;
        }

        Long getLastUpdateId() {
            return lastUpdateId;
        }

        QuestionRequestDTO getLastUpdateRequest() {
            return lastUpdateRequest;
        }

        boolean wasDeleteCalled() {
            return deleteCalled;
        }

        Long getLastDeleteId() {
            return lastDeleteId;
        }

        @Override
        public PagedResponse<QuestionResponseDTO> findAll(Pageable pageable) {
            findAllCalled = true;
            return toPaged(findAllResult, pageable);
        }

        @Override
        public PagedResponse<QuestionResponseDTO> findBySurveyId(Long surveyId, Pageable pageable) {
            findBySurveyCalled = true;
            lastSurveyFilterId = surveyId;
            return toPaged(findBySurveyIdResult, pageable);
        }

        @Override
        public QuestionResponseDTO findById(Long id) {
            lastFindByIdId = id;
            return findByIdResult;
        }

        @Override
        public QuestionResponseDTO create(QuestionRequestDTO requestDTO) {
            createCalled = true;
            lastCreateRequest = requestDTO;
            return createResult;
        }

        @Override
        public List<QuestionResponseDTO> createBatch(List<QuestionRequestDTO> requestDTOs) {
            lastBatchRequest = requestDTOs;
            return createBatchResult;
        }

        @Override
        public QuestionResponseDTO update(Long id, QuestionRequestDTO requestDTO) {
            lastUpdateId = id;
            lastUpdateRequest = requestDTO;
            return updateResult;
        }

        @Override
        public void delete(Long id) {
            deleteCalled = true;
            lastDeleteId = id;
        }

        private PagedResponse<QuestionResponseDTO> toPaged(List<QuestionResponseDTO> content, Pageable pageable) {
            int size = content.size();
            int pageNumber = pageable != null ? pageable.getPageNumber() : 0;
            int pageSize = pageable != null ? pageable.getPageSize() : (size == 0 ? 20 : size);
            int totalPages = size == 0 ? 0 : 1;
            return new PagedResponse<>(
                    content,
                    size,
                    totalPages,
                    pageNumber,
                    pageSize,
                    true,
                    true,
                    content.isEmpty()
            );
        }
    }
}
