package com.quat.englishService.controller;

import com.quat.englishService.service.ToeicListeningService;
import com.quat.englishService.service.ToeicPart7Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/toeic")
public class ToeicController {

    private static final Logger logger = LoggerFactory.getLogger(ToeicController.class);

    private final ToeicListeningService toeicListeningService;
    private final ToeicPart7Service toeicPart7Service;

    public ToeicController(ToeicListeningService toeicListeningService, ToeicPart7Service toeicPart7Service) {
        this.toeicListeningService = toeicListeningService;
        this.toeicPart7Service = toeicPart7Service;
    }

    @PostMapping("/trigger-listening")
    public ResponseEntity<Map<String, Object>> triggerToeicListening() {
        try {
            logger.info("Manually triggering TOEIC Listening processing");
            toeicListeningService.processDailyToeicListening();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TOEIC Listening processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual TOEIC Listening trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/trigger-part7")
    public ResponseEntity<Map<String, Object>> triggerToeicPart7() {
        try {
            logger.info("Manually triggering TOEIC Part 7 Reading processing");
            toeicPart7Service.processDailyToeicPart7();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "TOEIC Part 7 Reading processing completed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error during manual TOEIC Part 7 trigger: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/part7/generate")
    public ResponseEntity<Map<String, Object>> generateToeicPart7() {
        try {
            logger.info("Generating TOEIC Part 7 content");
            String content = toeicPart7Service.generateManualToeicPart7();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "content", content,
                "message", "TOEIC Part 7 content generated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error generating TOEIC Part 7 content: {}", e.getMessage(), e);
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
            "service", "toeic-listening-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
