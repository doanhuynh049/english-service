package com.quat.englishService.controller;

import com.quat.englishService.dto.JapaneseLesson;
import com.quat.englishService.service.JapaneseLessonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Japanese lesson operations
 * Provides endpoints for manual testing and lesson processing
 */
@RestController
@RequestMapping("/api/japanese")
public class JapaneseLessonController {

    private static final Logger logger = LoggerFactory.getLogger(JapaneseLessonController.class);

    private final JapaneseLessonService japaneseLessonService;

    public JapaneseLessonController(JapaneseLessonService japaneseLessonService) {
        this.japaneseLessonService = japaneseLessonService;
    }

    /**
     * Manually trigger daily Japanese lesson processing
     * POST /api/japanese/trigger-daily
     */
    @PostMapping("/trigger-daily")
    public ResponseEntity<Map<String, Object>> triggerDailyJapaneseLesson() {
        try {
            logger.info("Manually triggering daily Japanese lesson processing");
            japaneseLessonService.processDailyJapaneseLesson();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daily Japanese lesson processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual daily Japanese lesson trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Process a specific Japanese lesson
     * POST /api/japanese/process-lesson
     */
    @PostMapping("/process-lesson")
    public ResponseEntity<Map<String, Object>> processSpecificLesson(@RequestBody Map<String, Object> lessonData) {
        try {
            String topic = (String) lessonData.get("topic");
            String description = (String) lessonData.get("description");
            Integer day = (Integer) lessonData.get("day");

            if (topic == null || description == null || day == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing required fields: topic, description, day"
                ));
            }

            logger.info("Processing specific Japanese lesson: {}", topic);
            JapaneseLesson lesson = japaneseLessonService.processSpecificLesson(topic, description, day);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully processed Japanese lesson",
                "lesson", lesson
            ));
        } catch (Exception e) {
            logger.error("Error processing specific Japanese lesson: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get service status and configuration
     * GET /api/japanese/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "Japanese Lesson Service",
                "description", "Daily Japanese learning lessons with AI-generated content",
                "features", java.util.List.of(
                    "Excel-based lesson management",
                    "AI-generated lesson content", 
                    "Structured examples and practice tasks",
                    "HTML email delivery",
                    "Automatic status tracking"
                ),
                "endpoints", Map.of(
                    "trigger-daily", "POST - Trigger daily lesson processing",
                    "process-lesson", "POST - Process specific lesson with topic, description, and day",
                    "status", "GET - Get service status"
                ),
                "excelFormat", Map.of(
                    "columns", java.util.List.of("Topic (A)", "Description (B)", "Day (C)", "Status (D)"),
                    "statusValues", java.util.List.of("Open", "Done"),
                    "filename", "Japanese_Foundation.xlsx"
                )
            ));
        } catch (Exception e) {
            logger.error("Error getting Japanese service status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/japanese/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "japanese-lesson-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
