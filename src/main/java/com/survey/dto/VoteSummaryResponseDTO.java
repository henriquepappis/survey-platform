package com.survey.dto;

import java.util.List;

public class VoteSummaryResponseDTO {

    private Long surveyId;
    private List<QuestionVotes> questions;

    public VoteSummaryResponseDTO(Long surveyId, List<QuestionVotes> questions) {
        this.surveyId = surveyId;
        this.questions = questions;
    }

    public Long getSurveyId() {
        return surveyId;
    }

    public List<QuestionVotes> getQuestions() {
        return questions;
    }

    public static class QuestionVotes {
        private Long questionId;
        private String questionTitulo;
        private List<OptionVotes> options;

        public QuestionVotes(Long questionId, String questionTitulo, List<OptionVotes> options) {
            this.questionId = questionId;
            this.questionTitulo = questionTitulo;
            this.options = options;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public String getQuestionTitulo() {
            return questionTitulo;
        }

        public List<OptionVotes> getOptions() {
            return options;
        }
    }

    public static class OptionVotes {
        private Long optionId;
        private String optionTitulo;
        private Long total;

        public OptionVotes(Long optionId, String optionTitulo, Long total) {
            this.optionId = optionId;
            this.optionTitulo = optionTitulo;
            this.total = total;
        }

        public Long getOptionId() {
            return optionId;
        }

        public String getOptionTitulo() {
            return optionTitulo;
        }

        public Long getTotal() {
            return total;
        }
    }
}
