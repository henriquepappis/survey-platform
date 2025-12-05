package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.PagedResponse;
import com.survey.dto.SurveyDetailsResponseDTO;
import com.survey.dto.SurveyRequestDTO;
import com.survey.dto.SurveyResponseDTO;
import com.survey.service.SurveyService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de camada web para o SurveyController.
 *
 * Foca em:
 * - status HTTP
 * - JSON retornado
 * - chamada correta aos métodos do SurveyService
 */
class SurveyControllerTest {

    private MockMvc mockMvc;
    private TestSurveyService surveyService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        surveyService = new TestSurveyService();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        SurveyController controller = new SurveyController(surveyService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    // ------- helpers -------

    private SurveyResponseDTO buildSurveyResponse(Long id,
            String titulo,
            Boolean ativo,
            LocalDateTime dataValidade) {
        SurveyResponseDTO dto = new SurveyResponseDTO();
        dto.setId(id);
        dto.setTitulo(titulo);
        dto.setDescricao("Descricao");
        dto.setAtivo(ativo);
        dto.setDataValidade(dataValidade);
        if (dataValidade != null) {
            dto.setCreatedAt(dataValidade.minusDays(30));
            dto.setUpdatedAt(dataValidade.minusDays(1));
        }
        return dto;
    }

    private SurveyRequestDTO buildSurveyRequest(String titulo,
            Boolean ativo,
            LocalDateTime dataValidade) {
        return new SurveyRequestDTO(titulo, "Descricao", ativo, dataValidade);
    }

    // ------- testes -------

    @Test
    @DisplayName("GET /api/surveys deve retornar lista completa quando não há filtro 'ativo'")
    void getAllSurveys_shouldReturnAll() throws Exception {
        SurveyResponseDTO s1 = buildSurveyResponse(
                1L, "Pesquisa 1", true,
                LocalDateTime.of(2025, 12, 31, 23, 59, 59));
        SurveyResponseDTO s2 = buildSurveyResponse(
                2L, "Pesquisa 2", false,
                LocalDateTime.of(2026, 1, 1, 0, 0));

        surveyService.setFindAllResult(List.of(s1, s2));

        mockMvc.perform(get("/api/surveys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].titulo", is("Pesquisa 1")))
                .andExpect(jsonPath("$.content[0].ativo", is(true)))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].titulo", is("Pesquisa 2")))
                .andExpect(jsonPath("$.content[1].ativo", is(false)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.page", is(0)));

        assertTrue(surveyService.wasFindAllCalled());
        assertFalse(surveyService.wasFindAllAtivasCalled());
    }

    @Test
    @DisplayName("GET /api/surveys?ativo=true deve chamar findAllAtivas e retornar somente ativas")
    void getAllSurveys_withAtivoTrue_shouldReturnOnlyActive() throws Exception {
        SurveyResponseDTO s1 = buildSurveyResponse(
                1L, "Pesquisa Ativa 1", true,
                LocalDateTime.now().plusDays(10));

        surveyService.setFindAllAtivasResult(List.of(s1));

        mockMvc.perform(get("/api/surveys")
                .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ativo", is(true)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        assertTrue(surveyService.wasFindAllAtivasCalled());
        assertFalse(surveyService.wasFindAllCalled());
    }

    @Test
    @DisplayName("GET /api/surveys/{id}/structure deve retornar árvore completa da pesquisa")
    void getSurveyStructure_shouldReturnTree() throws Exception {
        SurveyDetailsResponseDTO.OptionDetails option =
                new SurveyDetailsResponseDTO.OptionDetails(50L, "Sim", true);
        SurveyDetailsResponseDTO.QuestionDetails question =
                new SurveyDetailsResponseDTO.QuestionDetails(10L, "Você recomenda?", 1, List.of(option));
        SurveyDetailsResponseDTO structure = new SurveyDetailsResponseDTO(
                1L,
                "Pesquisa NPS",
                "Desc",
                true,
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                List.of(question));
        surveyService.setStructureResult(structure);

        mockMvc.perform(get("/api/surveys/1/structure")
                .param("includeInactiveOptions", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].options", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].options[0].texto", is("Sim")));

        assertEquals(1L, surveyService.getLastStructureSurveyId());
        assertTrue(surveyService.wasStructureCalledWithInactive());
    }

    @Test
    @DisplayName("GET /api/surveys/{id} deve retornar a pesquisa por ID")
    void getSurveyById_shouldReturnSurvey() throws Exception {
        Long id = 1L;
        SurveyResponseDTO s1 = buildSurveyResponse(
                id, "Pesquisa por ID", true,
                LocalDateTime.of(2025, 12, 31, 23, 59, 59));

        surveyService.setFindByIdResult(s1);

        mockMvc.perform(get("/api/surveys/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.titulo", is("Pesquisa por ID")))
                .andExpect(jsonPath("$.ativo", is(true)));

        assertEquals(id, surveyService.getLastFindByIdId());
    }

    @Test
    @DisplayName("POST /api/surveys deve criar pesquisa e retornar 201 com corpo")
    void createSurvey_shouldReturn201AndBody() throws Exception {
        SurveyRequestDTO request = buildSurveyRequest(
                "Pesquisa Criada",
                true,
                LocalDateTime.of(2025, 12, 31, 23, 59, 59));

        SurveyResponseDTO response = buildSurveyResponse(
                10L, "Pesquisa Criada", true,
                request.getDataValidade());

        surveyService.setCreateResult(response);

        mockMvc.perform(post("/api/surveys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.titulo", is("Pesquisa Criada")))
                .andExpect(jsonPath("$.ativo", is(true)));

        assertTrue(surveyService.wasCreateCalled());
        assertEquals("Pesquisa Criada", surveyService.getLastCreateRequest().getTitulo());
    }

    @Test
    @DisplayName("POST /api/surveys com dados inválidos deve retornar 400 (validação Bean Validation)")
    void createSurvey_invalidData_shouldReturn400() throws Exception {
        // Título em branco e ativo null pra acionar @NotBlank e @NotNull
        SurveyRequestDTO request = buildSurveyRequest(
                "",
                null,
                null);

        mockMvc.perform(post("/api/surveys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertFalse(surveyService.wasCreateCalled());
    }

    @Test
    @DisplayName("POST /api/surveys/batch deve criar pesquisas em lote e retornar 201")
    void createSurveysBatch_shouldReturn201() throws Exception {
        SurveyRequestDTO r1 = buildSurveyRequest(
                "Pesquisa Lote 1", true, LocalDateTime.now().plusDays(30));
        SurveyRequestDTO r2 = buildSurveyRequest(
                "Pesquisa Lote 2", false, LocalDateTime.now().plusDays(60));

        SurveyResponseDTO s1 = buildSurveyResponse(
                100L, "Pesquisa Lote 1", true, r1.getDataValidade());
        SurveyResponseDTO s2 = buildSurveyResponse(
                101L, "Pesquisa Lote 2", false, r2.getDataValidade());

        surveyService.setCreateBatchResult(List.of(s1, s2));

        mockMvc.perform(post("/api/surveys/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(r1, r2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(100)))
                .andExpect(jsonPath("$[0].titulo", is("Pesquisa Lote 1")))
                .andExpect(jsonPath("$[0].ativo", is(true)))
                .andExpect(jsonPath("$[1].id", is(101)))
                .andExpect(jsonPath("$[1].titulo", is("Pesquisa Lote 2")))
                .andExpect(jsonPath("$[1].ativo", is(false)));

        assertEquals(2, surveyService.getLastBatchRequest().size());
    }

    @Test
    @DisplayName("PUT /api/surveys/{id} deve atualizar pesquisa existente")
    void updateSurvey_shouldReturnUpdatedSurvey() throws Exception {
        Long id = 20L;
        SurveyRequestDTO request = buildSurveyRequest(
                "Pesquisa Atualizada", false, LocalDateTime.now().plusDays(5));

        SurveyResponseDTO response = buildSurveyResponse(
                id, "Pesquisa Atualizada", false, request.getDataValidade());

        surveyService.setUpdateResult(response);

        mockMvc.perform(put("/api/surveys/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.titulo", is("Pesquisa Atualizada")))
                .andExpect(jsonPath("$.ativo", is(false)));

        assertEquals(id, surveyService.getLastUpdateId());
        assertEquals("Pesquisa Atualizada", surveyService.getLastUpdateRequest().getTitulo());
    }

    @Test
    @DisplayName("DELETE /api/surveys/{id} deve remover pesquisa e retornar 204")
    void deleteSurvey_shouldReturn204() throws Exception {
        Long id = 77L;

        mockMvc.perform(delete("/api/surveys/{id}", id))
                .andExpect(status().isNoContent());

        assertTrue(surveyService.wasDeleteCalled());
        assertEquals(id, surveyService.getLastDeleteId());
    }

    static class TestSurveyService extends SurveyService {

        private List<SurveyResponseDTO> findAllResult = List.of();
        private List<SurveyResponseDTO> findAllAtivasResult = List.of();
        private SurveyResponseDTO findByIdResult;
        private SurveyResponseDTO createResult;
        private List<SurveyResponseDTO> createBatchResult = List.of();
        private SurveyResponseDTO updateResult;

        private boolean findAllCalled;
        private boolean findAllAtivasCalled;

        private Long lastFindByIdId;
        private boolean createCalled;
        private SurveyRequestDTO lastCreateRequest;
        private List<SurveyRequestDTO> lastBatchRequest;
        private Long lastUpdateId;
        private SurveyRequestDTO lastUpdateRequest;
        private boolean deleteCalled;
        private Long lastDeleteId;

        private SurveyDetailsResponseDTO structureResult;
        private Long lastStructureSurveyId;
        private boolean lastStructureIncludeInactive;
        private Integer lastPageSize;

        TestSurveyService() {
            super(null, null, null, new SimpleMeterRegistry());
        }

        void setFindAllResult(List<SurveyResponseDTO> result) {
            this.findAllResult = result;
        }
        void setFindAllAtivasResult(List<SurveyResponseDTO> result) {
            this.findAllAtivasResult = result;
        }

        void setFindByIdResult(SurveyResponseDTO result) {
            this.findByIdResult = result;
        }

        void setCreateResult(SurveyResponseDTO result) {
            this.createResult = result;
        }

        void setCreateBatchResult(List<SurveyResponseDTO> result) {
            this.createBatchResult = result;
        }

        void setUpdateResult(SurveyResponseDTO result) {
            this.updateResult = result;
        }

        void setStructureResult(SurveyDetailsResponseDTO structureResult) {
            this.structureResult = structureResult;
        }

        Long getLastFindByIdId() {
            return lastFindByIdId;
        }

        boolean wasCreateCalled() {
            return createCalled;
        }

        SurveyRequestDTO getLastCreateRequest() {
            return lastCreateRequest;
        }

        List<SurveyRequestDTO> getLastBatchRequest() {
            return lastBatchRequest;
        }

        Long getLastUpdateId() {
            return lastUpdateId;
        }

        SurveyRequestDTO getLastUpdateRequest() {
            return lastUpdateRequest;
        }

        Long getLastStructureSurveyId() {
            return lastStructureSurveyId;
        }

        boolean wasStructureCalledWithInactive() {
            return lastStructureIncludeInactive;
        }

        boolean wasFindAllCalled() {
            return findAllCalled;
        }

        boolean wasFindAllAtivasCalled() {
            return findAllAtivasCalled;
        }

        boolean wasDeleteCalled() {
            return deleteCalled;
        }

        Long getLastDeleteId() {
            return lastDeleteId;
        }

        @Override
        public PagedResponse<SurveyResponseDTO> findAll(Pageable pageable) {
            findAllCalled = true;
            lastPageSize = pageable.getPageSize();
            return toPaged(findAllResult, pageable);
        }

        @Override
        public PagedResponse<SurveyResponseDTO> findAllAtivas(Pageable pageable) {
            findAllAtivasCalled = true;
            lastPageSize = pageable.getPageSize();
            return toPaged(findAllAtivasResult, pageable);
        }

        @Override
        public SurveyResponseDTO findById(Long id) {
            lastFindByIdId = id;
            return findByIdResult;
        }

        @Override
        public SurveyResponseDTO create(SurveyRequestDTO requestDTO) {
            createCalled = true;
            lastCreateRequest = requestDTO;
            return createResult;
        }

        @Override
        public List<SurveyResponseDTO> createBatch(List<SurveyRequestDTO> requestDTOs) {
            lastBatchRequest = requestDTOs;
            return createBatchResult;
        }

        @Override
        public SurveyResponseDTO update(Long id, SurveyRequestDTO requestDTO) {
            lastUpdateId = id;
            lastUpdateRequest = requestDTO;
            return updateResult;
        }

        @Override
        public void delete(Long id) {
            deleteCalled = true;
            lastDeleteId = id;
        }

        @Override
        public SurveyDetailsResponseDTO getSurveyStructure(Long id, boolean includeInactiveOptions, boolean includeDeleted) {
            lastStructureSurveyId = id;
            lastStructureIncludeInactive = includeInactiveOptions;
            return structureResult;
        }

        Integer getLastPageSize() {
            return lastPageSize;
        }


        private PagedResponse<SurveyResponseDTO> toPaged(List<SurveyResponseDTO> content, Pageable pageable) {
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

    @Test
    @DisplayName("GET /api/surveys deve limitar size máximo em 100")
    void getAllSurveys_shouldClampPageSize() throws Exception {
        surveyService.setFindAllResult(List.of());

        mockMvc.perform(get("/api/surveys").param("size", "1000"))
                .andExpect(status().isOk());

        assertThat(surveyService.getLastPageSize()).isEqualTo(100);
    }
}
