package com.quat.englishService.scheduler;

import com.quat.englishService.service.ToeicListeningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ToeicScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ToeicScheduler.class);

    private final ToeicListeningService toeicListeningService;

    public ToeicScheduler(ToeicListeningService toeicListeningService) {
        this.toeicListeningService = toeicListeningService;
    }

    @Scheduled(cron = "0 0 18 * * ?") // Run at 6:00 PM every day
    public void scheduledToeicListeningSession() {
        logger.info("Scheduled TOEIC Listening session started at 6:00 PM");
        toeicListeningService.processDailyToeicListening();
    }
}
