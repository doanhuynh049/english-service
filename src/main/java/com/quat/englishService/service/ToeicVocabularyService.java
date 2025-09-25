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
    private final ObjectMapper objectMapper;
    private String toeicExcelFilePath = "toeic_vocabulary_log.xlsx";


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
            // Get existing words to avoid duplicates
            Set<String> existingWords = getExistingToeicWords();
            
            // Create enhanced prompt that includes words to avoid
            String enhancedPrompt = buildEnhancedPrompt(existingWords);
            
            String response = geminiClient.generateContent(enhancedPrompt);
            
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
                .filter(word -> !existingWords.contains(word.getWord().toLowerCase()))
                .collect(Collectors.toList());

            logger.info("Successfully generated {} TOEIC vocabulary words", words.size());
            return words;

        } catch (Exception e) {
            logger.error("Error generating new TOEIC words: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Build enhanced prompt that includes words to avoid
     */
    private String buildEnhancedPrompt(Set<String> existingWords) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("""
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
            """);

        // Add words to avoid if there are existing words
        if (!existingWords.isEmpty()) {
            promptBuilder.append("\n\nIMPORTANT: Do NOT include any of these previously generated words:\n");
            
            // Convert to list and limit to avoid overly long prompts
            List<String> wordsList = new ArrayList<>(existingWords);
            int maxWordsToShow = Math.min(100, wordsList.size()); // Limit to 100 words to avoid token limits
            
            for (int i = 0; i < maxWordsToShow; i++) {
                promptBuilder.append("- ").append(wordsList.get(i)).append("\n");
            }
            
            if (existingWords.size() > maxWordsToShow) {
                promptBuilder.append("... and ").append(existingWords.size() - maxWordsToShow).append(" more words.\n");
            }
            
            promptBuilder.append("\nPlease generate completely different words that are not in the above list.\n");
        }

        return promptBuilder.toString();
    }

    /**
     * Save words to Excel file (append mode)
     */
    public void saveWordsToExcel(List<ToeicVocabularyWord> words) {
        logger.info("Saving {} TOEIC words to Excel...", words.size());
        
        try {
            saveToeicVocabularyWords(words);
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
            List<ToeicVocabularyWord> existingWords = getRandomToeicWords(5);
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
     * Build HTML email content using template
     */
    public String buildHtml(List<ToeicVocabularyWord> words) {
        logger.info("Building HTML content for {} words using template", words.size());
        
        try {
            // Load the HTML template
            String template = loadEmailTemplate();
            
            // Build word sections
            StringBuilder wordSections = new StringBuilder();
            for (int i = 0; i < words.size(); i++) {
                ToeicVocabularyWord word = words.get(i);
                wordSections.append(buildWordCard(word, i + 1));
            }

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{WORD_SECTIONS}}", wordSections.toString())
                .replace("{{GENERATION_DATE}}", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            logger.info("Successfully built HTML content with {} words using template", words.size());
            return content;

        } catch (Exception e) {
            logger.error("Failed to build HTML content using template, falling back to manual build: {}", e.getMessage(), e);
            return null;
        }
    }

    private String loadEmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/toeic-vocabulary-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("TOEIC vocabulary email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String buildWordCard(ToeicVocabularyWord word, int index) {
        StringBuilder card = new StringBuilder();
        
        card.append(String.format("""
            <div class="word-card">
                <div class="word-header">
                    <h2 class="word-title">%s</h2>
                    <div class="word-number">%d</div>
                </div>
                
                <div class="word-meta">
            """, word.getWord(), index));
        
        if (word.getPartOfSpeech() != null) {
            card.append(String.format("""
                    <span class="part-of-speech">%s</span>
                """, word.getPartOfSpeech()));
        }
        
        card.append("""
                    <span class="toeic-level">Advanced</span>
                </div>
            """);
        
        // Definition section
        if (word.getDefinition() != null) {
            card.append(String.format("""
                <div class="definition-section">
                    <div class="section-header">
                        <span class="section-icon">💡</span>
                        <span class="section-title">Definition</span>
                    </div>
                    <div class="definition-text">%s</div>
                </div>
                """, escapeHtml(word.getDefinition())));
        }
        
        // Example section
        if (word.getExample() != null) {
            card.append(String.format("""
                <div class="example-section">
                    <div class="section-header">
                        <span class="section-icon">📝</span>
                        <span class="section-title">Business Context Example</span>
                    </div>
                    <div class="example-text">%s</div>
                </div>
                """, escapeHtml(word.getExample())));
        }
        
        // Collocations section
        if (word.getCollocations() != null && word.getCollocations().length > 0) {
            card.append("""
                <div class="collocations-section">
                    <div class="section-header">
                        <span class="section-icon">🤝</span>
                        <span class="section-title">Common Collocations</span>
                    </div>
                    <div class="collocations-grid">
                """);
            
            for (String collocation : word.getCollocations()) {
                card.append(String.format("""
                        <div class="collocation-item">%s</div>
                    """, escapeHtml(collocation)));
            }
            
            card.append("""
                    </div>
                </div>
                """);
        }
        
        // Vietnamese translation section
        if (word.getVietnameseTranslation() != null) {
            card.append(String.format("""
                <div class="translation-section">
                    <div class="section-header">
                        <span class="section-icon">🇻🇳</span>
                        <span class="section-title">Vietnamese Translation</span>
                    </div>
                    <div class="vietnamese-text">%s</div>
                </div>
                """, escapeHtml(word.getVietnameseTranslation())));
        }
        
        card.append("</div>");
        
        // Add divider between words (except for the last word)
        if (index < 15) { // Assuming 15 is the max number of words
            card.append("<div class=\"word-divider\"></div>");
        }
        
        return card.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
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

    /**
     * Get existing TOEIC words from Excel to avoid duplicates
     */
    public Set<String> getExistingToeicWords() {
        Set<String> existingWords = new HashSet<>();
        
        try {
            java.io.File file = new java.io.File(toeicExcelFilePath);
            if (!file.exists()) {
                logger.info("TOEIC Excel file doesn't exist yet, no existing words to avoid");
                return existingWords;
            }

            // Read Excel file
            try (java.io.FileInputStream inputStream = new java.io.FileInputStream(toeicExcelFilePath);
                 org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)) {
                
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("TOEIC Vocabulary");
                if (sheet == null) {
                    logger.info("No TOEIC Vocabulary sheet found in Excel");
                    return existingWords;
                }

                // Extract words from the sheet (skip header row)
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header
                    
                    org.apache.poi.ss.usermodel.Cell wordCell = row.getCell(1); // Word column
                    if (wordCell != null && wordCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        String word = wordCell.getStringCellValue().trim().toLowerCase();
                        if (!word.isEmpty()) {
                            existingWords.add(word);
                        }
                    }
                }
            }

            logger.info("Found {} existing TOEIC words to avoid", existingWords.size());

        } catch (Exception e) {
            logger.error("Error reading existing TOEIC words from Excel: {}", e.getMessage(), e);
        }

        return existingWords;
    }

    /**
     * Get random TOEIC words from Excel file
     */
    public List<ToeicVocabularyWord> getRandomToeicWords(int count) {
        List<ToeicVocabularyWord> randomWords = new ArrayList<>();
        
        try {
            java.io.File file = new java.io.File(toeicExcelFilePath);
            if (!file.exists()) {
                logger.info("TOEIC Excel file doesn't exist yet, returning empty list");
                return randomWords;
            }

            // Read Excel file
            try (java.io.FileInputStream inputStream = new java.io.FileInputStream(toeicExcelFilePath);
                 org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)) {
                
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("TOEIC Vocabulary");
                if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                    logger.info("No TOEIC vocabulary words found in Excel");
                    return randomWords;
                }

                // Collect all data rows (skip header)
                List<org.apache.poi.ss.usermodel.Row> dataRows = new ArrayList<>();
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    if (row.getRowNum() > 0) { // Skip header
                        dataRows.add(row);
                    }
                }

                // Shuffle and take requested count
                Collections.shuffle(dataRows);
                int takeCount = Math.min(count, dataRows.size());

                for (int i = 0; i < takeCount; i++) {
                    org.apache.poi.ss.usermodel.Row row = dataRows.get(i);
                    ToeicVocabularyWord word = extractToeicWordFromRow(row);
                    if (word != null) {
                        randomWords.add(word);
                    }
                }
            }

            logger.info("Retrieved {} random TOEIC words from Excel", randomWords.size());

        } catch (Exception e) {
            logger.error("Error getting random TOEIC words from Excel: {}", e.getMessage(), e);
        }

        return randomWords;
    }

    /**
     * Save TOEIC vocabulary words to Excel file
     */
    public void saveToeicVocabularyWords(List<ToeicVocabularyWord> words) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = getToeicWorkbook(toeicExcelFilePath);
            org.apache.poi.ss.usermodel.Sheet sheet = getOrCreateSheet(workbook, "TOEIC Vocabulary");

            // Create header if sheet is empty
            if (sheet.getPhysicalNumberOfRows() == 0) {
                createToeicHeader(sheet);
            }

            // Add TOEIC vocabulary words
            int rowNum = sheet.getLastRowNum() + 1;
            for (ToeicVocabularyWord word : words) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                populateToeicRow(row, word);
            }

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save the workbook
            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(toeicExcelFilePath)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Successfully saved {} TOEIC vocabulary words to Excel", words.size());

        } catch (Exception e) {
            logger.error("Failed to save TOEIC vocabulary words to Excel: {}", e.getMessage(), e);
        }
    }

    private org.apache.poi.ss.usermodel.Workbook getToeicWorkbook(String filePath) throws java.io.IOException {
        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(filePath)) {
            return new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);
        } catch (java.io.IOException e) {
            // File doesn't exist, create a new workbook
            logger.info("Creating new TOEIC Excel workbook: {}", filePath);
            return new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        }
    }

    private org.apache.poi.ss.usermodel.Sheet getOrCreateSheet(org.apache.poi.ss.usermodel.Workbook workbook, String sheetName) {
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
            logger.info("Created new sheet: {}", sheetName);
        }
        return sheet;
    }

    private void createToeicHeader(org.apache.poi.ss.usermodel.Sheet sheet) {
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Word", "Part of Speech", "Definition", "Example", "Collocations", "Vietnamese Translation"};
        
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            
            // Create bold style for headers
            org.apache.poi.ss.usermodel.Workbook workbook = sheet.getWorkbook();
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateToeicRow(org.apache.poi.ss.usermodel.Row row, ToeicVocabularyWord word) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Date
        org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(0);
        dateCell.setCellValue(word.getCreatedAt().format(formatter));
        
        // Word
        org.apache.poi.ss.usermodel.Cell wordCell = row.createCell(1);
        wordCell.setCellValue(word.getWord());
        
        // Part of Speech
        org.apache.poi.ss.usermodel.Cell posCell = row.createCell(2);
        posCell.setCellValue(word.getPartOfSpeech() != null ? word.getPartOfSpeech() : "");
        
        // Definition
        org.apache.poi.ss.usermodel.Cell definitionCell = row.createCell(3);
        definitionCell.setCellValue(word.getDefinition() != null ? word.getDefinition() : "");
        
        // Example
        org.apache.poi.ss.usermodel.Cell exampleCell = row.createCell(4);
        exampleCell.setCellValue(word.getExample() != null ? word.getExample() : "");
        
        // Collocations
        org.apache.poi.ss.usermodel.Cell collocationsCell = row.createCell(5);
        if (word.getCollocations() != null && word.getCollocations().length > 0) {
            String collocationsStr = String.join("; ", word.getCollocations());
            collocationsCell.setCellValue(collocationsStr);
        } else {
            collocationsCell.setCellValue("");
        }
        
        // Vietnamese Translation
        org.apache.poi.ss.usermodel.Cell translationCell = row.createCell(6);
        translationCell.setCellValue(word.getVietnameseTranslation() != null ? word.getVietnameseTranslation() : "");
    }

    private ToeicVocabularyWord extractToeicWordFromRow(org.apache.poi.ss.usermodel.Row row) {
        try {
            ToeicVocabularyWord word = new ToeicVocabularyWord();
            
            // Extract data from cells
            org.apache.poi.ss.usermodel.Cell wordCell = row.getCell(1);
            if (wordCell != null && wordCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                word.setWord(wordCell.getStringCellValue().trim());
            } else {
                return null; // Skip if no word
            }
            
            org.apache.poi.ss.usermodel.Cell posCell = row.getCell(2);
            if (posCell != null && posCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                word.setPartOfSpeech(posCell.getStringCellValue().trim());
            }
            
            org.apache.poi.ss.usermodel.Cell definitionCell = row.getCell(3);
            if (definitionCell != null && definitionCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                word.setDefinition(definitionCell.getStringCellValue().trim());
            }
            
            org.apache.poi.ss.usermodel.Cell exampleCell = row.getCell(4);
            if (exampleCell != null && exampleCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                word.setExample(exampleCell.getStringCellValue().trim());
            }
            
            org.apache.poi.ss.usermodel.Cell collocationsCell = row.getCell(5);
            if (collocationsCell != null && collocationsCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                String collocationsStr = collocationsCell.getStringCellValue().trim();
                if (!collocationsStr.isEmpty()) {
                    word.setCollocations(collocationsStr.split(";\\s*"));
                }
            }
            
            org.apache.poi.ss.usermodel.Cell translationCell = row.getCell(6);
            if (translationCell != null && translationCell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                word.setVietnameseTranslation(translationCell.getStringCellValue().trim());
            }
            
            return word;
            
        } catch (Exception e) {
            logger.error("Error extracting TOEIC word from row: {}", e.getMessage(), e);
            return null;
        }
    }
    
}
