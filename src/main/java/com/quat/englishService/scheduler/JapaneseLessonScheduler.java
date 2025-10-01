package com.quat.englishService.scheduler;

import com.quat.englishService.service.JapaneseLessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Japanese lesson service
 * Runs daily to process and send Japanese lesson emails
 */
@Component
public class JapaneseLessonScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JapaneseLessonScheduler.class);

    private final JapaneseLessonService japaneseLessonService;

    public JapaneseLessonScheduler(JapaneseLessonService japaneseLessonService) {
        this.japaneseLessonService = japaneseLessonService;
    }

    /**
     * Scheduled to run every day at configurable time
     * Default: 8:00 AM (Asia/Ho_Chi_Minh timezone)
     * Cron expression: "0 0 8 * * *"
     * - Second: 0
     * - Minute: 0  
     * - Hour: 8 (8:00 AM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of the week)
     */
    @Scheduled(cron = "0 30 12 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyJapaneseLesson() {
        logger.info("Starting scheduled Japanese lesson task at 8:00 AM...");
        
        try {
            japaneseLessonService.processDailyJapaneseLesson();
            logger.info("Scheduled Japanese lesson task completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled Japanese lesson task failed: {}", e.getMessage(), e);
        }
    }
}
