package com.survey.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.OptionRequestDTO;
import com.survey.dto.OptionResponseDTO;
import com.survey.dto.PagedResponse;
import com.survey.service.OptionService;
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

class OptionControllerTest {

    private MockMvc mockMvc;
    private TestOptionService optionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        optionService = new TestOptionService();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        OptionController controller = new OptionController(optionService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    private OptionResponseDTO buildOptionResponse(Long id, String texto, boolean ativo, Long questionId) {
        OptionResponseDTO dto = new OptionResponseDTO();
        dto.setId(id);
        dto.setTexto(texto);
        dto.setAtivo(ativo);
        dto.setQuestionId(questionId);
        dto.setQuestionTexto("Pergunta " + questionId);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private OptionRequestDTO buildOptionRequest(String texto, Boolean ativo, Long questionId) {
        return new OptionRequestDTO(texto, ativo, questionId);
    }

    @Test
    @DisplayName("GET /api/options deve retornar todas as opções")
    void getAllOptions_shouldReturnAll() throws Exception {
        optionService.setFindAllResult(List.of(
                buildOptionResponse(1L, "Opção 1", true, 10L),
                buildOptionResponse(2L, "Opção 2", false, 10L)
        ));

        mockMvc.perform(get("/api/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].texto", is("Opção 1")))
                .andExpect(jsonPath("$.content[1].texto", is("Opção 2")))
                .andExpect(jsonPath("$.totalElements", is(2)));

        assertTrue(optionService.wasFindAllCalled());
        assertFalse(optionService.wasFindByQuestionCalled());
    }

    @Test
    @DisplayName("GET /api/options?questionId=XX&ativo=true deve filtrar por pergunta e status")
    void getAllOptions_withQuestionId_shouldFilter() throws Exception {
        optionService.setFindByQuestionIdResult(List.of(
                buildOptionResponse(3L, "Opção ativa", true, 99L)
        ));

        mockMvc.perform(get("/api/options")
                        .param("questionId", "99")
                        .param("ativo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ativo", is(true)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        assertTrue(optionService.wasFindByQuestionCalled());
        assertEquals(99L, optionService.getLastQuestionFilterId());
        assertTrue(optionService.getLastAtivoFilter());
    }

    @Test
    @DisplayName("GET /api/options/{id} deve retornar a opção por ID")
    void getOptionById_shouldReturnOption() throws Exception {
        optionService.setFindByIdResult(buildOptionResponse(55L, "Opção única", true, 5L));

        mockMvc.perform(get("/api/options/{id}", 55))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(55)))
                .andExpect(jsonPath("$.texto", is("Opção única")));

        assertEquals(55L, optionService.getLastFindByIdId());
    }

    @Test
    @DisplayName("POST /api/options deve criar opção e retornar 201")
    void createOption_shouldReturnCreated() throws Exception {
        OptionRequestDTO request = buildOptionRequest("Nova opção", true, 7L);
        optionService.setCreateResult(buildOptionResponse(70L, "Nova opção", true, 7L));

        mockMvc.perform(post("/api/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(70)))
                .andExpect(jsonPath("$.texto", is("Nova opção")));

        assertTrue(optionService.wasCreateCalled());
        assertEquals("Nova opção", optionService.getLastCreateRequest().getTexto());
    }

    @Test
    @DisplayName("POST /api/options com dados inválidos deve retornar 400")
    void createOption_invalidPayload_shouldReturn400() throws Exception {
        OptionRequestDTO invalid = buildOptionRequest("", null, null);

        mockMvc.perform(post("/api/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        assertFalse(optionService.wasCreateCalled());
    }

    @Test
    @DisplayName("POST /api/options/batch deve criar opções em lote e retornar 201")
    void createOptionsBatch_shouldReturnCreatedList() throws Exception {
        OptionRequestDTO r1 = buildOptionRequest("O1", true, 44L);
        OptionRequestDTO r2 = buildOptionRequest("O2", false, 44L);
        optionService.setCreateBatchResult(List.of(
                buildOptionResponse(1L, "O1", true, 44L),
                buildOptionResponse(2L, "O2", false, 44L)
        ));

        mockMvc.perform(post("/api/options/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(r1, r2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        assertEquals(2, optionService.getLastBatchRequest().size());
    }

    @Test
    @DisplayName("PUT /api/options/{id} deve atualizar opção existente")
    void updateOption_shouldReturnUpdated() throws Exception {
        OptionRequestDTO request = buildOptionRequest("Atualizada", false, 5L);
        optionService.setUpdateResult(buildOptionResponse(9L, "Atualizada", false, 5L));

        mockMvc.perform(put("/api/options/{id}", 9)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto", is("Atualizada")))
                .andExpect(jsonPath("$.ativo", is(false)));

        assertEquals(9L, optionService.getLastUpdateId());
        assertEquals("Atualizada", optionService.getLastUpdateRequest().getTexto());
    }

    @Test
    @DisplayName("DELETE /api/options/{id} deve retornar 204")
    void deleteOption_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/options/{id}", 88))
                .andExpect(status().isNoContent());

        assertTrue(optionService.wasDeleteCalled());
        assertEquals(88L, optionService.getLastDeleteId());
    }

    static class TestOptionService extends OptionService {

        private List<OptionResponseDTO> findAllResult = List.of();
        private List<OptionResponseDTO> findByQuestionIdResult = List.of();
        private OptionResponseDTO findByIdResult;
        private OptionResponseDTO createResult;
        private List<OptionResponseDTO> createBatchResult = List.of();
        private OptionResponseDTO updateResult;

        private boolean findAllCalled;
        private boolean findByQuestionCalled;
        private Long lastQuestionFilterId;
        private Boolean lastAtivoFilter;
        private Long lastFindByIdId;
        private boolean createCalled;
        private OptionRequestDTO lastCreateRequest;
        private List<OptionRequestDTO> lastBatchRequest;
        private Long lastUpdateId;
        private OptionRequestDTO lastUpdateRequest;
        private boolean deleteCalled;
        private Long lastDeleteId;

        TestOptionService() {
            super(null, null);
        }

        void setFindAllResult(List<OptionResponseDTO> result) {
            this.findAllResult = result;
        }

        void setFindByQuestionIdResult(List<OptionResponseDTO> result) {
            this.findByQuestionIdResult = result;
        }

        void setFindByIdResult(OptionResponseDTO result) {
            this.findByIdResult = result;
        }

        void setCreateResult(OptionResponseDTO result) {
            this.createResult = result;
        }

        void setCreateBatchResult(List<OptionResponseDTO> result) {
            this.createBatchResult = result;
        }

        void setUpdateResult(OptionResponseDTO result) {
            this.updateResult = result;
        }

        boolean wasFindAllCalled() {
            return findAllCalled;
        }

        boolean wasFindByQuestionCalled() {
            return findByQuestionCalled;
        }

        Long getLastQuestionFilterId() {
            return lastQuestionFilterId;
        }

        Boolean getLastAtivoFilter() {
            return lastAtivoFilter;
        }

        Long getLastFindByIdId() {
            return lastFindByIdId;
        }

        boolean wasCreateCalled() {
            return createCalled;
        }

        OptionRequestDTO getLastCreateRequest() {
            return lastCreateRequest;
        }

        List<OptionRequestDTO> getLastBatchRequest() {
            return lastBatchRequest;
        }

        Long getLastUpdateId() {
            return lastUpdateId;
        }

        OptionRequestDTO getLastUpdateRequest() {
            return lastUpdateRequest;
        }

        boolean wasDeleteCalled() {
            return deleteCalled;
        }

        Long getLastDeleteId() {
            return lastDeleteId;
        }

        @Override
        public PagedResponse<OptionResponseDTO> findAll(Pageable pageable) {
            findAllCalled = true;
            return toPaged(findAllResult, pageable);
        }

        @Override
        public PagedResponse<OptionResponseDTO> findByQuestionId(Long questionId, Boolean ativo, Pageable pageable) {
            findByQuestionCalled = true;
            lastQuestionFilterId = questionId;
            lastAtivoFilter = ativo;
            return toPaged(findByQuestionIdResult, pageable);
        }

        @Override
        public OptionResponseDTO findById(Long id) {
            lastFindByIdId = id;
            return findByIdResult;
        }

        @Override
        public OptionResponseDTO create(OptionRequestDTO requestDTO) {
            createCalled = true;
            lastCreateRequest = requestDTO;
            return createResult;
        }

        @Override
        public List<OptionResponseDTO> createBatch(List<OptionRequestDTO> requestDTOs) {
            lastBatchRequest = requestDTOs;
            return createBatchResult;
        }

        @Override
        public OptionResponseDTO update(Long id, OptionRequestDTO requestDTO) {
            lastUpdateId = id;
            lastUpdateRequest = requestDTO;
            return updateResult;
        }

        @Override
        public void delete(Long id) {
            deleteCalled = true;
            lastDeleteId = id;
        }

        private PagedResponse<OptionResponseDTO> toPaged(List<OptionResponseDTO> content, Pageable pageable) {
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
