package com.quat.englishService.controller;

import com.quat.englishService.dto.ThaiLesson;
import com.quat.englishService.service.ThaiLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Thai language learning operations
 * Provides endpoints for manual testing and lesson processing
 * Focus: Speaking and listening skills for beginners
 */
@RestController
@RequestMapping("/api/thai")
public class ThaiLearningController {

    private static final Logger logger = LoggerFactory.getLogger(ThaiLearningController.class);

    private final ThaiLearningService thaiLearningService;

    public ThaiLearningController(ThaiLearningService thaiLearningService) {
        this.thaiLearningService = thaiLearningService;
    }

    /**
     * Manually trigger daily Thai lesson processing
     * POST /api/thai/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<Map<String, Object>> triggerDailyThaiLesson() {
        try {
            logger.info("Manually triggering daily Thai lesson processing");
            thaiLearningService.processDailyThaiLesson();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily Thai lesson processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual daily Thai lesson trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Process a specific Thai lesson for testing
     * POST /api/thai/process-lesson
     */
    @PostMapping("/process-lesson")
    public ResponseEntity<Map<String, Object>> processSpecificLesson(@RequestBody Map<String, Object> lessonData) {
        try {
            String topic = (String) lessonData.get("topic");
            Integer day = (Integer) lessonData.get("day");

            if (topic == null || day == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing required fields: topic, day"
                ));
            }

            logger.info("Processing specific Thai lesson: Day {} - {}", day, topic);
            ThaiLesson lesson = thaiLearningService.processSpecificLesson(topic, day);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully processed Thai lesson",
                "lesson", Map.of(
                    "day", lesson.getDay(),
                    "topic", lesson.getTopic(),
                    "lessonTitle", lesson.getLessonTitle(),
                    "vocabularyCount", lesson.getVocabulary() != null ? lesson.getVocabulary().length : 0,
                    "exampleSentencesCount", lesson.getExampleSentences() != null ? lesson.getExampleSentences().length : 0,
                    "listeningExercisesCount", lesson.getListeningExercises() != null ? lesson.getListeningExercises().length : 0,
                    "speakingExercisesCount", lesson.getSpeakingExercises() != null ? lesson.getSpeakingExercises().length : 0,
                    "quizQuestionsCount", lesson.getQuizQuestions() != null ? lesson.getQuizQuestions().length : 0
                )
            ));
        } catch (Exception e) {
            logger.error("Error processing specific Thai lesson: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get service status and configuration
     * GET /api/thai/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "Thai Learning Service",
                "description", "Daily Thai language lessons focused on speaking and listening for beginners",
                "schedule", "Daily at 17:00 (Asia/Ho_Chi_Minh timezone)",
                "focus", java.util.List.of(
                    "Speaking skills for beginners",
                    "Listening comprehension", 
                    "Practical vocabulary with IPA pronunciation",
                    "Conversation scenarios",
                    "Tone awareness and pronunciation drills"
                ),
                "features", java.util.List.of(
                    "Excel-based lesson management (90 days total)",
                    "AI-generated lesson content with cultural context", 
                    "IPA pronunciation for all Thai vocabulary",
                    "Practical listening and speaking exercises",
                    "Interactive quiz questions",
                    "HTML email delivery with beautiful formatting",
                    "Automatic progress tracking"
                ),
                "endpoints", Map.of(
                    "trigger-daily", "POST - Trigger daily lesson processing",
                    "process-lesson", "POST - Process specific lesson with topic and day",
                    "status", "GET - Get service status and information",
                    "health", "GET - Health check endpoint"
                ),
                "excelFormat", Map.of(
                    "columns", java.util.List.of("Day (A)", "Topic (B)", "Status (C)", "Completed Day (D)"),
                    "statusValues", java.util.List.of("Open", "Done"),
                    "totalLessons", 90,
                    "filename", "Thai_Learning.xlsx",
                    "example", Map.of(
                        "day", 1,
                        "topic", "Basic Greetings and Introductions",
                        "status", "Open",
                        "completedDay", ""
                    )
                ),
                "learningApproach", Map.of(
                    "targetAudience", "Absolute beginners",
                    "skillFocus", java.util.List.of("Speaking", "Listening"),
                    "methodology", java.util.List.of(
                        "Practical conversation scenarios",
                        "Tone and pronunciation emphasis", 
                        "Cultural context integration",
                        "Step-by-step progression",
                        "Interactive practice exercises"
                    )
                )
            ));
        } catch (Exception e) {
            logger.error("Error getting Thai service status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/thai/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "thai-learning-service",
            "timestamp", System.currentTimeMillis(),
            "focus", "Speaking & Listening for Beginners"
        ));
    }
}
