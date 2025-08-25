package com.quat.englishService.service;

import com.quat.englishService.model.VocabularyWord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private Workbook getOrCreateWorkbook() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(excelFilePath)) {
            return new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            // File doesn't exist, create a new workbook
            logger.info("Creating new Excel workbook: {}", excelFilePath);
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
}
