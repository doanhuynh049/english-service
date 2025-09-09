package com.quat.englishService.controller;

import com.quat.englishService.service.IeltsReadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ielts")
public class IeltsController {

    private static final Logger logger = LoggerFactory.getLogger(IeltsController.class);

    private final IeltsReadingService ieltsReadingService;

    public IeltsController(IeltsReadingService ieltsReadingService) {
        this.ieltsReadingService = ieltsReadingService;
    }

    /**
     * Manually trigger IELTS Reading email generation and sending
     * GET /api/ielts/send-reading
     */
    @PostMapping("/send-reading")
    public ResponseEntity<Map<String, Object>> sendIeltsReading() {
        logger.info("Manual IELTS Reading email trigger requested");
        
        try {
            ieltsReadingService.processDailyIeltsReading();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "IELTS Reading email sent successfully",
                "timestamp", System.currentTimeMillis()
            );
            
            logger.info("Manual IELTS Reading email completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Manual IELTS Reading email failed: {}", e.getMessage(), e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to send IELTS Reading email: " + e.getMessage(),
                "error", e.getClass().getSimpleName(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
