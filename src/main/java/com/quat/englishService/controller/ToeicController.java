package com.quat.englishService.controller;

import com.quat.englishService.service.ToeicListeningService;
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

    public ToeicController(ToeicListeningService toeicListeningService) {
        this.toeicListeningService = toeicListeningService;
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "toeic-listening-service",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
