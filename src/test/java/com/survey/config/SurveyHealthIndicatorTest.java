package com.survey.config;

import com.survey.repository.SurveyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SurveyHealthIndicatorTest {

    private final SurveyRepository surveyRepository = Mockito.mock(SurveyRepository.class);
    private final SurveyHealthIndicator healthIndicator = new SurveyHealthIndicator(surveyRepository);

    @Test
    @DisplayName("Health indicator deve retornar UP quando count for bem-sucedido")
    void health_whenRepositoryAccessible_shouldReturnUp() {
        when(surveyRepository.count()).thenReturn(5L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsEntry("surveys.total", 5L);
    }

    @Test
    @DisplayName("Health indicator deve retornar DOWN quando ocorrer erro")
    void health_whenRepositoryThrows_shouldReturnDown() {
        when(surveyRepository.count()).thenThrow(new RuntimeException("DB offline"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }
}
