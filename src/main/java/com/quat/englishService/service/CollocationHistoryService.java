package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollocationHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(CollocationHistoryService.class);
    private static final String COLLOCATIONS_HISTORY_FILE = "toeic_collocations_history.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ToeicListeningService.Collocation> getCollocationsForToday(int reviewCount) {
        try {
            // Load existing collocations
            List<CollocationEntry> history = loadCollocationHistory();
            
            // Get 3 recent collocations for review (avoiding today's date)
            List<ToeicListeningService.Collocation> reviewCollocations = getReviewCollocations(history, reviewCount);
            logger.info("Selected {} review collocations", reviewCollocations.size());
            
            return reviewCollocations;
            
        } catch (Exception e) {
            logger.error("Error getting collocations for today: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void saveNewCollocations(List<ToeicListeningService.Collocation> newCollocations) {
        try {
            List<CollocationEntry> history = loadCollocationHistory();
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            // Remove any existing entries for today (in case of regeneration)
            history.removeIf(entry -> today.equals(entry.getDate()));
            
            // Add new collocations
            for (ToeicListeningService.Collocation collocation : newCollocations) {
                CollocationEntry entry = new CollocationEntry(
                    today,
                    collocation.getCollocation(),
                    collocation.getIpa(),
                    collocation.getMeaning(),
                    collocation.getExample(),
                    collocation.getVietnamese()
                );
                history.add(entry);
            }
            
            // Keep only last 90 days of history
            cleanupOldHistory(history);
            
            // Save updated history
            saveCollocationHistory(history);
            logger.info("Saved {} new collocations to history", newCollocations.size());
            
        } catch (Exception e) {
            logger.error("Error saving new collocations: {}", e.getMessage(), e);
        }
    }

    public String generateNewCollocationsPrompt(List<ToeicListeningService.Collocation> reviewCollocations) {
        return generateNewCollocationsPrompt(reviewCollocations, 7); // Default to 7 for backward compatibility
    }

    public String generateNewCollocationsPrompt(List<ToeicListeningService.Collocation> reviewCollocations, int numberOfNewCollocations) {
        StringBuilder excludeList = new StringBuilder();
        
        try {
            List<CollocationEntry> history = loadCollocationHistory();
            
            // Get all unique collocations from history for exclusion
            Set<String> usedCollocations = history.stream()
                .map(CollocationEntry::getCollocation)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
            
            if (!usedCollocations.isEmpty()) {
                excludeList.append("\n\nExclude these previously used collocations:\n");
                for (String collocation : usedCollocations) {
                    excludeList.append("- ").append(collocation).append("\n");
                }
            }
            
        } catch (Exception e) {
            logger.warn("Could not load collocation history for exclusion: {}", e.getMessage());
        }

        return String.format("""
            Generate %d NEW common collocations frequently used in TOEIC Listening tests for learners within the 600–950 score range (intermediate to advanced). 

            Requirements:

            1. Output the results strictly in JSON format, with no additional text.
            2. Each collocation should have the following fields:
               - "collocation": the exact phrase
               - "ipa": IPA pronunciation
               - "meaning": short explanation in simple English suitable for TOEIC learners
               - "example": example sentence in a workplace or business context (TOEIC style)
               - "vietnamese": Vietnamese translation for both the collocation and the example sentence
            3. Present the collocations as an array under the key "collocations".
            4. Ensure the JSON is properly formatted for easy parsing in Java.
            5. Keep the output consistent so it can be directly used to generate HTML emails with structured design.
            6. Generate ONLY %d new collocations as the review collocations will be added separately.

            Example of JSON structure:
            {
                "collocations": [
                    {
                        "collocation": "make a decision",
                        "ipa": "/meɪk ə dɪˈsɪʒən/",
                        "meaning": "To choose between alternatives or options",
                        "example": "The board of directors will make a decision about the merger next week.",
                        "vietnamese": "đưa ra quyết định - Hội đồng quản trị sẽ đưa ra quyết định về việc sáp nhập vào tuần tới."
                    }
                ]
            }%s
            """, numberOfNewCollocations, numberOfNewCollocations, excludeList.toString());
    }

    /**
     * Get all collocation history entries for external services
     */
    public List<CollocationEntry> getAllCollocationHistory() {
        return loadCollocationHistory();
    }

    private List<CollocationEntry> loadCollocationHistory() {
        try {
            File historyFile = new File(COLLOCATIONS_HISTORY_FILE);
            if (!historyFile.exists()) {
                logger.info("Collocation history file not found, starting with empty history");
                return new ArrayList<>();
            }

            String content = Files.readString(historyFile.toPath());
            return objectMapper.readValue(content, new TypeReference<List<CollocationEntry>>() {});
            
        } catch (IOException e) {
            logger.error("Error loading collocation history: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void saveCollocationHistory(List<CollocationEntry> history) throws IOException {
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(history);
        Files.writeString(Paths.get(COLLOCATIONS_HISTORY_FILE), content);
    }

    private List<ToeicListeningService.Collocation> getReviewCollocations(List<CollocationEntry> history, int count) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // Filter out today's entries and get recent ones
        List<CollocationEntry> candidates = history.stream()
            .filter(entry -> !today.equals(entry.getDate()))
            .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // Most recent first
            .limit(30) // Consider last 30 entries
            .collect(Collectors.toList());
        
        // Shuffle and take the requested count
        Collections.shuffle(candidates);
        
        return candidates.stream()
            .limit(count)
            .map(entry -> new ToeicListeningService.Collocation(
                entry.getCollocation(),
                entry.getIpa(),
                entry.getMeaning(),
                entry.getExample(),
                entry.getVietnamese()
            ))
            .collect(Collectors.toList());
    }

    private void cleanupOldHistory(List<CollocationEntry> history) {
        LocalDate cutoffDate = LocalDate.now().minusDays(90);
        String cutoffDateString = cutoffDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        history.removeIf(entry -> entry.getDate().compareTo(cutoffDateString) < 0);
        logger.debug("Cleaned up collocation history, keeping entries from {} onwards", cutoffDateString);
    }

    // Inner class for collocation history entries
    public static class CollocationEntry {
        private String date;
        private String collocation;
        private String ipa;
        private String meaning;
        private String example;
        private String vietnamese;

        public CollocationEntry() {} // For Jackson

        public CollocationEntry(String date, String collocation, String ipa, String meaning, String example, String vietnamese) {
            this.date = date;
            this.collocation = collocation;
            this.ipa = ipa;
            this.meaning = meaning;
            this.example = example;
            this.vietnamese = vietnamese;
        }

        // Getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getCollocation() { return collocation; }
        public void setCollocation(String collocation) { this.collocation = collocation; }
        
        public String getIpa() { return ipa; }
        public void setIpa(String ipa) { this.ipa = ipa; }
        
        public String getMeaning() { return meaning; }
        public void setMeaning(String meaning) { this.meaning = meaning; }
        
        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
        
        public String getVietnamese() { return vietnamese; }
        public void setVietnamese(String vietnamese) { this.vietnamese = vietnamese; }
    }
}
