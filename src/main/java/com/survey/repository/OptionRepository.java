package com.survey.repository;

import com.survey.entity.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    List<Option> findByQuestionId(Long questionId);

    Page<Option> findByQuestionId(Long questionId, Pageable pageable);

    List<Option> findByQuestionIdIn(List<Long> questionIds);

    List<Option> findByQuestionIdInAndAtivoTrue(List<Long> questionIds);

    List<Option> findByQuestionIdAndAtivoTrue(Long questionId);

    Page<Option> findByQuestionIdAndAtivoTrue(Long questionId, Pageable pageable);

    Optional<Option> findByIdAndQuestionId(Long id, Long questionId);

    long countByQuestionIdAndAtivoTrue(Long questionId);
}
