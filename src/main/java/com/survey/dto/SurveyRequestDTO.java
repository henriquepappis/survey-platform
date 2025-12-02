package com.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class SurveyRequestDTO {

    @NotBlank(message = "Título é obrigatório")
    @Size(min = 3, max = 255, message = "Título deve ter entre 3 e 255 caracteres")
    private String titulo;

    @NotNull(message = "Status ativo é obrigatório")
    private Boolean ativo;

    private LocalDateTime dataValidade;

    // Construtores
    public SurveyRequestDTO() {
    }

    public SurveyRequestDTO(String titulo, Boolean ativo, LocalDateTime dataValidade) {
        this.titulo = titulo;
        this.ativo = ativo;
        this.dataValidade = dataValidade;
    }

    // Getters e Setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDateTime dataValidade) {
        this.dataValidade = dataValidade;
    }

}
