package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.JapaneseLesson;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for managing Japanese learning lessons
 * Reads from Excel file, processes with AI, and sends emails
 */
@Service
public class JapaneseLessonService {

    private static final Logger logger = LoggerFactory.getLogger(JapaneseLessonService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    
    private String japaneseExcelFilePath = "Japanese_Foundation.xlsx";

    private static final String LESSON_PROMPT_TEMPLATE = """
            You are a Japanese language tutor.  
            Teach me today's lesson based on the following information:

            Topic: {Topic}  
            Description: {Description}  

            Requirements:
            1. Provide a clear explanation of the lesson (characters/grammar/phrases).  
            2. Show romaji (transliteration), English meaning, and example words/sentences.  
            3. Give me 2–3 practice tasks (writing, speaking, or quiz style).  
            4. Format the response as JSON with the following structure:
            {
              "lessonTitle": "string",
              "contentHtml": "<p>...</p>",
              "examples": ["string", "string"],
              "practiceTasks": ["string", "string"]
            }

            Make sure contentHtml contains well-structured HTML suitable for email display.
            """;

    public JapaneseLessonService(GeminiClient geminiClient, EmailService emailService, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * Main processing method for daily Japanese lesson
     */
    public void processDailyJapaneseLesson() {
        logger.info("Starting daily Japanese lesson processing...");

        try {
            // Step 1: Fetch next open lesson
            JapaneseLesson lesson = fetchNextOpenLesson();
            if (lesson == null) {
                logger.info("No open lessons found in Excel file");
                return;
            }

            logger.info("Processing lesson - Day {}: {}", lesson.getDay(), lesson.getTopic());

            // Step 2: Build AI prompt
            String prompt = buildLessonPrompt(lesson);

            // Step 3: Call AI API
            String aiResponse = geminiClient.generateContent(prompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("Empty response from AI for lesson: {}", lesson.getTopic());
                return;
            }

            // Step 4: Parse AI response
            parseAIResponse(lesson, aiResponse);
            logger.info("Successfully parsed AI response for lesson: {}", lesson.getLessonTitle());

            // Step 5: Generate email content
            String emailContent = buildEmailContent(lesson);

            // Step 6: Send email
            String subject = String.format("[Japanese Lesson - Day %d] %s", lesson.getDay(), lesson.getTopic());
            emailService.sendJapaneseLessonEmail(subject, emailContent);
            logger.info("Japanese lesson email sent successfully");

            // Step 7: Update Excel status
            updateLessonStatus(lesson);
            logger.info("Updated lesson status to Done in Excel");

            logger.info("Daily Japanese lesson processing completed successfully");

        } catch (Exception e) {
            logger.error("Error during daily Japanese lesson processing: {}", e.getMessage(), e);
            throw new RuntimeException("Japanese lesson processing failed", e);
        }
    }

    /**
     * Fetch the next lesson with status "Open" from Excel
     */
    private JapaneseLesson fetchNextOpenLesson() {
        try {
            File file = new File(japaneseExcelFilePath);
            if (!file.exists()) {
                logger.error("Japanese Excel file not found: {}", japaneseExcelFilePath);
                return null;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0); // Assuming first sheet
                if (sheet == null) {
                    logger.error("No sheet found in Japanese Excel file");
                    return null;
                }

                // Find the first row with Status = "Open"
                // New format: Day(A), Phase(B), Topic(C), Description(D), Status(E)
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header row

                    Cell statusCell = row.getCell(4); // Status is now in column E (index 4)
                    if (statusCell != null && "Open".equalsIgnoreCase(statusCell.getStringCellValue().trim())) {
                        // Extract lesson data
                        Cell dayCell = row.getCell(0); // Column A - Day
                        Cell phaseCell = row.getCell(1); // Column B - Phase
                        Cell topicCell = row.getCell(2); // Column C - Topic
                        Cell descriptionCell = row.getCell(3); // Column D - Description

                        if (dayCell != null && phaseCell != null && topicCell != null && descriptionCell != null) {
                            JapaneseLesson lesson = new JapaneseLesson();
                            lesson.setDay((int) dayCell.getNumericCellValue());
                            lesson.setPhase(phaseCell.getStringCellValue().trim());
                            lesson.setTopic(topicCell.getStringCellValue().trim());
                            lesson.setDescription(descriptionCell.getStringCellValue().trim());
                            lesson.setStatus("Open");

                            logger.info("Found open lesson: Day {} - {} - {}", lesson.getDay(), lesson.getPhase(), lesson.getTopic());
                            return lesson;
                        }
                    }
                }

                logger.info("No open lessons found in Excel file");
                return null;

            }
        } catch (Exception e) {
            logger.error("Error fetching next open lesson: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build AI prompt by replacing placeholders
     */
    private String buildLessonPrompt(JapaneseLesson lesson) {
        return LESSON_PROMPT_TEMPLATE
                .replace("{Topic}", lesson.getTopic())
                .replace("{Description}", lesson.getDescription());
    }

    /**
     * Parse AI response and populate lesson object
     */
    private void parseAIResponse(JapaneseLesson lesson, String aiResponse) throws Exception {
        try {
            // Clean the response - remove markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(aiResponse);
            logger.info("Cleaned AI response: {}", cleanedResponse);

            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(
                cleanedResponse, new TypeReference<Map<String, Object>>() {}
            );

            // Extract data
            lesson.setLessonTitle((String) responseMap.get("lessonTitle"));
            lesson.setContentHtml((String) responseMap.get("contentHtml"));

            // Handle examples array
            Object examplesObj = responseMap.get("examples");
            if (examplesObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> examplesList = (java.util.List<String>) examplesObj;
                lesson.setExamples(examplesList.toArray(new String[0]));
            }

            // Handle practice tasks array
            Object tasksObj = responseMap.get("practiceTasks");
            if (tasksObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> tasksList = (java.util.List<String>) tasksObj;
                lesson.setPracticeTasks(tasksList.toArray(new String[0]));
            }

        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            throw new Exception("Failed to parse AI response", e);
        }
    }

    /**
     * Clean JSON response by removing markdown code blocks
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "";
        }
        
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
     * Build email content HTML
     */
    private String buildEmailContent(JapaneseLesson lesson) {
        try {
            // Load the HTML template
            String template = loadEmailTemplate();

            // Build examples section
            StringBuilder examplesHtml = new StringBuilder();
            if (lesson.getExamples() != null) {
                for (String example : lesson.getExamples()) {
                    examplesHtml.append(String.format("""
                        <div class="example-item">
                            <span class="example-bullet">📝</span>
                            <span class="example-text">%s</span>
                        </div>
                        """, escapeHtml(example)));
                }
            }

            // Build practice tasks section
            StringBuilder tasksHtml = new StringBuilder();
            if (lesson.getPracticeTasks() != null) {
                for (String task : lesson.getPracticeTasks()) {
                    tasksHtml.append(String.format("""
                        <div class="task-item">
                            <span class="task-bullet">🎯</span>
                            <span class="task-text">%s</span>
                        </div>
                        """, escapeHtml(task)));
                }
            }

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{DAY}}", String.valueOf(lesson.getDay()))
                .replace("{{PHASE}}", escapeHtml(lesson.getPhase() != null ? lesson.getPhase() : ""))
                .replace("{{TOPIC}}", escapeHtml(lesson.getTopic()))
                .replace("{{LESSON_TITLE}}", escapeHtml(lesson.getLessonTitle()))
                .replace("{{CONTENT_HTML}}", lesson.getContentHtml())
                .replace("{{EXAMPLES}}", examplesHtml.toString())
                .replace("{{PRACTICE_TASKS}}", tasksHtml.toString())
                .replace("{{GENERATION_DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            return content;

        } catch (Exception e) {
            logger.error("Failed to build email content, falling back to simple template", e);
            return buildSimpleEmailContent(lesson);
        }
    }

    /**
     * Load email template from resources
     */
    private String loadEmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/japanese-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("Japanese email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Fallback simple email content
     */
    private String buildSimpleEmailContent(JapaneseLesson lesson) {
        StringBuilder content = new StringBuilder();
        content.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Daily Japanese Lesson</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #e74c3c; color: white; padding: 20px; text-align: center; border-radius: 8px; margin-bottom: 20px;">
                    <h1>🇯🇵 Daily Japanese Lesson</h1>
                    <p>Day %d: %s</p>
                    <p>%s: %s</p>
                    <p>%s</p>
                </div>
                
                <div style="border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                    <h2>%s</h2>
                    %s
                </div>
            """.formatted(
                lesson.getDay(),
                escapeHtml(lesson.getTopic()),
                escapeHtml(lesson.getPhase() != null ? lesson.getPhase() : "Japanese Foundation"),
                escapeHtml(lesson.getTopic()),
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                escapeHtml(lesson.getLessonTitle() != null ? lesson.getLessonTitle() : "Japanese Lesson"),
                lesson.getContentHtml() != null ? lesson.getContentHtml() : "Content not available"
            ));

        // Add examples if available
        if (lesson.getExamples() != null && lesson.getExamples().length > 0) {
            content.append("""
                <div style="border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                    <h3>📝 Examples</h3>
                    <ul>
                """);
            for (String example : lesson.getExamples()) {
                content.append("<li>").append(escapeHtml(example)).append("</li>");
            }
            content.append("</ul></div>");
        }

        // Add practice tasks if available
        if (lesson.getPracticeTasks() != null && lesson.getPracticeTasks().length > 0) {
            content.append("""
                <div style="border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                    <h3>🎯 Practice Tasks</h3>
                    <ol>
                """);
            for (String task : lesson.getPracticeTasks()) {
                content.append("<li>").append(escapeHtml(task)).append("</li>");
            }
            content.append("</ol></div>");
        }

        content.append("""
                <div style="text-align: center; margin-top: 30px; padding: 20px; background: #f8f9fa; border-radius: 8px;">
                    <p><strong>頑張って！ Keep Learning!</strong></p>
                    <p><small>Generated on %s • Powered by Gemini AI</small></p>
                </div>
            </body>
            </html>
            """.formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        return content.toString();
    }

    /**
     * Update lesson status from "Open" to "Done" in Excel
     */
    private void updateLessonStatus(JapaneseLesson lesson) {
        try {
            File file = new File(japaneseExcelFilePath);
            if (!file.exists()) {
                logger.error("Japanese Excel file not found for status update: {}", japaneseExcelFilePath);
                return;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) return;

                // Find the row with matching topic and day
                // New format: Day(A), Phase(B), Topic(C), Description(D), Status(E)
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header row

                    Cell dayCell = row.getCell(0); // Column A - Day
                    Cell topicCell = row.getCell(2); // Column C - Topic
                    Cell statusCell = row.getCell(4); // Column E - Status

                    if (dayCell != null && topicCell != null && statusCell != null) {
                        int rowDay = (int) dayCell.getNumericCellValue();
                        String rowTopic = topicCell.getStringCellValue().trim();

                        if (rowDay == lesson.getDay() && rowTopic.equals(lesson.getTopic())) {
                            // Update status to "Done"
                            statusCell.setCellValue("Done");
                            logger.info("Updated lesson status to Done: Day {} - {} - {}", lesson.getDay(), lesson.getPhase(), lesson.getTopic());
                            break;
                        }
                    }
                }

                // Save the workbook
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    workbook.write(outputStream);
                }

            }
        } catch (Exception e) {
            logger.error("Error updating lesson status: {}", e.getMessage(), e);
        }
    }

    /**
     * HTML escape utility
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * Manual processing method for testing
     */
    public JapaneseLesson processSpecificLesson(String topic, String description, int day) {
        logger.info("Processing specific Japanese lesson: {}", topic);
        
        try {
            JapaneseLesson lesson = new JapaneseLesson();
            lesson.setDay(day);
            lesson.setPhase("Manual Test");
            lesson.setTopic(topic);
            lesson.setDescription(description);
            lesson.setStatus("Open");
            
            // Build AI prompt
            String prompt = buildLessonPrompt(lesson);
            
            // Call AI API
            String aiResponse = geminiClient.generateContent(prompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                throw new RuntimeException("Empty response from AI");
            }
            
            // Parse AI response
            parseAIResponse(lesson, aiResponse);
            
            logger.info("Successfully processed specific lesson: {}", lesson.getLessonTitle());
            return lesson;
            
        } catch (Exception e) {
            logger.error("Error processing specific lesson: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process specific lesson", e);
        }
    }
}
