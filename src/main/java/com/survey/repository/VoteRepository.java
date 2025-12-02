package com.survey.repository;

import com.survey.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT v.question.id AS questionId, v.question.texto AS questionTitulo, " +
            "v.option.id AS optionId, v.option.texto AS optionTitulo, COUNT(v.id) AS total " +
            "FROM Vote v WHERE v.survey.id = :surveyId GROUP BY v.question.id, v.question.texto, v.option.id, v.option.texto")
    List<VoteSummary> summarizeBySurvey(@Param("surveyId") Long surveyId);

    boolean existsBySurveyIdAndIpAddressAndUserAgentAndCreatedAtAfter(Long surveyId,
                                                                      String ipAddress,
                                                                      String userAgent,
                                                                      java.time.LocalDateTime createdAfter);

    void deleteBySurveyId(Long surveyId);

    interface VoteSummary {
        Long getQuestionId();
        String getQuestionTitulo();
        Long getOptionId();
        String getOptionTitulo();
        Long getTotal();
    }

    @Query("SELECT v.question.id AS questionId, v.question.texto AS questionText, " +
            "v.option.id AS optionId, v.option.texto AS optionText, COUNT(v.id) AS total " +
            "FROM Vote v WHERE v.survey.id = :surveyId GROUP BY v.question.id, v.question.texto, v.option.id, v.option.texto")
    List<QuestionOptionCount> aggregateBySurvey(@Param("surveyId") Long surveyId);

    interface QuestionOptionCount {
        Long getQuestionId();
        String getQuestionText();
        Long getOptionId();
        String getOptionText();
        Long getTotal();
    }
}
