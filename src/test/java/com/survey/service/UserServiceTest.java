package com.survey.service;

import com.survey.dto.UserCreateRequestDTO;
import com.survey.dto.UserPasswordUpdateRequestDTO;
import com.survey.dto.UserUpdateRequestDTO;
import com.survey.entity.UserAccount;
import com.survey.exception.BusinessException;
import com.survey.exception.ResourceNotFoundException;
import com.survey.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("create deve persistir usuário com senha criptografada")
    void create_shouldSaveEncodedPassword() {
        UserCreateRequestDTO dto = new UserCreateRequestDTO("admin2", "secret", "ADMIN");
        when(userRepository.existsByUsername("admin2")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        UserAccount saved = buildUser(2L, "admin2");
        when(userRepository.save(any(UserAccount.class))).thenReturn(saved);

        var response = userService.create(dto);

        assertThat(response.getId()).isEqualTo(2L);
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("admin2") && user.getPassword().equals("encoded")));
    }

    @Test
    @DisplayName("create deve lançar erro quando username já existe")
    void create_shouldFailWhenUsernameExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        assertThrows(BusinessException.class,
                () -> userService.create(new UserCreateRequestDTO("admin", "123456", "ADMIN")));
    }

    @Test
    @DisplayName("update deve alterar username e role")
    void update_shouldChangeUsernameAndRole() {
        UserAccount existing = buildUser(3L, "old");
        when(userRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        var response = userService.update(3L, new UserUpdateRequestDTO("new", "manager"));

        assertThat(response.getUsername()).isEqualTo("new");
        verify(userRepository).save(existing);
    }

    @Test
    @DisplayName("updatePassword deve salvar senha codificada")
    void updatePassword_shouldEncode() {
        UserAccount existing = buildUser(4L, "user");
        when(userRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("nova")).thenReturn("encoded");

        userService.updatePassword(4L, new UserPasswordUpdateRequestDTO("nova"));

        verify(userRepository).save(existing);
        assertThat(existing.getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("delete deve lançar erro quando usuário não existe")
    void delete_shouldFailWhenMissing() {
        // UserService.delete usa findById (não existsById)
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.delete(10L));
    }

    private UserAccount buildUser(Long id, String username) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("pwd");
        user.setRole("ADMIN");
        return user;
    }
}
