package com.survey.dto;

import java.time.LocalDateTime;

public class SurveyResponseDTO {

    private Long id;
    private String titulo;
    private Boolean ativo;
    private LocalDateTime dataValidade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Construtores
    public SurveyResponseDTO() {
    }

    public SurveyResponseDTO(Long id, String titulo, Boolean ativo, LocalDateTime dataValidade,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.titulo = titulo;
        this.ativo = ativo;
        this.dataValidade = dataValidade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

