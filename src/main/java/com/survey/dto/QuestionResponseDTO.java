package com.survey.dto;

import java.time.LocalDateTime;

public class QuestionResponseDTO {

    private Long id;
    private String texto;
    private Integer ordem;
    private Long surveyId;
    private String surveyTitulo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Construtores
    public QuestionResponseDTO() {
    }

    public QuestionResponseDTO(Long id, String texto, Integer ordem, Long surveyId,
                             String surveyTitulo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.texto = texto;
        this.ordem = ordem;
        this.surveyId = surveyId;
        this.surveyTitulo = surveyTitulo;
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

    public String getSurveyTitulo() {
        return surveyTitulo;
    }

    public void setSurveyTitulo(String surveyTitulo) {
        this.surveyTitulo = surveyTitulo;
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

