package com.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OptionRequestDTO {

    @NotBlank(message = "Texto da opção é obrigatório")
    @Size(min = 1, max = 255, message = "Texto da opção deve ter entre 1 e 255 caracteres")
    private String texto;

    @NotNull(message = "Status ativo é obrigatório")
    private Boolean ativo;

    @NotNull(message = "ID da pergunta é obrigatório")
    private Long questionId;

    // Construtores
    public OptionRequestDTO() {
    }

    public OptionRequestDTO(String texto, Boolean ativo, Long questionId) {
        this.texto = texto;
        this.ativo = ativo;
        this.questionId = questionId;
    }

    // Getters e Setters
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
}

