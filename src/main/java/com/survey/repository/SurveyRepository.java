package com.survey.repository;

import com.survey.entity.Survey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    List<Survey> findByAtivoTrue();

    Page<Survey> findByAtivoTrue(Pageable pageable);

    @Query(value = "SELECT * FROM surveys", countQuery = "SELECT COUNT(*) FROM surveys", nativeQuery = true)
    Page<Survey> findAllIncludingDeleted(Pageable pageable);

    @Query(value = "SELECT * FROM surveys WHERE id = :id", nativeQuery = true)
    Optional<Survey> findByIdIncludingDeleted(@Param("id") Long id);

    Optional<Survey> findByIdAndAtivoTrue(Long id);

    boolean existsByTitulo(String titulo);

    boolean existsByTituloAndIdNot(String titulo, Long id);

    long countByAtivoTrue();

    List<Survey> findTop5ByOrderByCreatedAtDesc();

    List<Survey> findTop5ByDataValidadeAfterOrderByDataValidadeAsc(LocalDateTime date);
}
