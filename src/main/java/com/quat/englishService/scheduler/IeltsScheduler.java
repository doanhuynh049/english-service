package com.quat.englishService.scheduler;

import com.quat.englishService.service.IeltsReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IeltsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(IeltsScheduler.class);

    private final IeltsReadingService ieltsReadingService;

    public IeltsScheduler(IeltsReadingService ieltsReadingService) {
        this.ieltsReadingService = ieltsReadingService;
    }

    /**
     * Scheduled to run every day at 11:00 AM
     * Cron expression: "0 0 11 * * *"
     * - Second: 0
     * - Minute: 0
     * - Hour: 11 (11:00 AM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (every day of the week)
     */
    @Scheduled(cron = "0 0 11 * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDailyIeltsReading() {
        logger.info("Starting scheduled IELTS Reading email task at 11:00 AM...");
        
        try {
            ieltsReadingService.processDailyIeltsReading();
            logger.info("Scheduled IELTS Reading email task completed successfully");
        } catch (Exception e) {
            logger.error("Scheduled IELTS Reading email task failed: {}", e.getMessage(), e);
        }
    }
}
