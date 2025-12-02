package com.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta retornada após registrar um voto.
 */
public class VoteResponseDTO {
    @Schema(example = "15")
    private Long voteId;
    @Schema(example = "33")
    private Long sessionId;
    @Schema(example = "session-33", description = "Token antifraude (quando a coleta de audiência está ativa).")
    private String antifraudToken;

    public VoteResponseDTO(Long voteId, Long sessionId, String antifraudToken) {
        this.voteId = voteId;
        this.sessionId = sessionId;
        this.antifraudToken = antifraudToken;
    }

    public Long getVoteId() {
        return voteId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getAntifraudToken() {
        return antifraudToken;
    }
}
