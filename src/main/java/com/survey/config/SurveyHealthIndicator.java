package com.survey.config;

import com.survey.repository.SurveyRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SurveyHealthIndicator implements HealthIndicator {

    private final SurveyRepository surveyRepository;

    public SurveyHealthIndicator(SurveyRepository surveyRepository) {
        this.surveyRepository = surveyRepository;
    }

    @Override
    public Health health() {
        try {
            long total = surveyRepository.count();
            return Health.up()
                    .withDetail("surveys.total", total)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
