package com.quat.englishService.scheduler;

import com.quat.englishService.service.ThaiLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for Thai lesson service
 * Runs daily at 17:00 (5:00 PM) to process and send Thai lesson emails
 * Focus on speaking and listening skills for beginners
 */
@Component
public class ThaiLessonScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ThaiLessonScheduler.class);

    private final ThaiLearningService thaiLearningService;

    public ThaiLessonScheduler(ThaiLearningService thaiLearningService) {
        this.thaiLearningService = thaiLearningService;
    }

    /**
     * Scheduled to run every day at 17:00 (5:00 PM)
     * Cron expression: "0 0 17 * * *"
     * - Second: 0
     * - Minute: 0  
     * - Hour: 17 (5:00 PM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of the week)
     * 
     * Timezone: Asia/Ho_Chi_Minh (can be changed as needed)
     */
    @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyThaiLesson() {
        logger.info("Starting scheduled Thai lesson task at 17:00 (5:00 PM)...");
        
        try {
            thaiLearningService.processDailyThaiLesson();
            logger.info("Scheduled Thai lesson task completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled Thai lesson task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Manual trigger method for testing
     * This can be called manually for testing purposes
     */
    public void triggerManualThaiLesson() {
        logger.info("Manually triggering Thai lesson task...");
        
        try {
            thaiLearningService.processDailyThaiLesson();
            logger.info("Manual Thai lesson task completed successfully");
        } catch (Exception e) {
            logger.error("Manual Thai lesson task failed: {}", e.getMessage(), e);
            throw new RuntimeException("Manual Thai lesson failed", e);
        }
    }
}
