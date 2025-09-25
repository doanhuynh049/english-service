package com.quat.englishService.service;

import com.quat.englishService.model.VocabularyWord;
import com.quat.englishService.dto.ParsedVocabularyWord;
import com.quat.englishService.dto.ToeicVocabularyWord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    @Value("${app.excel-file-path}")
    private String excelFilePath;

    public void saveVocabularyWords(List<VocabularyWord> words) {
        try {
            Workbook workbook = getOrCreateWorkbook();
            Sheet sheet = getOrCreateSheet(workbook, "Vocabulary Log");

            // Create header if sheet is empty
            if (sheet.getPhysicalNumberOfRows() == 0) {
                createHeader(sheet);
            }

            // Add vocabulary words
            int rowNum = sheet.getLastRowNum() + 1;
            for (VocabularyWord word : words) {
                Row row = sheet.createRow(rowNum++);
                populateRow(row, word);
            }

            // Auto-size columns
            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save the workbook
            try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Successfully saved {} vocabulary words to Excel file: {}", words.size(), excelFilePath);

        } catch (Exception e) {
            logger.error("Failed to save vocabulary words to Excel: {}", e.getMessage(), e);
        }
    }

    public void saveVocabularyWordsDetailed(List<ParsedVocabularyWord> words) {
        try {
            Workbook workbook = getOrCreateWorkbook();
            Sheet detailedSheet = getOrCreateSheet(workbook, "Detailed Vocabulary Log");

            // Create detailed header if sheet is empty
            if (detailedSheet.getPhysicalNumberOfRows() == 0) {
                createDetailedHeader(detailedSheet);
            }

            // Add detailed vocabulary words
            int rowNum = detailedSheet.getLastRowNum() + 1;
            for (ParsedVocabularyWord word : words) {
                Row row = detailedSheet.createRow(rowNum++);
                populateDetailedRow(row, word);
            }

            // Auto-size columns
            for (int i = 0; i < 15; i++) {
                detailedSheet.autoSizeColumn(i);
            }

            // Save the workbook
            try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Successfully saved {} detailed vocabulary words to Excel", words.size());

        } catch (Exception e) {
            logger.error("Failed to save detailed vocabulary words to Excel: {}", e.getMessage(), e);
        }
    }

    public void saveToeicVocabularyWords(List<ToeicVocabularyWord> words) {
        try {
            String toeicExcelFilePath = "toeic_vocabulary_log.xlsx";
            Workbook workbook = getToeicWorkbook(toeicExcelFilePath);
            Sheet sheet = getOrCreateSheet(workbook, "TOEIC Vocabulary");

            // Create header if sheet is empty
            if (sheet.getPhysicalNumberOfRows() == 0) {
                createToeicHeader(sheet);
            }

            // Add TOEIC vocabulary words
            int rowNum = sheet.getLastRowNum() + 1;
            for (ToeicVocabularyWord word : words) {
                Row row = sheet.createRow(rowNum++);
                populateToeicRow(row, word);
            }

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Save the workbook
            try (FileOutputStream outputStream = new FileOutputStream(toeicExcelFilePath)) {
                workbook.write(outputStream);
            }

            workbook.close();
            logger.info("Successfully saved {} TOEIC vocabulary words to Excel", words.size());

        } catch (Exception e) {
            logger.error("Failed to save TOEIC vocabulary words to Excel: {}", e.getMessage(), e);
        }
    }

    public Set<String> getUsedWords() {
        Set<String> usedWords = new HashSet<>();

        try {
            File file = new File(excelFilePath);
            if (!file.exists()) {
                logger.info("Excel file doesn't exist yet, no used words to track");
                return usedWords;
            }

            Workbook workbook = getOrCreateWorkbook();

            // Check both sheets for used words
            Sheet basicSheet = workbook.getSheet("Vocabulary Log");
            if (basicSheet != null) {
                extractWordsFromSheet(basicSheet, usedWords, 1); // Word column index 1
            }

            Sheet detailedSheet = workbook.getSheet("Detailed Vocabulary Log");
            if (detailedSheet != null) {
                extractWordsFromSheet(detailedSheet, usedWords, 1); // Word column index 1
            }

            workbook.close();
            logger.info("Found {} previously used words", usedWords.size());

        } catch (Exception e) {
            logger.error("Error reading used words from Excel: {}", e.getMessage(), e);
        }

        return usedWords;
    }

    private void extractWordsFromSheet(Sheet sheet, Set<String> usedWords, int wordColumnIndex) {
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row

            Cell wordCell = row.getCell(wordColumnIndex);
            if (wordCell != null && wordCell.getCellType() == CellType.STRING) {
                String word = wordCell.getStringCellValue().trim().toLowerCase();
                if (!word.isEmpty()) {
                    usedWords.add(word);
                }
            }
        }
    }

    private Workbook getOrCreateWorkbook() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(excelFilePath)) {
            return new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            // File doesn't exist, create a new workbook
            logger.info("Creating new Excel workbook: {}", excelFilePath);
            return new XSSFWorkbook();
        }
    }

    private Workbook getToeicWorkbook(String filePath) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(filePath)) {
            return new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            // File doesn't exist, create a new workbook
            logger.info("Creating new TOEIC Excel workbook: {}", filePath);
            return new XSSFWorkbook();
        }
    }

    private Sheet getOrCreateSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
        }
        return sheet;
    }

    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        // Create header style
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create header cells
        String[] headers = {"Date", "Word", "Explanation"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDetailedHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        // Create header style
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);

        Font whiteFont = sheet.getWorkbook().createFont();
        whiteFont.setBold(true);
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(whiteFont);

        // Create detailed header cells
        String[] headers = {
            "Date", "Word", "Pronunciation", "Part of Speech", "Simple Definition",
            "Advanced Definition", "Example Sentences", "Collocations", "Synonyms",
            "Antonyms", "Confused Words", "Word Family", "Vietnamese Translation",
            "Pronunciation Audio", "Example Audio"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createToeicHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Date", "Word", "Part of Speech", "Definition", "Example", "Collocations", "Vietnamese Translation"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            
            // Create bold style for headers
            Workbook workbook = sheet.getWorkbook();
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateRow(Row row, VocabularyWord word) {
        // Date column
        Cell dateCell = row.createCell(0);
        dateCell.setCellValue(word.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Word column
        Cell wordCell = row.createCell(1);
        wordCell.setCellValue(word.getWord());

        // Explanation column
        Cell explanationCell = row.createCell(2);
        explanationCell.setCellValue(word.getExplanation());

        // Set text wrapping for explanation cell
        CellStyle wrapStyle = row.getSheet().getWorkbook().createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
        explanationCell.setCellStyle(wrapStyle);
    }

    private void populateDetailedRow(Row row, ParsedVocabularyWord word) {
        CellStyle wrapStyle = row.getSheet().getWorkbook().createCellStyle();
        wrapStyle.setWrapText(true);
        wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

        int colIndex = 0;

        // Date
        setCellValue(row, colIndex++, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Word
        setCellValue(row, colIndex++, word.getWord());

        // Pronunciation
        setCellValue(row, colIndex++, word.getPronunciation());

        // Part of Speech
        setCellValue(row, colIndex++, word.getPartOfSpeech());

        // Simple Definition
        setCellValueWithWrap(row, colIndex++, word.getSimpleDefinition(), wrapStyle);

        // Advanced Definition
        setCellValueWithWrap(row, colIndex++, word.getAdvancedDefinition(), wrapStyle);

        // Example Sentences
        String examples = word.getExampleSentences() != null ? String.join("; ", word.getExampleSentences()) : null;
        setCellValueWithWrap(row, colIndex++, examples, wrapStyle);

        // Collocations
        setCellValueWithWrap(row, colIndex++, word.getCollocations(), wrapStyle);

        // Synonyms
        setCellValueWithWrap(row, colIndex++, word.getSynonyms(), wrapStyle);

        // Antonyms
        setCellValueWithWrap(row, colIndex++, word.getAntonyms(), wrapStyle);

        // Confused Words
        setCellValueWithWrap(row, colIndex++, word.getConfusedWords(), wrapStyle);

        // Word Family
        setCellValueWithWrap(row, colIndex++, word.getWordFamily(), wrapStyle);

        // Vietnamese Translation
        setCellValueWithWrap(row, colIndex++, word.getVietnameseTranslation(), wrapStyle);

        // Pronunciation Audio Path
        setCellValue(row, colIndex++, word.getPronunciationAudioPath());

        // Example Audio Path
        setCellValue(row, colIndex++, word.getExampleAudioPath());
    }

    private void populateToeicRow(Row row, ToeicVocabularyWord word) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Date
        Cell dateCell = row.createCell(0);
        dateCell.setCellValue(word.getCreatedAt().format(formatter));
        
        // Word
        Cell wordCell = row.createCell(1);
        wordCell.setCellValue(word.getWord());
        
        // Part of Speech
        Cell posCell = row.createCell(2);
        posCell.setCellValue(word.getPartOfSpeech() != null ? word.getPartOfSpeech() : "");
        
        // Definition
        Cell definitionCell = row.createCell(3);
        definitionCell.setCellValue(word.getDefinition() != null ? word.getDefinition() : "");
        
        // Example
        Cell exampleCell = row.createCell(4);
        exampleCell.setCellValue(word.getExample() != null ? word.getExample() : "");
        
        // Collocations
        Cell collocationsCell = row.createCell(5);
        if (word.getCollocations() != null && word.getCollocations().length > 0) {
            String collocationsStr = String.join("; ", word.getCollocations());
            collocationsCell.setCellValue(collocationsStr);
        } else {
            collocationsCell.setCellValue("");
        }
        
        // Vietnamese Translation
        Cell translationCell = row.createCell(6);
        translationCell.setCellValue(word.getVietnameseTranslation() != null ? word.getVietnameseTranslation() : "");
    }

    private ToeicVocabularyWord extractToeicWordFromRow(Row row) {
        try {
            ToeicVocabularyWord word = new ToeicVocabularyWord();
            
            // Extract data from cells
            Cell wordCell = row.getCell(1);
            if (wordCell != null && wordCell.getCellType() == CellType.STRING) {
                word.setWord(wordCell.getStringCellValue().trim());
            } else {
                return null; // Skip if no word
            }
            
            Cell posCell = row.getCell(2);
            if (posCell != null && posCell.getCellType() == CellType.STRING) {
                word.setPartOfSpeech(posCell.getStringCellValue().trim());
            }
            
            Cell definitionCell = row.getCell(3);
            if (definitionCell != null && definitionCell.getCellType() == CellType.STRING) {
                word.setDefinition(definitionCell.getStringCellValue().trim());
            }
            
            Cell exampleCell = row.getCell(4);
            if (exampleCell != null && exampleCell.getCellType() == CellType.STRING) {
                word.setExample(exampleCell.getStringCellValue().trim());
            }
            
            Cell collocationsCell = row.getCell(5);
            if (collocationsCell != null && collocationsCell.getCellType() == CellType.STRING) {
                String collocationsStr = collocationsCell.getStringCellValue().trim();
                if (!collocationsStr.isEmpty()) {
                    word.setCollocations(collocationsStr.split(";\\s*"));
                }
            }
            
            Cell translationCell = row.getCell(6);
            if (translationCell != null && translationCell.getCellType() == CellType.STRING) {
                word.setVietnameseTranslation(translationCell.getStringCellValue().trim());
            }
            
            return word;
            
        } catch (Exception e) {
            logger.error("Error extracting TOEIC word from row: {}", e.getMessage(), e);
            return null;
        }
    }

    private void setCellValue(Row row, int columnIndex, String value) {
        Cell cell = row.createCell(columnIndex);
        if (value != null && !value.trim().isEmpty()) {
            cell.setCellValue(value);
        }
    }

    private void setCellValueWithWrap(Row row, int columnIndex, String value, CellStyle wrapStyle) {
        Cell cell = row.createCell(columnIndex);
        if (value != null && !value.trim().isEmpty()) {
            cell.setCellValue(value);
            cell.setCellStyle(wrapStyle);
        }
    }
}
