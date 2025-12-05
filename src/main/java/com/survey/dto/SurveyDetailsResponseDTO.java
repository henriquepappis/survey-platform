package com.survey.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa a estrutura completa de uma pesquisa,
 * com perguntas e opções agrupadas para facilitar o consumo no frontend.
 */
public class SurveyDetailsResponseDTO {

    private Long id;
    private String titulo;
    private String descricao;
    private Boolean ativo;
    private LocalDateTime dataValidade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuestionDetails> questions;

    public SurveyDetailsResponseDTO(Long id,
                                    String titulo,
                                    String descricao,
                                    Boolean ativo,
                                    LocalDateTime dataValidade,
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt,
                                    List<QuestionDetails> questions) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.ativo = ativo;
        this.dataValidade = dataValidade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.questions = questions;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public LocalDateTime getDataValidade() {
        return dataValidade;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<QuestionDetails> getQuestions() {
        return questions;
    }

    public static class QuestionDetails {
        private Long id;
        private String texto;
        private Integer ordem;
        private List<OptionDetails> options;

        public QuestionDetails(Long id, String texto, Integer ordem, List<OptionDetails> options) {
            this.id = id;
            this.texto = texto;
            this.ordem = ordem;
            this.options = options;
        }

        public Long getId() {
            return id;
        }

        public String getTexto() {
            return texto;
        }

        public Integer getOrdem() {
            return ordem;
        }

        public List<OptionDetails> getOptions() {
            return options;
        }
    }

    public static class OptionDetails {
        private Long id;
        private String texto;
        private Boolean ativo;

        public OptionDetails(Long id, String texto, Boolean ativo) {
            this.id = id;
            this.texto = texto;
            this.ativo = ativo;
        }

        public Long getId() {
            return id;
        }

        public String getTexto() {
            return texto;
        }

        public Boolean getAtivo() {
            return ativo;
        }
    }
}
