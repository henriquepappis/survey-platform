package com.survey.repository;

import com.survey.entity.ResponseSession;
import com.survey.entity.ResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ResponseSessionRepository extends JpaRepository<ResponseSession, Long> {

    long countByCreatedAtAfter(LocalDateTime date);

    long countByStatus(ResponseStatus status);

    @Query("SELECT AVG(function('TIMESTAMPDIFF', SECOND, rs.startedAt, rs.completedAt)) " +
            "FROM ResponseSession rs WHERE rs.startedAt IS NOT NULL AND rs.completedAt IS NOT NULL")
    Double averageCompletionSeconds();

    @Query("SELECT rs.survey.id AS surveyId, COUNT(rs.id) AS total, " +
            "SUM(CASE WHEN rs.status = com.survey.entity.ResponseStatus.COMPLETED THEN 1 ELSE 0 END) AS completed, " +
            "SUM(CASE WHEN rs.status = com.survey.entity.ResponseStatus.ABANDONED THEN 1 ELSE 0 END) AS abandoned " +
            "FROM ResponseSession rs GROUP BY rs.survey.id")
    List<SurveyAggregate> aggregateBySurvey();

    interface SurveyAggregate {
        Long getSurveyId();
        Long getTotal();
        Long getCompleted();
        Long getAbandoned();
    }

    List<ResponseSession> findBySurveyIdAndCreatedAtBetween(Long surveyId, LocalDateTime start, LocalDateTime end);

    void deleteBySurveyId(Long surveyId);

    void deleteByQuestionId(Long questionId);

    long deleteByCreatedAtBefore(LocalDateTime threshold);
}
