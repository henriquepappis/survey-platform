package com.survey.repository;

import com.survey.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findBySurveyId(Long surveyId, Pageable pageable);

    List<Question> findBySurveyIdOrderByOrdemAsc(Long surveyId);

    Optional<Question> findByIdAndSurveyId(Long id, Long surveyId);

    boolean existsBySurveyIdAndOrdem(Long surveyId, Integer ordem);

    boolean existsBySurveyIdAndOrdemAndIdNot(Long surveyId, Integer ordem, Long id);

    long countBySurveyId(Long surveyId);

    void deleteBySurveyId(Long surveyId);
}
