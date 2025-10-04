package com.quat.englishService.controller;

import com.quat.englishService.dto.ToeicVocabularyWord;
import com.quat.englishService.service.ToeicVocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
                    "test-audio", "POST - Test audio generation for specified words",
                    "send-audio-email", "POST - Send test email with audio attachments",
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

    /**
     * Test audio generation for specified words
     * POST /api/toeic-vocabulary/test-audio
     */
    @PostMapping("/test-audio")
    public ResponseEntity<Map<String, Object>> testAudioGeneration(@RequestBody List<String> words) {
        try {
            logger.info("Testing audio generation for TOEIC vocabulary words: {}", words);
            
            // Process words to get detailed explanations
            List<ToeicVocabularyWord> processedWords = toeicVocabularyService.processSpecificWords(words);
            
            // Generate audio files
            List<String> audioFilePaths = toeicVocabularyService.generateAudioFiles(processedWords);
            
            // Build response with audio file information
            List<Map<String, Object>> audioInfo = new ArrayList<>();
            for (int i = 0; i < processedWords.size(); i++) {
                ToeicVocabularyWord word = processedWords.get(i);
                Map<String, Object> wordInfo = new HashMap<>();
                wordInfo.put("word", word.getWord());
                wordInfo.put("definition", word.getDefinition());
                wordInfo.put("example", word.getExample());
                
                // Find corresponding audio files
                List<String> wordAudioFiles = audioFilePaths.stream()
                    .filter(path -> path.contains(word.getWord().toLowerCase()))
                    .collect(Collectors.toList());
                wordInfo.put("audioFiles", wordAudioFiles);
                
                audioInfo.add(wordInfo);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Audio generation test completed",
                "wordCount", processedWords.size(),
                "totalAudioFiles", audioFilePaths.size(),
                "audioDetails", audioInfo
            ));
            
        } catch (Exception e) {
            logger.error("Error testing audio generation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Audio generation test failed: " + e.getMessage(),
                "wordCount", 0,
                "totalAudioFiles", 0
            ));
        }
    }

    /**
     * Send test email with audio attachments
     * POST /api/toeic-vocabulary/send-audio-email
     */
    @PostMapping("/send-audio-email")
    public ResponseEntity<Map<String, Object>> sendAudioEmail(@RequestBody List<String> words) {
        try {
            logger.info("Sending test TOEIC vocabulary email with audio for words: {}", words);
            
            // Process words to get detailed explanations
            List<ToeicVocabularyWord> processedWords = toeicVocabularyService.processSpecificWords(words);
            
            // Generate audio files
            List<String> audioFilePaths = toeicVocabularyService.generateAudioFiles(processedWords);
            
            // Build HTML content
            String htmlContent = toeicVocabularyService.buildHtml(processedWords);
            
            // Send email with audio attachments
            toeicVocabularyService.sendEmailWithAudio(htmlContent, audioFilePaths);
            
            // Clean up audio files after sending
            toeicVocabularyService.cleanupAudioFiles(audioFilePaths);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test TOEIC vocabulary email with audio sent successfully",
                "wordCount", processedWords.size(),
                "audioFilesGenerated", audioFilePaths.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error sending test TOEIC vocabulary email with audio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Test email with audio failed: " + e.getMessage(),
                "wordCount", 0,
                "audioFilesGenerated", 0
            ));
        }
    }
}
