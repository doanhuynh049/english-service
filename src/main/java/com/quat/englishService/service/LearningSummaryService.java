package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.LearningSummary;
import com.quat.englishService.dto.JapaneseVocabulary;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing learning summaries and Excel generation
 * Reusable across all learning services (Japanese, TOEIC, Vocabulary, etc.)
 */
@Service
public class LearningSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(LearningSummaryService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${app.learning-summary-excel-path}")
    private String learningExcelPath;

    // AI prompt template for generating summaries
    private static final String SUMMARY_PROMPT_TEMPLATE = """
            You are a language learning tutor for {ServiceType}.
            
            Summarize today's lesson into major knowledge points for a learning summary table.
            
            Lesson content:
            Topic: {Topic}
            Description: {Description}
            Content: {Content}
            
            Requirements:
            1. Extract key knowledge and important points from the lesson.
            2. Provide 2-3 example words or sentences with translations.
            3. Add optional notes or tips if needed.
            
            Format your output strictly as JSON:
            {{
              "KeyKnowledge": "Brief summary of main concepts and grammar points covered",
              "Examples": "Example 1: 私はご飯を食べる (I eat rice), Example 2: 今日は暑い (Today is hot)",
              "Notes": "Important tips or additional context for better understanding"
            }}
            
            Important:
            - JSON must be valid for automatic parsing
            - Keep summaries concise to fit in Excel cells
            - Focus on the most important learning points
            """;

    // AI prompt template for generating vocabulary
    private static final String VOCABULARY_PROMPT_TEMPLATE = """
        You are a Japanese language tutor. Extract and create beginner-friendly vocabulary entries from today's lesson content.

        Lesson content:
        Topic: {Topic}
        Description: {Description}
        Content: {Content}

        Requirements:
        1. Extract 8-12 important vocabulary words suitable for a beginner
        2. For each word, provide complete information in the specified format
        3. Include a mix of different parts of speech (verbs, nouns, adjectives, etc.)
        4. Focus on words essential for understanding the lesson topic
        5. Use simple example sentences appropriate for beginners
        6. Provide romaji to help with pronunciation
        7. Include both English and Vietnamese translations

        Format your output strictly as a JSON array:
        {
        "vocabulary": [
            {
            "wordKanji": "食べる",
            "wordKana": "たべる", 
            "romaji": "taberu",
            "partOfSpeech": "verb",
            "definition": "to eat",
            "vietnamese": "ăn",
            "exampleSentenceJp": "私は寿司を食べる。",
            "exampleSentenceEn": "I eat sushi.",
            "collocations": "食べに行く (go to eat), 食べ物 (food)",
            "synonyms": "なし (none)",
            "confusableWords": "飲む (nomu – to drink)",
            "notes": "Ichidan verb, beginner-friendly"
            }
        ]
        }

        Important:
        - JSON must be valid for automatic parsing
        - Include both kanji and kana readings when applicable
        - Provide simple and practical example sentences
        - List relevant collocations and confusable words
        - Keep notes concise but informative
    """;


    public LearningSummaryService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate learning summary using AI and save to Excel
     * @param day Day number
     * @param phase Learning phase
     * @param topic Lesson topic
     * @param description Lesson description
     * @param content Lesson content
     * @param serviceType Service type (Japanese, TOEIC, etc.)
     * @return Generated LearningSummary
     */
    public LearningSummary generateAndSaveSummary(int day, String phase, String topic, 
                                                  String description, String content, String serviceType) {
        try {
            logger.info("Generating learning summary for Day {} - {}: {}", day, serviceType, topic);

            // Create summary object
            LearningSummary summary = new LearningSummary(day, phase, topic, serviceType);

            // Generate AI summary
            String aiSummary = generateAISummary(topic, description, content, serviceType);
            if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                parseAISummary(summary, aiSummary);
            } else {
                // Fallback if AI fails
                summary.setKeyKnowledge("Key concepts from " + topic);
                summary.setExamples("Examples from lesson content");
                summary.setNotes("Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            // Save to Excel
            saveToExcel(summary);

            logger.info("Successfully generated and saved learning summary for Day {}", day);
            return summary;

        } catch (Exception e) {
            logger.error("Error generating learning summary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate learning summary", e);
        }
    }

    /**
     * Generate AI summary using the prompt template
     */
    private String generateAISummary(String topic, String description, String content, String serviceType) {
        try {
            String prompt = SUMMARY_PROMPT_TEMPLATE
                    .replace("{ServiceType}", serviceType)
                    .replace("{Topic}", topic)
                    .replace("{Description}", description != null ? description : "")
                    .replace("{Content}", content != null ? content : "");

            return geminiClient.generateContent(prompt);
        } catch (Exception e) {
            logger.error("Error generating AI summary: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse AI response and populate summary object
     */
    private void parseAISummary(LearningSummary summary, String aiResponse) {
        try {
            // Clean the response - remove markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(aiResponse);
            
            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(
                cleanedResponse, new TypeReference<Map<String, Object>>() {}
            );

            // Extract data
            summary.setKeyKnowledge((String) responseMap.get("KeyKnowledge"));
            summary.setExamples((String) responseMap.get("Examples"));
            summary.setNotes((String) responseMap.get("Notes"));

        } catch (Exception e) {
            logger.error("Error parsing AI summary response: {}", e.getMessage(), e);
            // Set fallback values
            summary.setKeyKnowledge("Learning summary for " + summary.getTopic());
            summary.setExamples("Examples from lesson");
            summary.setNotes("AI parsing failed - manual review needed");
        }
    }

    /**
     * Clean JSON response by removing markdown code blocks
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "";
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    /**
     * Save learning summary to Excel file
     */
    private void saveToExcel(LearningSummary summary) {
        try {
            File file = new File(learningExcelPath);
            Workbook workbook;
            Sheet sheet;

            // Create or open existing Excel file
            if (file.exists()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(inputStream);
                    sheet = workbook.getSheetAt(0);
                }
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Learning Summary");
                createHeaderRow(workbook, sheet);
                
                // Create vocabulary sheet
                Sheet vocabSheet = workbook.createSheet("Japanese Vocabulary");
                createVocabularyHeaderRow(workbook, vocabSheet);
            }

            // Find or create row for this day and service
            int targetRow = findOrCreateRowForDay(sheet, summary.getDay(), summary.getServiceType());
            
            // Populate row with summary data
            populateRow(workbook, sheet, targetRow, summary);

            // Apply fixed column widths if not already set (for existing files)
            if (sheet.getColumnWidth(0) != 1500) { // Default width, needs setting
                sheet.setColumnWidth(0, 1500);  // Day - narrow
                sheet.setColumnWidth(1, 3000);  // Phase  
                sheet.setColumnWidth(2, 4000);  // Topic
                sheet.setColumnWidth(3, 7000);  // Key Knowledge
                sheet.setColumnWidth(4, 6000);  // Examples
                sheet.setColumnWidth(5, 4000);  // Notes
            }

            // Save the workbook
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Learning summary saved to Excel: {}", learningExcelPath);

        } catch (Exception e) {
            logger.error("Error saving learning summary to Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save learning summary to Excel", e);
        }
    }

    /**
     * Create header row with proper formatting
     */
    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create header cells (removed Service column)
        String[] headers = {"Day", "Phase", "Topic", "Key Knowledge", "Examples", "Notes"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Set optimized column widths for better screen visibility
        sheet.setColumnWidth(0, 1500);  // Day - narrower
        sheet.setColumnWidth(1, 3000);  // Phase
        sheet.setColumnWidth(2, 4000);  // Topic
        sheet.setColumnWidth(3, 7000);  // Key Knowledge - reduced
        sheet.setColumnWidth(4, 6000);  // Examples - reduced
        sheet.setColumnWidth(5, 4000);  // Notes - reduced
    }

    /**
     * Find existing row for day, or create new one (removed service column logic)
     */
    private int findOrCreateRowForDay(Sheet sheet, int day, String serviceType) {
        // Search for existing row with same day
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell dayCell = row.getCell(0);
                if (dayCell != null) {
                    try {
                        int existingDay = (int) dayCell.getNumericCellValue();
                        if (existingDay == day) {
                            return i; // Found existing row
                        }
                    } catch (Exception e) {
                        // Continue searching if cell format is unexpected
                    }
                }
            }
        }

        // Create new row
        int newRowIndex = sheet.getLastRowNum() + 1;
        sheet.createRow(newRowIndex);
        return newRowIndex;
    }

    /**
     * Populate row with learning summary data
     */
    private void populateRow(Workbook workbook, Sheet sheet, int rowIndex, LearningSummary summary) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }

        // Create cell style for data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Populate cells (removed service column)
        createCell(row, 0, summary.getDay(), dataStyle);
        createCell(row, 1, summary.getPhase(), dataStyle);
        createCell(row, 2, summary.getTopic(), dataStyle);
        createCell(row, 3, summary.getKeyKnowledge(), dataStyle);
        createCell(row, 4, summary.getExamples(), dataStyle);
        createCell(row, 5, summary.getNotes(), dataStyle);
    }

    /**
     * Helper method to create and style cells
     */
    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }

    /**
     * Save learning progress for a specific service with summary data
     * @param serviceType Service type (Japanese, TOEIC, etc.)
     * @param summary Learning summary to save
     * @return Path to the Excel file
     */
    public String saveLearningProgress(String serviceType, LearningSummary summary) {
        try {
            logger.info("Saving learning progress for {}: Day {} - {}", serviceType, summary.getDay(), summary.getTopic());
            
            // Set service type if not already set
            if (summary.getServiceType() == null) {
                summary.setServiceType(serviceType);
            }
            
            // Save to Excel
            saveToExcel(summary);
            
            logger.info("Successfully saved learning progress to Excel: {}", learningExcelPath);
            return learningExcelPath;
            
        } catch (Exception e) {
            logger.error("Error saving learning progress: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save learning progress", e);
        }
    }

    /**
     * Get the path to the learning summary Excel file
     */
    public String getExcelFilePath() {
        return learningExcelPath;
    }

    /**
     * Check if Excel file exists
     */
    public boolean excelFileExists() {
        return new File(learningExcelPath).exists();
    }

    /**
     * Generate vocabulary from lesson content using AI
     */
    public List<JapaneseVocabulary> generateVocabulary(String topic, String description, String content, int lessonDay) {
        try {
            logger.info("Generating vocabulary for lesson: {}", topic);

            String prompt = VOCABULARY_PROMPT_TEMPLATE
                    .replace("{Topic}", topic)
                    .replace("{Description}", description != null ? description : "")
                    .replace("{Content}", content != null ? content : "");

            String aiResponse = geminiClient.generateContent(prompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("Empty vocabulary response from AI");
                return new ArrayList<>();
            }

            // Parse AI response
            String cleanedResponse = cleanJsonResponse(aiResponse);
            Map<String, Object> responseMap = objectMapper.readValue(
                cleanedResponse, new TypeReference<Map<String, Object>>() {}
            );

            List<JapaneseVocabulary> vocabularyList = new ArrayList<>();
            Object vocabObj = responseMap.get("vocabulary");
            if (vocabObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> vocabMapList = (List<Map<String, Object>>) vocabObj;
                
                for (Map<String, Object> vocabMap : vocabMapList) {
                    JapaneseVocabulary vocab = new JapaneseVocabulary();
                    vocab.setWordKanji((String) vocabMap.get("wordKanji"));
                    vocab.setWordKana((String) vocabMap.get("wordKana"));
                    vocab.setRomaji((String) vocabMap.get("romaji"));
                    vocab.setPartOfSpeech((String) vocabMap.get("partOfSpeech"));
                    vocab.setDefinition((String) vocabMap.get("definition"));
                    vocab.setVietnamese((String) vocabMap.get("vietnamese"));
                    vocab.setExampleSentenceJp((String) vocabMap.get("exampleSentenceJp"));
                    vocab.setExampleSentenceEn((String) vocabMap.get("exampleSentenceEn"));
                    vocab.setCollocations((String) vocabMap.get("collocations"));
                    vocab.setSynonyms((String) vocabMap.get("synonyms"));
                    vocab.setConfusableWords((String) vocabMap.get("confusableWords"));
                    vocab.setNotes((String) vocabMap.get("notes"));
                    vocab.setLessonDay(lessonDay);
                    vocab.setLessonTopic(topic);
                    
                    vocabularyList.add(vocab);
                }
            }

            logger.info("Generated {} vocabulary entries", vocabularyList.size());
            return vocabularyList;

        } catch (Exception e) {
            logger.error("Error generating vocabulary: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Save vocabulary list to Excel vocabulary sheet
     */
    public void saveVocabularyToExcel(List<JapaneseVocabulary> vocabularyList) {
        if (vocabularyList.isEmpty()) {
            logger.info("No vocabulary to save");
            return;
        }

        try {
            File file = new File(learningExcelPath);
            Workbook workbook;
            Sheet vocabSheet;

            // Open existing file or create new one
            if (file.exists()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(inputStream);
                    if (workbook.getNumberOfSheets() < 2) {
                        vocabSheet = workbook.createSheet("Japanese Vocabulary");
                        createVocabularyHeaderRow(workbook, vocabSheet);
                    } else {
                        vocabSheet = workbook.getSheetAt(1); // Vocabulary sheet
                    }
                }
            } else {
                workbook = new XSSFWorkbook();
                // Create summary sheet first
                Sheet summarySheet = workbook.createSheet("Learning Summary");
                createHeaderRow(workbook, summarySheet);
                // Create vocabulary sheet
                vocabSheet = workbook.createSheet("Japanese Vocabulary");
                createVocabularyHeaderRow(workbook, vocabSheet);
            }

            // Add vocabulary entries
            for (JapaneseVocabulary vocab : vocabularyList) {
                addVocabularyRow(workbook, vocabSheet, vocab);
            }

            // Save the workbook
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Saved {} vocabulary entries to Excel", vocabularyList.size());

        } catch (Exception e) {
            logger.error("Error saving vocabulary to Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save vocabulary to Excel", e);
        }
    }

    /**
     * Create header row for vocabulary sheet
     */
    private void createVocabularyHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Create header cells
        String[] headers = {"ID", "Word (Kanji)", "Word (Kana)", "Romaji", "Part of Speech", 
                           "Definition", "Vietnamese", "Example Sentence (JP)", "Example Sentence (EN)", 
                           "Collocations", "Synonyms", "Confusable Words", "Notes"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Set column widths for vocabulary sheet
        sheet.setColumnWidth(0, 1000);   // ID
        sheet.setColumnWidth(1, 3000);   // Word (Kanji)
        sheet.setColumnWidth(2, 3000);   // Word (Kana)
        sheet.setColumnWidth(3, 2500);   // Romaji
        sheet.setColumnWidth(4, 3000);   // Part of Speech
        sheet.setColumnWidth(5, 4000);   // Definition
        sheet.setColumnWidth(6, 3000);   // Vietnamese
        sheet.setColumnWidth(7, 5000);   // Example Sentence (JP)
        sheet.setColumnWidth(8, 5000);   // Example Sentence (EN)
        sheet.setColumnWidth(9, 4000);   // Collocations
        sheet.setColumnWidth(10, 3000);  // Synonyms
        sheet.setColumnWidth(11, 4000);  // Confusable Words
        sheet.setColumnWidth(12, 3000);  // Notes
    }

    /**
     * Add vocabulary row to the vocabulary sheet
     */
    private void addVocabularyRow(Workbook workbook, Sheet sheet, JapaneseVocabulary vocab) {
        int nextRowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(nextRowNum);

        // Create cell style for data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);
        dataStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Set auto-generated ID
        vocab.setId(nextRowNum);

        // Populate cells
        createCell(row, 0, vocab.getId(), dataStyle);
        createCell(row, 1, vocab.getWordKanji(), dataStyle);
        createCell(row, 2, vocab.getWordKana(), dataStyle);
        createCell(row, 3, vocab.getRomaji(), dataStyle);
        createCell(row, 4, vocab.getPartOfSpeech(), dataStyle);
        createCell(row, 5, vocab.getDefinition(), dataStyle);
        createCell(row, 6, vocab.getVietnamese(), dataStyle);
        createCell(row, 7, vocab.getExampleSentenceJp(), dataStyle);
        createCell(row, 8, vocab.getExampleSentenceEn(), dataStyle);
        createCell(row, 9, vocab.getCollocations(), dataStyle);
        createCell(row, 10, vocab.getSynonyms(), dataStyle);
        createCell(row, 11, vocab.getConfusableWords(), dataStyle);
        createCell(row, 12, vocab.getNotes(), dataStyle);
    }

    /**
     * Get random vocabulary entries for email
     */
    public List<JapaneseVocabulary> getRandomVocabularyForEmail(int count) {
        try {
            File file = new File(learningExcelPath);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            List<JapaneseVocabulary> allVocab = new ArrayList<>();
            
            try (FileInputStream inputStream = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                
                if (workbook.getNumberOfSheets() < 2) {
                    return new ArrayList<>();
                }
                
                Sheet vocabSheet = workbook.getSheetAt(1);
                
                // Read vocabulary entries (skip header row)
                for (int i = 1; i <= vocabSheet.getLastRowNum(); i++) {
                    Row row = vocabSheet.getRow(i);
                    if (row != null) {
                        JapaneseVocabulary vocab = new JapaneseVocabulary();
                        vocab.setId(getCellIntValue(row.getCell(0)));
                        vocab.setWordKanji(getCellStringValue(row.getCell(1)));
                        vocab.setWordKana(getCellStringValue(row.getCell(2)));
                        vocab.setRomaji(getCellStringValue(row.getCell(3)));
                        vocab.setPartOfSpeech(getCellStringValue(row.getCell(4)));
                        vocab.setDefinition(getCellStringValue(row.getCell(5)));
                        vocab.setVietnamese(getCellStringValue(row.getCell(6)));
                        vocab.setExampleSentenceJp(getCellStringValue(row.getCell(7)));
                        vocab.setExampleSentenceEn(getCellStringValue(row.getCell(8)));
                        vocab.setCollocations(getCellStringValue(row.getCell(9)));
                        vocab.setSynonyms(getCellStringValue(row.getCell(10)));
                        vocab.setConfusableWords(getCellStringValue(row.getCell(11)));
                        vocab.setNotes(getCellStringValue(row.getCell(12)));
                        
                        allVocab.add(vocab);
                    }
                }
            }

            // Return random selection
            Collections.shuffle(allVocab);
            return allVocab.stream().limit(count).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting random vocabulary: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to safely get string value from cell
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        try {
            return cell.getStringCellValue();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Helper method to safely get int value from cell
     */
    private int getCellIntValue(Cell cell) {
        if (cell == null) return 0;
        try {
            return (int) cell.getNumericCellValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
