package com.survey.config;

import com.survey.repository.ResponseSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Limpeza periódica de sessões antigas para ensaio de operação (dev).
 */
@Component
@EnableScheduling
public class RetentionCleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetentionCleanupTask.class);

    private final ResponseSessionRepository responseSessionRepository;
    private final int retentionDays;

    public RetentionCleanupTask(ResponseSessionRepository responseSessionRepository,
                                @Value("${app.privacy.retention-days:90}") int retentionDays) {
        this.responseSessionRepository = responseSessionRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.privacy.cleanup-cron:0 0 3 * * *}")
    public void purgeOldSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        long deleted = responseSessionRepository.deleteByCreatedAtBefore(threshold);
        if (deleted > 0) {
            LOGGER.info("Retention cleanup removed {} response sessions older than {} days", deleted, retentionDays);
        }
    }
}
