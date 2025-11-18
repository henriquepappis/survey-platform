package com.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class QuestionRequestDTO {

    @NotBlank(message = "Texto da pergunta é obrigatório")
    @Size(min = 3, max = 500, message = "Texto da pergunta deve ter entre 3 e 500 caracteres")
    private String texto;

    @NotNull(message = "Ordem é obrigatória")
    private Integer ordem;

    @NotNull(message = "ID da pesquisa é obrigatório")
    private Long surveyId;

    // Construtores
    public QuestionRequestDTO() {
    }

    public QuestionRequestDTO(String texto, Integer ordem, Long surveyId) {
        this.texto = texto;
        this.ordem = ordem;
        this.surveyId = surveyId;
    }

    // Getters e Setters
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }
}

