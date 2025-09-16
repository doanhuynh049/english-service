package com.quat.englishService.controller;

import com.quat.englishService.dto.ParsedVocabularyWord;
import com.quat.englishService.service.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @PostMapping("/trigger-daily")
    public ResponseEntity<Map<String, Object>> triggerDailyVocabulary() {
        try {
            logger.info("Manually triggering daily vocabulary processing");
            vocabularyService.processDailyVocabulary();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily vocabulary processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual daily vocabulary trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/process-words")
    public ResponseEntity<List<ParsedVocabularyWord>> processSpecificWords(
            @RequestBody List<String> words) {
        try {
            logger.info("Processing specific words: {}", words);
            List<ParsedVocabularyWord> processedWords = vocabularyService.processSpecificWords(words);

            logger.info("Successfully processed {} words", processedWords.size());
            return ResponseEntity.ok(processedWords);
        } catch (Exception e) {
            logger.error("Error processing specific words: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/process-words-with-email")
    public ResponseEntity<Map<String, Object>> processSpecificWordsWithEmail(
            @RequestBody List<String> words) {
        try {
            logger.info("Processing specific words with email: {}", words);
            List<ParsedVocabularyWord> processedWords = vocabularyService.processSpecificWordsWithEmail(words);

            logger.info("Successfully processed {} words with email", processedWords.size());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Words processed and email sent successfully",
                "wordsProcessed", processedWords.size(),
                "words", processedWords
            ));
        } catch (Exception e) {
            logger.error("Error processing specific words with email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "vocabulary-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
