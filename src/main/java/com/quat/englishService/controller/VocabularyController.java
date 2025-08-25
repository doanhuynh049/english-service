package com.quat.englishService.controller;

import com.quat.englishService.service.VocabularyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerVocabularySession() {
        try {
            vocabularyService.triggerManualVocabularySession();
            return ResponseEntity.ok("Vocabulary session triggered successfully! Check your email and Excel file.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error triggering vocabulary session: " + e.getMessage());
        }
    }
}
