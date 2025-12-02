package com.survey.repository;

import com.survey.entity.Survey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByAtivoTrue();

    Page<Survey> findByAtivoTrue(Pageable pageable);

    Optional<Survey> findByIdAndAtivoTrue(Long id);

    boolean existsByTitulo(String titulo);

    boolean existsByTituloAndIdNot(String titulo, Long id);

    long countByAtivoTrue();

    List<Survey> findTop5ByOrderByCreatedAtDesc();

    List<Survey> findTop5ByDataValidadeAfterOrderByDataValidadeAsc(LocalDateTime date);
}
