package com.survey.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.dto.AuthRequest;
import com.survey.entity.UserAccount;
import com.survey.repository.UserRepository;
import com.survey.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");

        UserAccount user = new UserAccount();
        user.setUsername("julia");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("USER");

        userRepository.saveAll(List.of(admin, user));
    }

    @Test
    @DisplayName("Login deve retornar token Bearer e expiração")
    void login_shouldReturnToken() throws Exception {
        AuthRequest request = new AuthRequest("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("Login com senha incorreta deve retornar 401")
    void login_withWrongPassword_shouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest("admin", "wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rota ADMIN sem token deve responder 403 (acesso negado)")
    void adminRoute_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rota ADMIN com token de USER deve responder 403")
    void adminRoute_withUserToken_shouldReturn403() throws Exception {
        String token = generateToken("julia", "USER");

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rota ADMIN com token ADMIN deve responder 200")
    void adminRoute_withAdminToken_shouldReturn200() throws Exception {
        String token = generateToken("admin", "ADMIN");

        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.totalSurveys").exists());
    }

    private String generateToken(String username, String role) {
        User principal = new User(username, "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
        String token = jwtTokenProvider.generateToken(authentication);
        assertThat(token).isNotBlank();
        return token;
    }
}
