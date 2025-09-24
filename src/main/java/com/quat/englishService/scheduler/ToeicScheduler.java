package com.quat.englishService.scheduler;

import com.quat.englishService.service.ToeicListeningService;
import com.quat.englishService.service.ToeicPart7Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ToeicScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ToeicScheduler.class);

    private final ToeicListeningService toeicListeningService;
    private final ToeicPart7Service toeicPart7Service;

    public ToeicScheduler(ToeicListeningService toeicListeningService, ToeicPart7Service toeicPart7Service) {
        this.toeicListeningService = toeicListeningService;
        this.toeicPart7Service = toeicPart7Service;
    }

    @Scheduled(cron = "0 0 6 * * ?") // Run at 6:00 AM (06:00) every day
    public void scheduledToeicListeningSession() {
        logger.info("Scheduled TOEIC listening session started at 6:00 AM");
        try {
            toeicListeningService.processDailyToeicListening();
            logger.info("Scheduled TOEIC listening session completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled TOEIC listening session: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 18 * * ?") // Run at 6:00 PM (18:00) every day
    public void scheduledToeicPart7Session() {
        logger.info("Scheduled TOEIC Part 7 reading session started at 6:00 PM");
        try {
            toeicPart7Service.processDailyToeicPart7();
            logger.info("Scheduled TOEIC Part 7 reading session completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled TOEIC Part 7 reading session: {}", e.getMessage(), e);
        }
    }
}