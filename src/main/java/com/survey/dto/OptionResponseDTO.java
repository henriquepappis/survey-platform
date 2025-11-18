package com.survey.dto;

import java.time.LocalDateTime;

public class OptionResponseDTO {

    private Long id;
    private String texto;
    private Boolean ativo;
    private Long questionId;
    private String questionTexto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Construtores
    public OptionResponseDTO() {
    }

    public OptionResponseDTO(Long id, String texto, Boolean ativo, Long questionId,
                           String questionTexto, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.texto = texto;
        this.ativo = ativo;
        this.questionId = questionId;
        this.questionTexto = questionTexto;
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

    public String getQuestionTexto() {
        return questionTexto;
    }

    public void setQuestionTexto(String questionTexto) {
        this.questionTexto = questionTexto;
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

