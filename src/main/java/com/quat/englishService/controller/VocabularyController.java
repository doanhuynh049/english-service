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

    @PostMapping("/generate-toeic-words")
    public ResponseEntity<Map<String, Object>> generateToeicWords(
            @RequestParam(defaultValue = "10") int count) {
        try {
            logger.info("Generating {} TOEIC vocabulary words with weighted categories", count);
            List<String> toeicWords = vocabularyService.generateToeicVocabulary(count);

            logger.info("Successfully generated {} TOEIC words", toeicWords.size());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TOEIC vocabulary words generated successfully",
                "count", toeicWords.size(),
                "words", toeicWords,
                "categoryWeights", Map.of(
                    "BUSINESS", "45%",
                    "GENERAL", "25%", 
                    "ACADEMIC", "20%",
                    "SCIENTIFIC", "10%"
                )
            ));
        } catch (Exception e) {
            logger.error("Error generating TOEIC words: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/process-toeic-words-with-email")
    public ResponseEntity<Map<String, Object>> processToeicWordsWithEmail(
            @RequestParam(defaultValue = "4") int count) {
        try {
            logger.info("Generating and processing {} TOEIC words with email", count);
            
            // Generate TOEIC-specific vocabulary
            List<String> toeicWords = vocabularyService.generateToeicVocabulary(count);
            logger.info("Generated TOEIC words: {}", toeicWords);
            
            // Process the words and send email
            List<ParsedVocabularyWord> processedWords = vocabularyService.processSpecificWordsWithEmail(toeicWords);

            logger.info("Successfully processed {} TOEIC words with email", processedWords.size());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TOEIC vocabulary words generated, processed and email sent successfully",
                "wordsProcessed", processedWords.size(),
                "originalWords", toeicWords,
                "processedWords", processedWords,
                "categoryWeights", Map.of(
                    "BUSINESS", "45%",
                    "GENERAL", "25%", 
                    "ACADEMIC", "20%",
                    "SCIENTIFIC", "10%"
                )
            ));
        } catch (Exception e) {
            logger.error("Error processing TOEIC words with email: {}", e.getMessage(), e);
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
