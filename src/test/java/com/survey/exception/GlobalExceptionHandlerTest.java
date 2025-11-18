package com.survey.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResourceNotFoundException deve retornar 404 com path correto")
    void handleResourceNotFound_shouldBuildErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/surveys/1");

        var response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("não encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getPath()).isEqualTo("/api/surveys/1");
        assertThat(response.getBody().getMessage()).contains("não encontrado");
    }

    @Test
    @DisplayName("handleMethodArgumentNotValidException deve listar erros de campo")
    void handleValidationException_shouldExposeFieldErrors() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/surveys");

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "surveyRequestDTO");
        bindingResult.addError(new FieldError("surveyRequestDTO", "titulo", "obrigatório"));
        Method method = Dummy.class.getDeclaredMethod("accept", String.class);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new org.springframework.core.MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetails())
                .containsEntry("titulo", "obrigatório");
    }

    @Test
    @DisplayName("handleTypeMismatch deve retornar 400 com mensagem amigável")
    void handleTypeMismatch_shouldReturnBadRequest() {
        HttpServletRequest request = new MockHttpServletRequest();
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, null);

        var response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("id");
    }

    private static class Dummy {
        @SuppressWarnings("unused")
        void accept(String value) {
        }
    }
}
