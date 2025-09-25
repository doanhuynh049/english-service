package com.quat.englishService.controller;

import com.quat.englishService.dto.ToeicVocabularyWord;
import com.quat.englishService.service.ToeicVocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for TOEIC vocabulary operations
 * Provides endpoints for manual testing and word processing
 */
@RestController
@RequestMapping("/api/toeic-vocabulary")
public class ToeicVocabularyController {

    private static final Logger logger = LoggerFactory.getLogger(ToeicVocabularyController.class);

    private final ToeicVocabularyService toeicVocabularyService;

    public ToeicVocabularyController(ToeicVocabularyService toeicVocabularyService) {
        this.toeicVocabularyService = toeicVocabularyService;
    }

    /**
     * Manually trigger daily TOEIC vocabulary processing
     * POST /api/toeic-vocabulary/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<Map<String, Object>> triggerDailyToeicVocabulary() {
        try {
            logger.info("Manually triggering daily TOEIC vocabulary processing");
            toeicVocabularyService.processDailyToeicVocabulary();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily TOEIC vocabulary processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual daily TOEIC vocabulary trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Generate new TOEIC vocabulary words
     * GET /api/toeic-vocabulary/generate-new
     */
    @GetMapping("/generate-new")
    public ResponseEntity<Map<String, Object>> generateNewWords() {
        try {
            logger.info("Manually generating new TOEIC vocabulary words");
            List<ToeicVocabularyWord> newWords = toeicVocabularyService.generateNewWords();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully generated new TOEIC vocabulary words",
                "words", newWords,
                "count", newWords.size()
            ));
        } catch (Exception e) {
            logger.error("Error generating new TOEIC vocabulary words: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Process specific TOEIC words with detailed explanations
     * POST /api/toeic-vocabulary/process-words
     */
    @PostMapping("/process-words")
    public ResponseEntity<Map<String, Object>> processSpecificWords(@RequestBody List<String> words) {
        try {
            logger.info("Processing specific TOEIC words: {}", words);
            List<ToeicVocabularyWord> processedWords = toeicVocabularyService.processSpecificWords(words);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully processed TOEIC vocabulary words",
                "words", processedWords,
                "count", processedWords.size()
            ));
        } catch (Exception e) {
            logger.error("Error processing specific TOEIC words: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Send test email with specified words
     * POST /api/toeic-vocabulary/send-test-email
     */
    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody List<String> words) {
        try {
            logger.info("Sending test TOEIC vocabulary email with words: {}", words);
            
            // Process words to get detailed explanations
            List<ToeicVocabularyWord> processedWords = toeicVocabularyService.processSpecificWords(words);
            
            // Build HTML content
            String htmlContent = toeicVocabularyService.buildHtml(processedWords);
            
            // Send email
            String subject = "🎯 Test TOEIC Vocabulary - " + 
                           java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                           " (Manual Test)";
            toeicVocabularyService.sendEmail(htmlContent);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test TOEIC vocabulary email sent successfully",
                "wordCount", processedWords.size()
            ));
        } catch (Exception e) {
            logger.error("Error sending test TOEIC vocabulary email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get service status and configuration
     * GET /api/toeic-vocabulary/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "TOEIC Vocabulary Service",
                "description", "Advanced TOEIC vocabulary for score 800+, focusing on Part 6 & 7",
                "features", List.of(
                    "AI-generated vocabulary words",
                    "Business and academic contexts", 
                    "Detailed explanations with collocations",
                    "Vietnamese translations",
                    "Excel logging and history",
                    "HTML email delivery"
                ),
                "endpoints", Map.of(
                    "trigger-daily", "POST - Trigger daily vocabulary processing",
                    "generate-new", "GET - Generate 10 new vocabulary words",
                    "process-words", "POST - Get detailed explanations for specific words",
                    "send-test-email", "POST - Send test email with specified words",
                    "status", "GET - Get service status"
                )
            ));
        } catch (Exception e) {
            logger.error("Error getting service status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
