package com.quat.englishService.scheduler;

import com.quat.englishService.service.ToeicVocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for TOEIC vocabulary service
 * Runs daily at 9:00 AM to send advanced TOEIC vocabulary emails
 */
@Component
public class ToeicVocabularyScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ToeicVocabularyScheduler.class);

    private final ToeicVocabularyService toeicVocabularyService;

    public ToeicVocabularyScheduler(ToeicVocabularyService toeicVocabularyService) {
        this.toeicVocabularyService = toeicVocabularyService;
    }

    /**
     * Scheduled to run every day at 9:00 AM
     * Cron expression: "0 0 9 * * *"
     * - Second: 0
     * - Minute: 0  
     * - Hour: 9 (9:00 AM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of the week)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyToeicVocabulary() {
        logger.info("Scheduled TOEIC vocabulary session started at 9:00 AM");
        
        try {
            toeicVocabularyService.processDailyToeicVocabulary();
            logger.info("Scheduled TOEIC vocabulary session completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled TOEIC vocabulary session: {}", e.getMessage(), e);
        }
    }
}
