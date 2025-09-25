package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.ToeicVocabularyWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing advanced TOEIC vocabulary targeting score 800+
 * Focuses on Part 6 (Text Completion) and Part 7 (Reading Comprehension)
 */
@Service
public class ToeicVocabularyService {

    private static final Logger logger = LoggerFactory.getLogger(ToeicVocabularyService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;
    private final ExcelService excelService;
    private final ObjectMapper objectMapper;

    @Value("${app.toeic-excel-file-path:toeic_vocabulary_log.xlsx}")
    private String toeicExcelFilePath;

    // AI Prompts for TOEIC vocabulary generation
    private static final String GENERATE_NEW_WORDS_PROMPT = """
            Generate 10 new advanced TOEIC vocabulary words that are suitable for learners targeting a score of 800+, focusing on Part 6 (Text Completion) and Part 7 (Reading Comprehension).
            
            For each word, provide:
            - Word
            - Part of Speech (POS)
            - Short definition (1–2 lines)
            
            Output in JSON array format, e.g.:
            [
              { "word": "alleviate", "pos": "verb", "definition": "to make a problem or situation less severe" },
              { "word": "comply", "pos": "verb", "definition": "to act in accordance with rules or requests" }
            ]
            
            Focus on business, academic, and formal contexts commonly found in TOEIC tests.
            """;

    private static final String GENERATE_EXPLANATIONS_PROMPT = """
            You are helping TOEIC learners aiming for a score of 800+. I will give you 15 vocabulary words (10 new + 5 random from past lists).
            
            For each word, provide the following in JSON format:
            - Word
            - Part of Speech (POS)
            - Definition (concise but clear)
            - Example sentence (formal/business context, TOEIC style)
            - Common collocations (3–4 useful ones)
            - Vietnamese translation
            
            Example output format:
            [
              {
                "word": "alleviate",
                "pos": "verb",
                "definition": "to make a problem or situation less severe",
                "example": "The new software was designed to alleviate workload for office staff.",
                "collocations": ["alleviate pressure", "alleviate concerns", "alleviate difficulties"],
                "translation": "giảm bớt, làm nhẹ"
              }
            ]
            
            Words to process: %s
            """;

    public ToeicVocabularyService(GeminiClient geminiClient, EmailService emailService, 
                                  ExcelService excelService, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.excelService = excelService;
        this.objectMapper = objectMapper;
    }

    /**
     * Main daily processing method
     */
    public void processDailyToeicVocabulary() {
        logger.info("Starting daily TOEIC vocabulary processing...");

        try {
            // Step 1: Generate 10 new words
            List<ToeicVocabularyWord> newWords = generateNewWords();
            logger.info("Generated {} new TOEIC words", newWords.size());

            // Step 2: Save new words to Excel
            if (!newWords.isEmpty()) {
                saveWordsToExcel(newWords);
                logger.info("Saved {} new words to Excel", newWords.size());
            }

            // Step 3: Select 15 words for email (10 new + 5 random old)
            List<ToeicVocabularyWord> selectedWords = selectWordsForEmail(newWords);
            logger.info("Selected {} words for email", selectedWords.size());

            // Step 4: Get detailed explanations from AI
            List<ToeicVocabularyWord> enrichedWords = getExplanationsFromAPI(selectedWords);
            logger.info("Enriched {} words with detailed explanations", enrichedWords.size());

            // Step 5: Build HTML email content
            String htmlContent = buildHtml(enrichedWords);

            // Step 6: Send email
            sendEmail(htmlContent);

            logger.info("Daily TOEIC vocabulary processing completed successfully");

        } catch (Exception e) {
            logger.error("Error during daily TOEIC vocabulary processing: {}", e.getMessage(), e);
            throw new RuntimeException("Daily TOEIC vocabulary processing failed", e);
        }
    }

    /**
     * Generate 10 new advanced TOEIC vocabulary words using AI
     */
    public List<ToeicVocabularyWord> generateNewWords() {
        logger.info("Generating 10 new advanced TOEIC vocabulary words...");

        try {
            String response = geminiClient.generateContent(GENERATE_NEW_WORDS_PROMPT);
            
            if (response == null || response.trim().isEmpty()) {
                logger.warn("Empty response from AI, returning empty list");
                return Collections.emptyList();
            }

            logger.info("Full response: {}", response);

            // Clean the response - remove markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned response: {}", cleanedResponse);

            // Parse JSON response
            List<Map<String, String>> wordMaps = objectMapper.readValue(
                cleanedResponse, new TypeReference<List<Map<String, String>>>() {}
            );

            List<ToeicVocabularyWord> words = wordMaps.stream()
                .map(map -> {
                    ToeicVocabularyWord word = new ToeicVocabularyWord();
                    word.setWord(map.get("word"));
                    word.setPartOfSpeech(map.get("pos"));
                    word.setDefinition(map.get("definition"));
                    return word;
                })
                .collect(Collectors.toList());

            logger.info("Successfully generated {} TOEIC vocabulary words", words.size());
            return words;

        } catch (Exception e) {
            logger.error("Error generating new TOEIC words: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Save words to Excel file (append mode)
     */
    public void saveWordsToExcel(List<ToeicVocabularyWord> words) {
        logger.info("Saving {} TOEIC words to Excel...", words.size());
        
        try {
            excelService.saveToeicVocabularyWords(words);
            logger.info("Successfully saved {} TOEIC words to Excel", words.size());
        } catch (Exception e) {
            logger.error("Error saving TOEIC words to Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save TOEIC words to Excel", e);
        }
    }

    /**
     * Select 15 words for email: 10 new + 5 random from existing
     */
    public List<ToeicVocabularyWord> selectWordsForEmail(List<ToeicVocabularyWord> newWords) {
        logger.info("Selecting words for email: {} new words provided", newWords.size());
        
        List<ToeicVocabularyWord> selectedWords = new ArrayList<>();
        
        // Add all new words (up to 10)
        selectedWords.addAll(newWords.stream().limit(10).collect(Collectors.toList()));
        
        // Get 5 random words from Excel
        try {
            List<ToeicVocabularyWord> existingWords = excelService.getRandomToeicWords(5);
            selectedWords.addAll(existingWords);
            logger.info("Added {} existing words from Excel", existingWords.size());
        } catch (Exception e) {
            logger.warn("Could not get existing words from Excel, continuing with new words only: {}", e.getMessage());
        }
        
        // Shuffle the final list
        Collections.shuffle(selectedWords);
        
        logger.info("Selected {} total words for email", selectedWords.size());
        return selectedWords;
    }

    /**
     * Get detailed explanations from AI for selected words
     */
    public List<ToeicVocabularyWord> getExplanationsFromAPI(List<ToeicVocabularyWord> words) {
        logger.info("Getting detailed explanations for {} words", words.size());
        
        try {
            // Create comma-separated list of words
            String wordsList = words.stream()
                .map(ToeicVocabularyWord::getWord)
                .collect(Collectors.joining(", "));
            
            String prompt = String.format(GENERATE_EXPLANATIONS_PROMPT, wordsList);
            String response = geminiClient.generateContent(prompt);
            
            if (response == null || response.trim().isEmpty()) {
                logger.warn("Empty response from AI for explanations");
                return words; // Return original words without enrichment
            }

            logger.info("Full explanations response: {}", response);

            // Clean the response - remove markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(response);
            logger.info("Cleaned explanations response: {}", cleanedResponse);

            // Parse JSON response
            List<Map<String, Object>> explanationMaps = objectMapper.readValue(
                cleanedResponse, new TypeReference<List<Map<String, Object>>>() {}
            );

            // Map explanations back to words
            Map<String, ToeicVocabularyWord> wordMap = words.stream()
                .collect(Collectors.toMap(
                    w -> w.getWord().toLowerCase(), 
                    w -> w, 
                    (existing, replacement) -> existing
                ));

            for (Map<String, Object> explanationMap : explanationMaps) {
                String word = (String) explanationMap.get("word");
                if (word != null && wordMap.containsKey(word.toLowerCase())) {
                    ToeicVocabularyWord toeicWord = wordMap.get(word.toLowerCase());
                    toeicWord.setDefinition((String) explanationMap.get("definition"));
                    toeicWord.setExample((String) explanationMap.get("example"));
                    toeicWord.setVietnameseTranslation((String) explanationMap.get("translation"));
                    
                    // Handle collocations array
                    Object collocationsObj = explanationMap.get("collocations");
                    if (collocationsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> collocationsList = (List<String>) collocationsObj;
                        toeicWord.setCollocations(collocationsList.toArray(new String[0]));
                    }
                }
            }

            logger.info("Successfully enriched {} words with detailed explanations", words.size());
            return words;

        } catch (Exception e) {
            logger.error("Error getting explanations from API: {}", e.getMessage(), e);
            return words; // Return original words without enrichment
        }
    }

    /**
     * Build HTML email content manually
     */
    public String buildHtml(List<ToeicVocabularyWord> words) {
        logger.info("Building HTML content for {} words", words.size());
        
        StringBuilder html = new StringBuilder();
        
        // Email header
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <title>Daily TOEIC Vocabulary - Score 800+</title>\n")
            .append("    <style>\n")
            .append("        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 800px; margin: 0 auto; padding: 20px; }\n")
            .append("        h1 { color: #2c3e50; text-align: center; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n")
            .append("        h2 { color: #e74c3c; margin-top: 30px; margin-bottom: 15px; }\n")
            .append("        .word-container { background: #f8f9fa; border-left: 4px solid #3498db; padding: 15px; margin: 20px 0; border-radius: 5px; }\n")
            .append("        .pos { color: #7f8c8d; font-style: italic; font-size: 0.9em; }\n")
            .append("        .definition { margin: 10px 0; font-weight: 500; }\n")
            .append("        .example { background: #e8f5e8; padding: 10px; border-radius: 5px; margin: 10px 0; font-style: italic; }\n")
            .append("        .collocations { background: #fff3cd; padding: 10px; border-radius: 5px; margin: 10px 0; }\n")
            .append("        .translation { color: #6f42c1; font-weight: 500; margin: 10px 0; }\n")
            .append("        .footer { text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; color: #7f8c8d; }\n")
            .append("        hr { border: none; height: 2px; background: linear-gradient(to right, #3498db, #e74c3c); margin: 30px 0; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n");
        
        // Email title
        html.append("    <h1>🎯 Daily TOEIC Vocabulary - Target Score 800+</h1>\n")
            .append("    <p style=\"text-align: center; color: #7f8c8d; font-size: 1.1em;\">")
            .append("Part 6 & 7 Focus • Business & Academic Contexts • ")
            .append(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")))
            .append("</p>\n\n");
        
        // Word entries
        for (int i = 0; i < words.size(); i++) {
            ToeicVocabularyWord word = words.get(i);
            
            html.append("    <div class=\"word-container\">\n");
            html.append("        <h2>").append(i + 1).append(". ").append(word.getWord()).append("</h2>\n");
            
            if (word.getPartOfSpeech() != null) {
                html.append("        <p class=\"pos\">").append(word.getPartOfSpeech()).append("</p>\n");
            }
            
            if (word.getDefinition() != null) {
                html.append("        <p class=\"definition\"><strong>Definition:</strong> ").append(word.getDefinition()).append("</p>\n");
            }
            
            if (word.getExample() != null) {
                html.append("        <div class=\"example\"><strong>Example:</strong> ").append(word.getExample()).append("</div>\n");
            }
            
            if (word.getCollocations() != null && word.getCollocations().length > 0) {
                html.append("        <div class=\"collocations\">\n");
                html.append("            <strong>Common Collocations:</strong><br>\n");
                for (String collocation : word.getCollocations()) {
                    html.append("            • ").append(collocation).append("<br>\n");
                }
                html.append("        </div>\n");
            }
            
            if (word.getVietnameseTranslation() != null) {
                html.append("        <p class=\"translation\"><strong>Vietnamese:</strong> ").append(word.getVietnameseTranslation()).append("</p>\n");
            }
            
            html.append("    </div>\n");
            
            if (i < words.size() - 1) {
                html.append("    <hr/>\n");
            }
        }
        
        // Footer
        html.append("    <div class=\"footer\">\n")
            .append("        <p><strong>💡 Study Tips:</strong></p>\n")
            .append("        <p>• Focus on business and formal contexts<br>\n")
            .append("        • Practice collocations in sentences<br>\n")
            .append("        • Review words from previous days<br>\n")
            .append("        • Use these words in TOEIC practice tests</p>\n")
            .append("        <p style=\"margin-top: 20px;\"><em>Keep up the great work! Target: TOEIC 800+ 🚀</em></p>\n")
            .append("    </div>\n");
        
        html.append("</body>\n</html>");
        
        logger.info("Successfully built HTML content with {} words", words.size());
        return html.toString();
    }

    /**
     * Send email with HTML content
     */
    public void sendEmail(String htmlContent) {
        logger.info("Sending TOEIC vocabulary email...");
        
        try {
            String subject = "🎯 Daily TOEIC Vocabulary - " + 
                           java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                           " (Target: 800+)";
            
            emailService.sendToeicVocabularyEmail(subject, htmlContent);
            logger.info("Successfully sent TOEIC vocabulary email");
            
        } catch (Exception e) {
            logger.error("Error sending TOEIC vocabulary email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send TOEIC vocabulary email", e);
        }
    }

    /**
     * Manual processing method for testing
     */
    public List<ToeicVocabularyWord> processSpecificWords(List<String> wordStrings) {
        logger.info("Processing {} specific TOEIC words manually", wordStrings.size());
        
        // Convert strings to ToeicVocabularyWord objects
        List<ToeicVocabularyWord> words = wordStrings.stream()
                .map(wordStr -> {
                    ToeicVocabularyWord word = new ToeicVocabularyWord();
                    word.setWord(wordStr);
                    return word;
                })
                .collect(Collectors.toList());
        
        // Get explanations and return
        return getExplanationsFromAPI(words);
    }

    /**
     * Clean JSON response by removing markdown code blocks and other formatting
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "";
        }
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); // Remove "```json"
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); // Remove "```"
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); // Remove trailing "```"
        }
        
        return cleaned.trim();
    }
}
