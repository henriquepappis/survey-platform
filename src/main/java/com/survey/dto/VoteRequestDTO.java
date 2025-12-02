package com.survey.dto;

import com.survey.entity.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class VoteRequestDTO {

    @NotNull
    @Schema(example = "1", description = "ID da pesquisa")
    private Long surveyId;

    @NotNull
    @Schema(example = "10", description = "ID da pergunta dentro da pesquisa")
    private Long questionId;

    @NotNull
    @Schema(example = "100", description = "ID da opção selecionada")
    private Long optionId;

    @Schema(example = "desktop")
    private String deviceType;
    @Schema(example = "macOS")
    private String operatingSystem;
    @Schema(example = "Chrome")
    private String browser;
    @Schema(example = "paid-ads")
    private String source;
    @Schema(example = "BR")
    private String country;
    @Schema(example = "SP")
    private String state;
    @Schema(example = "São Paulo")
    private String city;
    @Schema(description = "Status da resposta", example = "COMPLETED")
    private ResponseStatus status;
    @Schema(description = "Data/hora de início da resposta", example = "2025-12-01T18:00:00")
    private LocalDateTime startedAt;
    @Schema(description = "Data/hora de conclusão da resposta", example = "2025-12-01T18:00:05")
    private LocalDateTime completedAt;

    public VoteRequestDTO() {
    }

    public VoteRequestDTO(Long surveyId, Long questionId, Long optionId) {
        this.surveyId = surveyId;
        this.questionId = questionId;
        this.optionId = optionId;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(Long surveyId) {
        this.surveyId = surveyId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
