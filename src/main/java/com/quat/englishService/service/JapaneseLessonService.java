package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.JapaneseLesson;
import com.quat.englishService.dto.LearningSummary;
import com.quat.englishService.dto.JapaneseVocabulary;
import com.quat.englishService.dto.ListeningPractice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final LearningSummaryService learningSummaryService;
    private final AudioService audioService;
    
    @Value("${app.japanese-excel-file-path}")
    private String japaneseExcelFilePath;

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
              "examples": ["<strong>私はご飯を食べる。</strong><br/>(わたしは ごはんを たべる。 - Watashi wa gohan o taberu.)<br/>I eat rice.", "<strong>今日は暑いです。</strong><br/>(きょうは あついです。 - Kyou wa atsui desu.)<br/>Today is hot."],
              "practiceTasks": ["<strong>Task 1:</strong> Write 3 sentences using hiragana characters you learned today.", "<strong>Task 2:</strong> Practice pronunciation by reading aloud: <br/>• おはよう (ohayou) - Good morning<br/>• ありがとう (arigatou) - Thank you"]
            }

            IMPORTANT formatting guidelines:
            - For examples: Each should contain Japanese text in <strong> tags, followed by romaji/pronunciation in parentheses, then English translation
            - For practiceTasks: Each task should be a single string that can contain HTML formatting
            - Use <strong> tags for emphasis and task titles/numbers  
            - Use <br/> for line breaks within examples and tasks
            - Use bullet points (•) and <br/> for lists instead of <ul>/<li> tags
            - Keep HTML simple and email-compatible
            - Make sure contentHtml contains well-structured HTML suitable for email display
            """;

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            You are a Japanese language tutor. 
            Summarize today's lesson into major knowledge points for a learning summary table.

            Lesson content:
            Topic: {Topic}
            Phase: {Phase}
            Description: {Description}
            Lesson Title: {LessonTitle}
            Content: {Content}

            Requirements:
            1. List key knowledge and important points from the lesson.
            2. Provide 2–3 example words or sentences (Japanese with romaji and English).
            3. Add optional notes or tips if needed.

            Format your output strictly as JSON:
            {
                "keyKnowledge": "Takeaway knowlegde learned (2 sentences)",
                "examples": "Key examples: こんにちは (konnichiwa) - Hello, ありがとう (arigatou) - Thank you",
                "notes": "Additional tips or important notes for retention"
            }

            Important:
            - JSON must be valid for automatic parsing.
            - Summaries should be clear and concise to display in one Excel row.
            - Keep examples short but informative.
            """;

    private static final String LISTENING_PRACTICE_PROMPT_TEMPLATE = """
            You are a Japanese language tutor creating listening practice exercises for beginners.
            
            Based on today's lesson content, create a listening practice exercise:
            
            Lesson Content:
            Topic: {Topic}
            Description: {Description}
            Lesson Title: {LessonTitle}
            Content: {Content}
            
            Requirements:
            1. Extract exactly 3 most important words from the lesson content
            2. For each word, provide complete information suitable for beginners
            3. Create 1 short paragraph (3-5 sentences) using all 3 words naturally
            4. Use simple, beginner-friendly language throughout
            
            Format your output strictly as JSON:
            {
                "words": [
                    {
                        "japanese": "こんにちは",
                        "romaji": "konnichiwa", 
                        "english": "hello",
                        "vietnamese": "xin chào",
                        "exampleSentence": "私(わたし)はこんにちはと言(い)います。",
                        "exampleRomaji": "Watashi wa konnichiwa to iimasu.",
                        "exampleEnglish": "I say hello."
                    }
                ],
                "listeningParagraph": {
                    "japanese": "私はこんにちはと言います。今日はいい天気です。ありがとうございます。",
                    "romaji": "Watashi wa konnichiwa to iimasu. Kyou wa ii tenki desu. Arigatou gozaimasu.",
                    "english": "I say hello. Today is good weather. Thank you very much."
                }
            }
            
            Important:
            - JSON must be valid for automatic parsing
            - Use only words appropriate for beginners
            - Make sure the paragraph sounds natural and coherent
            - Include all 3 words naturally in the paragraph
            - Keep sentences simple and clear for beginners
            """;

    public JapaneseLessonService(GeminiClient geminiClient, EmailService emailService, ObjectMapper objectMapper, LearningSummaryService learningSummaryService, AudioService audioService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.learningSummaryService = learningSummaryService;
        this.audioService = audioService;
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
            Thread.sleep(2000); // Wait for file system to stabilize
            // Step 5: Generate learning summary and save to Excel
            LearningSummary summary = generateLearningSummary(lesson);
            String excelFilePath = learningSummaryService.saveLearningProgress("Japanese", summary);
            logger.info("Generated and saved learning summary to Excel: {}", excelFilePath);
            Thread.sleep(2000); // Wait for file system to stabilize
            // Step 5.5: Generate vocabulary and save to Excel
            List<JapaneseVocabulary> vocabularyList = learningSummaryService.generateVocabulary(
                lesson.getTopic(), lesson.getDescription(), lesson.getContentHtml(), lesson.getDay());
            learningSummaryService.saveVocabularyToExcel(vocabularyList);
            logger.info("Generated and saved {} vocabulary entries to Excel", vocabularyList.size());
            Thread.sleep(2000); // Wait for file system to stabilize
            // Step 5.6: Generate listening practice with audio
            ListeningPractice listeningPractice = generateListeningPractice(lesson);
            lesson.setListeningPractice(listeningPractice);
            logger.info("Generated listening practice with audio files");

            // Step 5.7: Generate vocabulary audio file
            String vocabularyAudioPath = generateVocabularyAudio(vocabularyList);
            lesson.setVocabularyAudioPath(vocabularyAudioPath);
            if (vocabularyAudioPath != null) {
                logger.info("Generated vocabulary audio file: {}", vocabularyAudioPath);
            }

            // Step 6: Generate email content
            String emailContent = buildEmailContent(lesson, vocabularyList);

            // Step 7: Send email with Excel and audio attachments
            String subject = String.format("[Japanese Lesson - Day %d] %s", lesson.getDay(), lesson.getTopic());
            emailService.sendJapaneseLessonEmailWithAttachments(subject, emailContent, excelFilePath, 
                                                               lesson.getListeningPractice(), lesson.getVocabularyAudioPath());
            logger.info("Japanese lesson email sent successfully with Excel, audio, and vocabulary audio attachments");

            // Step 8: Update Excel status
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
     * Build email content HTML with newly generated vocabulary
     */
    private String buildEmailContent(JapaneseLesson lesson, List<JapaneseVocabulary> generatedVocabulary) {
        try {
            // Load the HTML template
            String template = loadEmailTemplate();

            // Build examples section
            StringBuilder examplesHtml = new StringBuilder();
            if (lesson.getExamples() != null) {
                for (String example : lesson.getExamples()) {
                    // Don't escape HTML for examples as they contain formatted content
                    examplesHtml.append(String.format("""
                        <div class="example-item">
                            <span class="example-bullet">📝</span>
                            <span class="example-text">%s</span>
                        </div>
                        """, example));
                }
            }

            // Build practice tasks section
            StringBuilder tasksHtml = new StringBuilder();
            if (lesson.getPracticeTasks() != null) {
                for (String task : lesson.getPracticeTasks()) {
                    // Don't escape HTML for practice tasks as they contain formatted content
                    tasksHtml.append(String.format("""
                        <div class="task-item">
                            <span class="task-bullet">🎯</span>
                            <span class="task-text">%s</span>
                        </div>
                        """, task));
                }
            }

            // Build vocabulary table section - prioritize newly generated vocabulary
            String vocabularyTable = (generatedVocabulary != null && !generatedVocabulary.isEmpty()) 
                ? buildVocabularyTable(generatedVocabulary) 
                : buildVocabularyTable();

            // Build listening practice section
            String listeningPractice = buildListeningPracticeHtml(lesson);

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{DAY}}", String.valueOf(lesson.getDay()))
                .replace("{{PHASE}}", escapeHtml(lesson.getPhase() != null ? lesson.getPhase() : ""))
                .replace("{{TOPIC}}", escapeHtml(lesson.getTopic()))
                .replace("{{LESSON_TITLE}}", escapeHtml(lesson.getLessonTitle()))
                .replace("{{CONTENT_HTML}}", lesson.getContentHtml() != null ? lesson.getContentHtml() : "")
                .replace("{{EXAMPLES}}", examplesHtml.toString())
                .replace("{{PRACTICE_TASKS}}", tasksHtml.toString())
                .replace("{{VOCABULARY_TABLE}}", vocabularyTable)
                .replace("{{LISTENING_PRACTICE}}", listeningPractice)
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
     * Generate learning summary using AI
     */
    private LearningSummary generateLearningSummary(JapaneseLesson lesson) {
        try {
            // Build summary prompt
            String summaryPrompt = SUMMARY_PROMPT_TEMPLATE
                    .replace("{Topic}", lesson.getTopic())
                    .replace("{Phase}", lesson.getPhase() != null ? lesson.getPhase() : "")
                    .replace("{Description}", lesson.getDescription())
                    .replace("{LessonTitle}", lesson.getLessonTitle() != null ? lesson.getLessonTitle() : "")
                    .replace("{Content}", stripHtmlTags(lesson.getContentHtml() != null ? lesson.getContentHtml() : ""));

            // Call AI API for summary
            String aiSummaryResponse = geminiClient.generateContent(summaryPrompt);
            if (aiSummaryResponse == null || aiSummaryResponse.trim().isEmpty()) {
                logger.warn("Empty summary response from AI, creating fallback summary");
                return createFallbackSummary(lesson);
            }

            // Parse AI summary response
            String cleanedSummaryResponse = cleanJsonResponse(aiSummaryResponse);
            Map<String, Object> summaryMap = objectMapper.readValue(
                cleanedSummaryResponse, new TypeReference<Map<String, Object>>() {}
            );

            // Create LearningSummary object
            LearningSummary summary = new LearningSummary();
            summary.setDay(lesson.getDay());
            summary.setPhase(lesson.getPhase());
            summary.setTopic(lesson.getTopic());
            summary.setKeyKnowledge((String) summaryMap.get("keyKnowledge"));
            summary.setExamples((String) summaryMap.get("examples"));
            summary.setNotes((String) summaryMap.get("notes"));

            logger.info("Successfully generated learning summary for Day {}: {}", lesson.getDay(), lesson.getTopic());
            return summary;

        } catch (Exception e) {
            logger.error("Error generating learning summary: {}", e.getMessage(), e);
            return createFallbackSummary(lesson);
        }
    }

    /**
     * Create fallback summary when AI fails
     */
    private LearningSummary createFallbackSummary(JapaneseLesson lesson) {
        LearningSummary summary = new LearningSummary();
        summary.setDay(lesson.getDay());
        summary.setPhase(lesson.getPhase());
        summary.setTopic(lesson.getTopic());
        summary.setKeyKnowledge("Studied " + lesson.getTopic() + " - " + lesson.getDescription());
        summary.setExamples("Examples from lesson content");
        summary.setNotes("Review lesson materials for better understanding");
        return summary;
    }

    /**
     * Strip HTML tags for summary generation
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Build vocabulary table HTML for email
     */
    private String buildVocabularyTable() {
        try {
            List<JapaneseVocabulary> randomVocab = learningSummaryService.getRandomVocabularyForEmail(5);
            
            if (randomVocab.isEmpty()) {
                return "<p>No vocabulary entries available yet. Vocabulary will appear after more lessons.</p>";
            }

            return buildVocabularyTableHtml(randomVocab);

        } catch (Exception e) {
            logger.error("Error building vocabulary table: {}", e.getMessage(), e);
            return "<p>Unable to load vocabulary table.</p>";
        }
    }

    /**
     * Build vocabulary table HTML for email with newly generated vocabulary
     */
    private String buildVocabularyTable(List<JapaneseVocabulary> newVocabulary) {
        try {
            List<JapaneseVocabulary> selectedVocab = new ArrayList<>();
            
            // First, try to select 5 words from the newly generated vocabulary
            if (newVocabulary != null && !newVocabulary.isEmpty()) {
                Collections.shuffle(new ArrayList<>(newVocabulary));
                selectedVocab.addAll(newVocabulary.subList(0, newVocabulary.size()));
                logger.info("Added {} words from newly generated vocabulary", newVocabulary.size());
            }
            
            // If we need more words, get additional ones from existing vocabulary
            List<JapaneseVocabulary> existingVocab = learningSummaryService.getRandomVocabularyForEmail(5);
            
            // Avoid duplicates by checking word kanji
            Set<String> existingKanji = selectedVocab.stream()
                .map(JapaneseVocabulary::getWordKanji)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            existingVocab.stream()
                .filter(vocab -> !existingKanji.contains(vocab.getWordKanji()))
                .limit(5)
                .forEach(selectedVocab::add);
            
            logger.info("Added {} words from existing vocabulary", 
                        Math.min(5, existingVocab.size()));
            
            if (selectedVocab.isEmpty()) {
                return "<p>No vocabulary entries available yet. Vocabulary will appear after more lessons.</p>";
            }

            return buildVocabularyTableHtml(selectedVocab);

        } catch (Exception e) {
            logger.error("Error building vocabulary table: {}", e.getMessage(), e);
            return "<p>Unable to load vocabulary table.</p>";
        }
    }

    /**
     * Helper method to build the actual HTML for vocabulary table
     */
    private String buildVocabularyTableHtml(List<JapaneseVocabulary> vocabularyList) {
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("""
            <div class="vocabulary-table">
                <table>
                    <thead>
                        <tr>
                            <th>Word</th>
                            <th>Reading</th>
                            <th>Meaning</th>
                            <th>Example</th>
                            <th>Notes</th>
                        </tr>
                    </thead>
                    <tbody>
            """);

        for (JapaneseVocabulary vocab : vocabularyList) {
                tableHtml.append(String.format("""
                    <tr>
                        <td>
                            <div class="vocab-kanji">%s</div>
                            <div class="vocab-kana">%s</div>
                            <div class="vocab-romaji">%s</div>
                        </td>
                        <td>
                            <span class="vocab-pos">%s</span>
                        </td>
                        <td>
                            <div><strong>EN:</strong> %s</div>
                            <div><strong>VN:</strong> %s</div>
                        </td>
                        <td>
                            <div class="vocab-example-jp">%s</div>
                            <div class="vocab-example-en">%s</div>
                        </td>
                        <td>%s</td>
                    </tr>
                    """,
                    escapeHtml(vocab.getWordKanji() != null ? vocab.getWordKanji() : ""),
                    escapeHtml(vocab.getWordKana() != null ? vocab.getWordKana() : ""),
                    escapeHtml(vocab.getRomaji() != null ? vocab.getRomaji() : ""),
                    escapeHtml(vocab.getPartOfSpeech() != null ? vocab.getPartOfSpeech() : ""),
                    escapeHtml(vocab.getDefinition() != null ? vocab.getDefinition() : ""),
                    escapeHtml(vocab.getVietnamese() != null ? vocab.getVietnamese() : ""),
                    escapeHtml(vocab.getExampleSentenceJp() != null ? vocab.getExampleSentenceJp() : ""),
                    escapeHtml(vocab.getExampleSentenceEn() != null ? vocab.getExampleSentenceEn() : ""),
                    escapeHtml(vocab.getNotes() != null ? vocab.getNotes() : "")
                ));
            }

        tableHtml.append("""
                    </tbody>
                </table>
            </div>
            """);

        return tableHtml.toString();
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

    /**
     * Generate listening practice content and audio files
     */
    private ListeningPractice generateListeningPractice(JapaneseLesson lesson) {
        try {
            logger.info("Generating listening practice for lesson: {}", lesson.getTopic());
            
            // Build listening practice prompt
            String listeningPrompt = LISTENING_PRACTICE_PROMPT_TEMPLATE
                    .replace("{Topic}", lesson.getTopic())
                    .replace("{Description}", lesson.getDescription())
                    .replace("{LessonTitle}", lesson.getLessonTitle() != null ? lesson.getLessonTitle() : "")
                    .replace("{Content}", stripHtmlTags(lesson.getContentHtml() != null ? lesson.getContentHtml() : ""));

            // Call AI API for listening practice
            String aiResponse = geminiClient.generateContent(listeningPrompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("Empty listening practice response from AI");
                return null;
            }

            // Parse AI response
            String cleanedResponse = cleanJsonResponse(aiResponse);
            Map<String, Object> responseMap = objectMapper.readValue(
                cleanedResponse, new TypeReference<Map<String, Object>>() {}
            );

            ListeningPractice practice = new ListeningPractice();
            
            // Parse words
            Object wordsObj = responseMap.get("words");
            if (wordsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> wordsList = (List<Map<String, Object>>) wordsObj;
                List<ListeningPractice.Word> words = new ArrayList<>();
                
                for (Map<String, Object> wordMap : wordsList) {
                    ListeningPractice.Word word = new ListeningPractice.Word();
                    word.setJapanese((String) wordMap.get("japanese"));
                    word.setRomaji((String) wordMap.get("romaji"));
                    word.setEnglish((String) wordMap.get("english"));
                    word.setVietnamese((String) wordMap.get("vietnamese"));
                    word.setExampleSentence((String) wordMap.get("exampleSentence"));
                    word.setExampleRomaji((String) wordMap.get("exampleRomaji"));
                    word.setExampleEnglish((String) wordMap.get("exampleEnglish"));
                    words.add(word);
                }
                practice.setWords(words);
            }
            
            // Parse listening paragraph
            Object paragraphObj = responseMap.get("listeningParagraph");
            if (paragraphObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paragraphMap = (Map<String, Object>) paragraphObj;
                ListeningPractice.Paragraph paragraph = new ListeningPractice.Paragraph();
                paragraph.setJapanese((String) paragraphMap.get("japanese"));
                paragraph.setRomaji((String) paragraphMap.get("romaji"));
                paragraph.setEnglish((String) paragraphMap.get("english"));
                practice.setListeningParagraph(paragraph);
            }

            // Generate audio files asynchronously
            generateAudioFilesAsync(practice);
            
            logger.info("Successfully generated listening practice with {} words", 
                       practice.getWords() != null ? practice.getWords().size() : 0);
            return practice;

        } catch (Exception e) {
            logger.error("Error generating listening practice: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate audio files for listening practice asynchronously
     */
    private void generateAudioFilesAsync(ListeningPractice practice) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Generate audio for each word
        if (practice.getWords() != null) {
            for (ListeningPractice.Word word : practice.getWords()) {
                // Generate audio for word pronunciation
                CompletableFuture<Void> wordAudioFuture = CompletableFuture.runAsync(() -> {
                    try {
                        AudioService.AudioInfo audioInfo = audioService.generateAudioFiles(
                            word.getJapanese(), word.getExampleSentence());
                        if (audioInfo != null) {
                            word.setWordAudioUrl(audioInfo.getPronunciationUrl());
                            word.setExampleAudioUrl(audioInfo.getExampleUrl());
                            logger.debug("Generated audio for word: {}", word.getJapanese());
                        }
                    } catch (Exception e) {
                        logger.error("Error generating audio for word '{}': {}", word.getJapanese(), e.getMessage());
                    }
                }, executor);
                futures.add(wordAudioFuture);
            }
        }

        // Generate audio for listening paragraph
        if (practice.getListeningParagraph() != null) {
            CompletableFuture<Void> paragraphAudioFuture = CompletableFuture.runAsync(() -> {
                try {
                    String paragraphText = practice.getListeningParagraph().getJapanese();
                    AudioService.AudioInfo audioInfo = audioService.generateAudioFiles(
                        "listening_paragraph", paragraphText);
                    if (audioInfo != null) {
                        practice.getListeningParagraph().setAudioUrl(audioInfo.getExampleUrl());
                        logger.debug("Generated audio for listening paragraph");
                    }
                } catch (Exception e) {
                    logger.error("Error generating audio for listening paragraph: {}", e.getMessage());
                }
            }, executor);
            futures.add(paragraphAudioFuture);
        }

        // Wait for all audio generation to complete (with timeout)
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.get(60, java.util.concurrent.TimeUnit.SECONDS); // 60 second timeout
            logger.info("All audio files generated successfully");
        } catch (Exception e) {
            logger.warn("Some audio files may not have been generated: {}", e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Build listening practice HTML for email template
     */
    private String buildListeningPracticeHtml(JapaneseLesson lesson) {
        try {
            ListeningPractice practice = lesson.getListeningPractice();
            if (practice == null) {
                return "<p>No listening practice available for this lesson.</p>";
            }

            StringBuilder html = new StringBuilder();

            // Build words section
            if (practice.getWords() != null && !practice.getWords().isEmpty()) {
                html.append("<div class=\"listening-words\">");
                
                for (ListeningPractice.Word word : practice.getWords()) {
                    html.append(String.format("""
                        <div class="listening-word">
                            <div class="word-header">
                                <div>
                                    <div class="word-japanese">%s</div>
                                    <div class="word-romaji">%s</div>
                                </div>
                            </div>
                            <div class="word-meanings">
                                <div class="word-english"><strong>EN:</strong> %s</div>
                                <div class="word-vietnamese"><strong>VN:</strong> %s</div>
                            </div>
                            <div class="word-example">
                                <div class="example-japanese">%s</div>
                                <div class="example-romaji">%s</div>
                                <div class="example-english">%s</div>
                                <div class="audio-controls">
                                    %s
                                    %s
                                </div>
                            </div>
                        </div>
                        """,
                        escapeHtml(word.getJapanese() != null ? word.getJapanese() : ""),
                        escapeHtml(word.getRomaji() != null ? word.getRomaji() : ""),
                        escapeHtml(word.getEnglish() != null ? word.getEnglish() : ""),
                        escapeHtml(word.getVietnamese() != null ? word.getVietnamese() : ""),
                        escapeHtml(word.getExampleSentence() != null ? word.getExampleSentence() : ""),
                        escapeHtml(word.getExampleRomaji() != null ? word.getExampleRomaji() : ""),
                        escapeHtml(word.getExampleEnglish() != null ? word.getExampleEnglish() : ""),
                        word.getWordAudioUrl() != null ? 
                            String.format("<a href=\"%s\" class=\"audio-button\">🔊 Word</a>", word.getWordAudioUrl()) : "",
                        word.getExampleAudioUrl() != null ? 
                            String.format("<a href=\"%s\" class=\"audio-button\">🔊 Example</a>", word.getExampleAudioUrl()) : ""
                    ));
                }
                
                html.append("</div>");
            }

            // Build listening paragraph section
            if (practice.getListeningParagraph() != null) {
                ListeningPractice.Paragraph paragraph = practice.getListeningParagraph();
                html.append(String.format("""
                    <div class="listening-paragraph">
                        <div class="paragraph-header">
                            <div class="paragraph-title">🎧 Listening Practice</div>
                            %s
                        </div>
                        <div class="paragraph-japanese">%s</div>
                        <div class="paragraph-romaji">%s</div>
                        <div class="paragraph-english">%s</div>
                    </div>
                    """,
                    paragraph.getAudioUrl() != null ? 
                        String.format("<a href=\"%s\" class=\"audio-button\">🔊 Play Audio</a>", paragraph.getAudioUrl()) : "",
                    escapeHtml(paragraph.getJapanese() != null ? paragraph.getJapanese() : ""),
                    escapeHtml(paragraph.getRomaji() != null ? paragraph.getRomaji() : ""),
                    escapeHtml(paragraph.getEnglish() != null ? paragraph.getEnglish() : "")
                ));
            }

            return html.toString();

        } catch (Exception e) {
            logger.error("Error building listening practice HTML: {}", e.getMessage(), e);
            return "<p>Unable to load listening practice content.</p>";
        }
    }

    /**
     * Generate vocabulary audio file that reads all words and their examples
     * Format: Word -> 2s pause -> Example -> 5s pause -> Next word
     */
    private String generateVocabularyAudio(List<JapaneseVocabulary> vocabularyList) {
        if (vocabularyList == null || vocabularyList.isEmpty()) {
            logger.info("No vocabulary to generate audio for");
            return null;
        }

        try {
            // Build the vocabulary reading script with proper timing
            StringBuilder script = new StringBuilder();
            script.append("Let's review today's key vocabulary words. ");
            
            for (int i = 0; i < vocabularyList.size(); i++) {
                JapaneseVocabulary vocab = vocabularyList.get(i);
                
                // Announce word number
                script.append("Word number ").append(i + 1).append(". ");
                
                // Read the Japanese word (prefer Kanji, fallback to Kana)
                String japaneseWord = "";
                if (vocab.getWordKanji() != null && !vocab.getWordKanji().trim().isEmpty()) {
                    japaneseWord = vocab.getWordKanji();
                    script.append(japaneseWord);
                } else if (vocab.getWordKana() != null && !vocab.getWordKana().trim().isEmpty()) {
                    japaneseWord = vocab.getWordKana();
                    script.append(japaneseWord);
                }
                
                // // Add English meaning
                // if (vocab.getDefinition() != null && !vocab.getDefinition().trim().isEmpty()) {
                //     script.append(". Meaning: ").append(vocab.getDefinition()).append(". ");
                // }
                
                // Add 2-second pause (represented by dots and spaces)
                script.append("... ... ");
                
                // Read example sentence if available
                if (vocab.getExampleSentenceJp() != null && !vocab.getExampleSentenceJp().trim().isEmpty()) {
                    script.append("Example: ").append(vocab.getExampleSentenceJp());
                    
                    // // Add English translation of example
                    // if (vocab.getExampleSentenceEn() != null && !vocab.getExampleSentenceEn().trim().isEmpty()) {
                    //     script.append(". In English: ").append(vocab.getExampleSentenceEn());
                    // }
                    script.append(". ");
                } else {
                    script.append("No example available for this word. ");
                }
                
                // Add 5-second pause between words (longer pause)
                script.append("... ... ... ... ... ... ... ... ... ... ");
            }
            
            script.append("That completes today's vocabulary review with ")
                  .append(vocabularyList.size())
                  .append(" words. Remember to practice these words in your daily conversations. Good luck with your Japanese studies!");
            
            // Generate audio file using AudioService
            String vocabularyText = script.toString();
            logger.info("Generated vocabulary script with {} characters for {} words", vocabularyText.length(), vocabularyList.size());
            
            AudioService.AudioInfo audioInfo = audioService.generateAudioFilesWithMonologue(
                "vocabulary_review", vocabularyText);
            
            if (audioInfo != null) {
                logger.info("Successfully generated vocabulary audio file: {} words", vocabularyList.size());
                return audioInfo.getExamplePath(); // Use example path for the vocabulary file
            } else {
                logger.error("Failed to generate vocabulary audio file");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error generating vocabulary audio: {}", e.getMessage(), e);
            return null;
        }
    }
}
