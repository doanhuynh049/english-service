package com.quat.englishService.scheduler;

import com.quat.englishService.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VocabularyScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyScheduler.class);

    private final VocabularyService vocabularyService;

    public VocabularyScheduler(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @Scheduled(cron = "0 30 4 * * ?") // Run at 4:30 AM every day
    public void scheduledVocabularySession() {
        logger.info("Scheduled vocabulary session started at 4:30 AM");
        vocabularyService.processDailyVocabulary();
    }
}
