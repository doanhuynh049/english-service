package com.quat.englishService.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/audio")
public class AudioController {

    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Value("${app.audio.storage-path:/tmp/vocabulary-audio}")
    private String audioStoragePath;

    @GetMapping("/{date}/{filename}")
    public ResponseEntity<Resource> getAudioFile(
            @PathVariable String date,
            @PathVariable String filename) {

        try {
            // Validate filename for security
            if (!isValidFilename(filename) || !isValidDate(date)) {
                logger.warn("Invalid audio file request: date={}, filename={}", date, filename);
                return ResponseEntity.badRequest().build();
            }

            // Build file path
            Path filePath = Paths.get(audioStoragePath, date, filename);
            File file = filePath.toFile();

            // Check if file exists and is readable
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                logger.warn("Audio file not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Create resource
            Resource resource = new FileSystemResource(file);

            // Set appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                       "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"); // Cache for 1 hour

            logger.debug("Serving audio file: {}", filePath);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving audio file: date={}, filename={}, error={}",
                        date, filename, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Audio service is running");
    }

    private boolean isValidFilename(String filename) {
        // Only allow alphanumeric characters, underscores, hyphens, and .mp3 extension
        return filename != null &&
               filename.matches("^[a-zA-Z0-9_-]+\\.(mp3|wav)$") &&
               filename.length() <= 100;
    }

    private boolean isValidDate(String date) {
        // Validate date format (YYYY-MM-DD)
        return date != null && date.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }
}
