package com.survey.dto;

import jakarta.validation.constraints.NotNull;

public class VoteRequestDTO {

    @NotNull
    private Long surveyId;

    @NotNull
    private Long questionId;

    @NotNull
    private Long optionId;

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
}
