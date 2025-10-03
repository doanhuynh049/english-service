package com.quat.englishService.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.ThaiLesson;
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

/**
 * Service for managing Thai language learning lessons
 * Focuses on speaking and listening skills for beginners
 */
@Service
public class ThaiLearningService {

    private static final Logger logger = LoggerFactory.getLogger(ThaiLearningService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final AudioService audioService;
    
    @Value("${app.thai-excel-file-path}")
    private String thaiExcelFilePath;

    private static final String THAI_LESSON_PROMPT_TEMPLATE = """
            You are a Thai language teacher for beginners who want to focus on **speaking and listening skills (not reading or writing)**.
            Teach me today's lesson based on the following topic: {Topic}

            Requirements:
            1. Provide practical vocabulary (5–8 words) with IPA pronunciation (Thai script optional, only as reference).
            2. Include simple example sentences with IPA so learners can speak immediately.
            3. Create 3–4 listening-focused exercises (tone drills, repeat after teacher, listen & choose meaning).
            4. Create 3–4 speaking exercises (pronunciation drills, shadowing, role-play).
            5. Add 3–5 quiz questions that test listening & speaking comprehension, not reading.
            6. Keep content simple, encouraging, and beginner-friendly.
            7. Focus on practical everyday Thai tourists/beginners need (greetings, food, transport, shopping, asking for help).
            8. Format response as JSON with this structure:

            {
            "lessonTitle": "string - Clear lesson title",
            "contentHtml": "<p>Short intro & overview in HTML</p>",
            "vocabulary": [
                {
                "ipa": "/sa.wàt.diː/",
                "thai": "สวัสดี (optional)",
                "english": "Hello",
                "vietnamese": "Xin chào",
                "example": "sa-wàt-dii khrap/kha – Hello (polite)"
                }
            ],
            "exampleSentences": [
                "sa-wàt-dii khrap, phǒm chʉ̂ʉ John – Hello, my name is John",
                "khun pen yàang-rai bâang khrap – How are you?"
            ],
            "listeningExercises": [
                "Listen and repeat: /sa.wàt.diː/ – focus on the falling tone",
                "Tone recognition: Which word has rising tone, /khǎw/ or /khâw/?",
                "Comprehension: Listen to a greeting dialogue and identify the word for 'Hello'"
            ],
            "speakingExercises": [
                "Pronunciation drill: Say /sa.wàt.diː/ 10 times with correct tone",
                "Shadowing: Repeat after the teacher’s voice slowly, then naturally",
                "Role-play: Introduce yourself in Thai with greeting and your name"
            ],
            "quizQuestions": [
                {
                "question": "Which is the polite way to say 'Hello'?",
                "options": ["sa-wàt-dii khrap/kha", "halo", "wai", "chai"],
                "correctAnswer": "sa-wàt-dii khrap/kha",
                "explanation": "In Thai, use 'khrap' if male, 'kha' if female for politeness."
                }
            ]
            }
            """;

    public ThaiLearningService(GeminiClient geminiClient, EmailService emailService, ObjectMapper objectMapper, AudioService audioService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
        this.audioService = audioService;
    }

    /**
     * Main processing method for daily Thai lesson
     */
    public void processDailyThaiLesson() {
        logger.info("Starting daily Thai lesson processing...");

        try {
            // Step 1: Fetch next open lesson
            ThaiLesson lesson = fetchNextOpenLesson();
            if (lesson == null) {
                logger.info("No open lessons found in Excel file");
                sendErrorEmail("No Thai lessons available", "All lessons completed or no open lessons found.");
                return;
            }

            logger.info("Processing Thai lesson - Day {}: {}", lesson.getDay(), lesson.getTopic());

            // Step 2: Build AI prompt
            String prompt = buildLessonPrompt(lesson);

            // Step 3: Call AI API
            String aiResponse = geminiClient.generateContent(prompt);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                logger.warn("Empty response from AI for lesson: {}", lesson.getTopic());
                sendErrorEmail("AI Service Error", "Failed to generate content for topic: " + lesson.getTopic());
                return;
            }

            // Step 4: Parse AI response
            parseAIResponse(lesson, aiResponse);
            logger.info("Successfully parsed AI response for lesson: {}", lesson.getLessonTitle());

            // Step 5: Generate audio files for vocabulary and listening practice
            generateThaiAudioFiles(lesson);
            logger.info("Generated audio files for Thai lesson");

            // Step 6: Generate and send email
            String emailContent = buildEmailContent(lesson);
            String subject = String.format("[Thai Lesson - Day %d] %s", lesson.getDay(), lesson.getTopic());
            
            // Send email with Excel and audio attachments
            String updatedExcelPath = updateLessonStatus(lesson);
            emailService.sendThaiLessonEmailWithAudioAttachments(subject, emailContent, updatedExcelPath, lesson);
            
            logger.info("Thai lesson email sent successfully");
            logger.info("Daily Thai lesson processing completed successfully");

        } catch (Exception e) {
            logger.error("Error during daily Thai lesson processing: {}", e.getMessage(), e);
            sendErrorEmail("System Error", "An error occurred while processing today's Thai lesson: " + e.getMessage());
        }
    }

    /**
     * Fetch the next lesson with status "Open" from Excel
     */
    private ThaiLesson fetchNextOpenLesson() {
        try {
            File file = new File(thaiExcelFilePath);
            if (!file.exists()) {
                logger.error("Thai Excel file not found: {}", thaiExcelFilePath);
                return null;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    logger.error("No sheet found in Thai Excel file");
                    return null;
                }

                // Find the first row with Status = "Open"
                // Excel format: Day(A), Topic(B), Status(C), Completed Day(D)
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header row

                    Cell dayCell = row.getCell(0);
                    Cell topicCell = row.getCell(1); 
                    Cell statusCell = row.getCell(2);
                    Cell completedDayCell = row.getCell(3);

                    if (statusCell != null && "Open".equalsIgnoreCase(statusCell.getStringCellValue().trim())) {
                        ThaiLesson lesson = new ThaiLesson();
                        
                        // Set day
                        if (dayCell != null) {
                            if (dayCell.getCellType() == CellType.NUMERIC) {
                                lesson.setDay((int) dayCell.getNumericCellValue());
                            } else {
                                lesson.setDay(Integer.parseInt(dayCell.getStringCellValue().trim()));
                            }
                        }
                        
                        // Set topic
                        if (topicCell != null) {
                            lesson.setTopic(topicCell.getStringCellValue().trim());
                        }
                        
                        // Set status
                        lesson.setStatus(statusCell.getStringCellValue().trim());
                        
                        // Set completed day if exists
                        if (completedDayCell != null && completedDayCell.getCellType() == CellType.STRING) {
                            String completedDay = completedDayCell.getStringCellValue().trim();
                            if (!completedDay.isEmpty()) {
                                lesson.setCompletedDay(completedDay);
                            }
                        }
                        
                        logger.info("Found open Thai lesson: Day {} - {}", lesson.getDay(), lesson.getTopic());
                        return lesson;
                    }
                }

                logger.info("No open Thai lessons found in Excel file");
                return null;

            }
        } catch (Exception e) {
            logger.error("Error fetching next open Thai lesson: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build AI prompt by replacing placeholders
     */
    private String buildLessonPrompt(ThaiLesson lesson) {
        return THAI_LESSON_PROMPT_TEMPLATE.replace("{Topic}", lesson.getTopic());
    }

    /**
     * Parse AI response and populate lesson object
     */
    private void parseAIResponse(ThaiLesson lesson, String aiResponse) throws Exception {
        try {
            // Clean the response - remove markdown code blocks if present
            String cleanedResponse = cleanJsonResponse(aiResponse);
            logger.debug("Cleaned AI response: {}", cleanedResponse);

            // Parse JSON response
            Map<String, Object> responseMap = objectMapper.readValue(
                cleanedResponse, new TypeReference<Map<String, Object>>() {}
            );

            // Extract basic fields
            lesson.setLessonTitle((String) responseMap.get("lessonTitle"));
            lesson.setContentHtml((String) responseMap.get("contentHtml"));

            // Parse vocabulary array
            Object vocabObj = responseMap.get("vocabulary");
            if (vocabObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> vocabList = (List<Map<String, Object>>) vocabObj;
                List<ThaiLesson.ThaiVocabulary> vocabularyItems = new ArrayList<>();
                
                for (Map<String, Object> vocabMap : vocabList) {
                    ThaiLesson.ThaiVocabulary vocab = new ThaiLesson.ThaiVocabulary();
                    vocab.setThai((String) vocabMap.get("thai"));
                    vocab.setIpa((String) vocabMap.get("ipa"));
                    vocab.setEnglish((String) vocabMap.get("english"));
                    vocab.setRomanization((String) vocabMap.get("romanization"));
                    vocab.setExampleThai((String) vocabMap.get("exampleThai"));
                    vocab.setExampleIpa((String) vocabMap.get("exampleIpa"));
                    vocab.setExampleEnglish((String) vocabMap.get("exampleEnglish"));
                    vocabularyItems.add(vocab);
                }
                lesson.setVocabulary(vocabularyItems.toArray(new ThaiLesson.ThaiVocabulary[0]));
            }

            // Parse example sentences
            Object examplesObj = responseMap.get("exampleSentences");
            if (examplesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> examplesList = (List<String>) examplesObj;
                lesson.setExampleSentences(examplesList.toArray(new String[0]));
            }

            // Parse listening exercises
            Object listeningObj = responseMap.get("listeningExercises");
            if (listeningObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> listeningList = (List<?>) listeningObj;
                List<ThaiLesson.ThaiExercise> listeningExercises = new ArrayList<>();
                
                for (Object item : listeningList) {
                    ThaiLesson.ThaiExercise exercise = new ThaiLesson.ThaiExercise();
                    exercise.setType("listening");
                    
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> exerciseMap = (Map<String, Object>) item;
                        exercise.setInstruction((String) exerciseMap.get("instruction"));
                        exercise.setThai((String) exerciseMap.get("thai"));
                        exercise.setIpa((String) exerciseMap.get("ipa"));
                        exercise.setEnglish((String) exerciseMap.get("english"));
                        exercise.setAudioHint((String) exerciseMap.get("audioHint"));
                    } else if (item instanceof String) {
                        // Simple string exercise
                        exercise.setInstruction((String) item);
                    }
                    listeningExercises.add(exercise);
                }
                lesson.setListeningExercises(listeningExercises.toArray(new ThaiLesson.ThaiExercise[0]));
            }

            // Parse speaking exercises
            Object speakingObj = responseMap.get("speakingExercises");
            if (speakingObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> speakingList = (List<?>) speakingObj;
                List<ThaiLesson.ThaiExercise> speakingExercises = new ArrayList<>();
                
                for (Object item : speakingList) {
                    ThaiLesson.ThaiExercise exercise = new ThaiLesson.ThaiExercise();
                    exercise.setType("speaking");
                    
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> exerciseMap = (Map<String, Object>) item;
                        exercise.setInstruction((String) exerciseMap.get("instruction"));
                        exercise.setThai((String) exerciseMap.get("thai"));
                        exercise.setIpa((String) exerciseMap.get("ipa"));
                        exercise.setEnglish((String) exerciseMap.get("english"));
                        exercise.setAudioHint((String) exerciseMap.get("audioHint"));
                    } else if (item instanceof String) {
                        // Simple string exercise
                        exercise.setInstruction((String) item);
                    }
                    speakingExercises.add(exercise);
                }
                lesson.setSpeakingExercises(speakingExercises.toArray(new ThaiLesson.ThaiExercise[0]));
            }

            // Parse quiz questions
            Object quizObj = responseMap.get("quizQuestions");
            if (quizObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> quizList = (List<Map<String, Object>>) quizObj;
                List<ThaiLesson.ThaiQuiz> quizQuestions = new ArrayList<>();
                
                for (Map<String, Object> quizMap : quizList) {
                    ThaiLesson.ThaiQuiz quiz = new ThaiLesson.ThaiQuiz();
                    quiz.setQuestion((String) quizMap.get("question"));
                    
                    // Parse correctAnswer as index
                    Object correctAnswerObj = quizMap.get("correctAnswer");
                    if (correctAnswerObj instanceof Number) {
                        quiz.setCorrectAnswer(((Number) correctAnswerObj).intValue());
                    } else if (correctAnswerObj instanceof String) {
                        try {
                            quiz.setCorrectAnswer(Integer.parseInt((String) correctAnswerObj));
                        } catch (NumberFormatException e) {
                            quiz.setCorrectAnswer(0); // Default to first option
                        }
                    }
                    
                    quiz.setExplanation((String) quizMap.get("explanation"));
                    
                    // Parse options
                    Object optionsObj = quizMap.get("options");
                    if (optionsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> optionsList = (List<String>) optionsObj;
                        quiz.setOptions(optionsList.toArray(new String[0]));
                    }
                    
                    quizQuestions.add(quiz);
                }
                lesson.setQuizQuestions(quizQuestions.toArray(new ThaiLesson.ThaiQuiz[0]));
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
    private String buildEmailContent(ThaiLesson lesson) {
        try {
            // Load the HTML template
            String template = loadEmailTemplate();

            // Build vocabulary cards
            String vocabularyCards = buildVocabularyCards(lesson.getVocabulary());

            // Build example sentences
            String exampleSentences = buildExampleSentences(lesson.getExampleSentences());

            // Build listening exercises
            String listeningExercises = buildThaiExercisesList(lesson.getListeningExercises());

            // Build speaking exercises
            String speakingExercises = buildThaiExercisesList(lesson.getSpeakingExercises());

            // Build quiz questions
            String quizQuestions = buildQuizQuestions(lesson.getQuizQuestions());

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{DAY}}", String.valueOf(lesson.getDay()))
                .replace("{{TOPIC}}", escapeHtml(lesson.getTopic()))
                .replace("{{LESSON_TITLE}}", escapeHtml(lesson.getLessonTitle()))
                .replace("{{CONTENT_HTML}}", lesson.getContentHtml() != null ? lesson.getContentHtml() : "")
                .replace("{{VOCABULARY_CARDS}}", vocabularyCards)
                .replace("{{EXAMPLE_SENTENCES}}", exampleSentences)
                .replace("{{LISTENING_EXERCISES}}", listeningExercises)
                .replace("{{SPEAKING_EXERCISES}}", speakingExercises)
                .replace("{{QUIZ_QUESTIONS}}", quizQuestions)
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
        try (var inputStream = getClass().getResourceAsStream("/thai-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("Thai email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Build vocabulary cards HTML
     */
    private String buildVocabularyCards(ThaiLesson.ThaiVocabulary[] vocabulary) {
        if (vocabulary == null || vocabulary.length == 0) {
            return "<p>No vocabulary items available for this lesson.</p>";
        }

        StringBuilder cards = new StringBuilder();
        for (ThaiLesson.ThaiVocabulary vocab : vocabulary) {
            cards.append(String.format("""
                <div class="vocab-card">
                    <div class="thai-word">%s</div>
                    <div class="ipa-pronunciation">%s</div>
                    <div class="romanization">%s</div>
                    <div class="translation">
                        <div class="english">🇺🇸 %s</div>
                    </div>
                    <div class="example">
                        <div class="example-thai">%s</div>
                        <div class="example-english">%s</div>
                    </div>
                </div>
                """,
                escapeHtml(vocab.getThai() != null ? vocab.getThai() : ""),
                escapeHtml(vocab.getIpa() != null ? vocab.getIpa() : ""),
                escapeHtml(vocab.getRomanization() != null ? vocab.getRomanization() : ""),
                escapeHtml(vocab.getEnglish() != null ? vocab.getEnglish() : ""),
                escapeHtml(vocab.getExampleThai() != null ? vocab.getExampleThai() : ""),
                escapeHtml(vocab.getExampleEnglish() != null ? vocab.getExampleEnglish() : "")
            ));
        }
        return cards.toString();
    }

    /**
     * Build example sentences HTML
     */
    private String buildExampleSentences(String[] examples) {
        if (examples == null || examples.length == 0) {
            return "<p>No example sentences available for this lesson.</p>";
        }

        StringBuilder sentences = new StringBuilder();
        sentences.append("<div class=\"example-sentences-list\">");
        for (int i = 0; i < examples.length; i++) {
            sentences.append(String.format("""
                <div class="example-item">
                    <span class="example-number">%d.</span>
                    <span class="example-text">%s</span>
                </div>
                """, i + 1, escapeHtml(examples[i])));
        }
        sentences.append("</div>");
        return sentences.toString();
    }

    /**
     * Build Thai exercises list HTML
     */
    private String buildThaiExercisesList(ThaiLesson.ThaiExercise[] exercises) {
        if (exercises == null || exercises.length == 0) {
            return "<li class=\"exercise-item\">No exercises available for this section.</li>";
        }

        StringBuilder exercisesList = new StringBuilder();
        for (int i = 0; i < exercises.length; i++) {
            ThaiLesson.ThaiExercise exercise = exercises[i];
            exercisesList.append(String.format("""
                <li class="exercise-item">
                    <span class="exercise-number">%d.</span>
                    <div class="exercise-content">
                        <div class="instruction">%s</div>
                        %s
                        %s
                        %s
                    </div>
                </li>
                """, 
                i + 1, 
                escapeHtml(exercise.getInstruction() != null ? exercise.getInstruction() : ""),
                exercise.getThai() != null ? "<div class=\"thai-text\">" + escapeHtml(exercise.getThai()) + "</div>" : "",
                exercise.getIpa() != null ? "<div class=\"ipa-text\">" + escapeHtml(exercise.getIpa()) + "</div>" : "",
                exercise.getEnglish() != null ? "<div class=\"english-text\">" + escapeHtml(exercise.getEnglish()) + "</div>" : ""
            ));
        }
        return exercisesList.toString();
    }

    /**
     * Build quiz questions HTML
     */
    private String buildQuizQuestions(ThaiLesson.ThaiQuiz[] quizQuestions) {
        if (quizQuestions == null || quizQuestions.length == 0) {
            return "<p>No quiz questions available for this lesson.</p>";
        }

        StringBuilder quiz = new StringBuilder();
        for (int i = 0; i < quizQuestions.length; i++) {
            ThaiLesson.ThaiQuiz question = quizQuestions[i];
            quiz.append(String.format("""
                <div class="quiz-container">
                    <div class="quiz-question">%d. %s</div>
                    <ul class="quiz-options">
                """, i + 1, escapeHtml(question.getQuestion())));

            // Add options
            if (question.getOptions() != null) {
                for (String option : question.getOptions()) {
                    quiz.append(String.format("""
                        <li class="quiz-option">%s</li>
                        """, escapeHtml(option)));
                }
            }

            quiz.append(String.format("""
                    </ul>
                    <div class="quiz-answer">
                        <strong>Answer:</strong> %s<br/>
                        <strong>Explanation:</strong> %s
                    </div>
                </div>
                """,
                question.getOptions() != null && question.getCorrectAnswer() >= 0 && question.getCorrectAnswer() < question.getOptions().length ?
                    escapeHtml(question.getOptions()[question.getCorrectAnswer()]) : "Option " + (question.getCorrectAnswer() + 1),
                escapeHtml(question.getExplanation() != null ? question.getExplanation() : "")
            ));
        }
        return quiz.toString();
    }

    /**
     * Update lesson status from "Open" to "Done" and return Excel file path
     */
    private String updateLessonStatus(ThaiLesson lesson) {
        try {
            File file = new File(thaiExcelFilePath);
            if (!file.exists()) {
                logger.error("Thai Excel file not found for status update: {}", thaiExcelFilePath);
                return null;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    logger.error("No sheet found in Thai Excel file for update");
                    return null;
                }

                // Find and update the row
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    Cell dayCell = row.getCell(0);
                    Cell topicCell = row.getCell(1);
                    
                    if (dayCell != null && topicCell != null) {
                        int rowDay = (dayCell.getCellType() == CellType.NUMERIC) ? 
                            (int) dayCell.getNumericCellValue() : 
                            Integer.parseInt(dayCell.getStringCellValue().trim());
                        String rowTopic = topicCell.getStringCellValue().trim();
                        
                        if (rowDay == lesson.getDay() && rowTopic.equals(lesson.getTopic())) {
                            // Update status to "Done"
                            Cell statusCell = row.getCell(2);
                            if (statusCell == null) {
                                statusCell = row.createCell(2);
                            }
                            statusCell.setCellValue("Done");
                            
                            // Update completed day
                            Cell completedDayCell = row.getCell(3);
                            if (completedDayCell == null) {
                                completedDayCell = row.createCell(3);
                            }
                            completedDayCell.setCellValue(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                            
                            logger.info("Updated lesson status: Day {} - {} -> Done", lesson.getDay(), lesson.getTopic());
                            break;
                        }
                    }
                }

                // Save the updated workbook
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    workbook.write(outputStream);
                }
                
                return thaiExcelFilePath;

            }
        } catch (Exception e) {
            logger.error("Error updating lesson status: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send error email when something goes wrong
     */
    private void sendErrorEmail(String errorType, String errorMessage) {
        try {
            String subject = "[Thai Learning Service] " + errorType;
            String content = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: #ff6b6b; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                        <h1>🚨 Thai Learning Service Alert</h1>
                        <p>%s</p>
                    </div>
                    <div style="background: #f8f9fa; padding: 20px; border-radius: 8px;">
                        <h3>Error Details:</h3>
                        <p>%s</p>
                        <p><strong>Time:</strong> %s</p>
                        <p><em>Please check the service logs for more details.</em></p>
                    </div>
                </body>
                </html>
                """, 
                errorType,
                errorMessage,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            emailService.sendThaiLessonEmail(subject, content);
            logger.info("Error email sent successfully");
            
        } catch (Exception e) {
            logger.error("Failed to send error email: {}", e.getMessage(), e);
        }
    }

    /**
     * Fallback simple email content
     */
    private String buildSimpleEmailContent(ThaiLesson lesson) {
        StringBuilder content = new StringBuilder();
        content.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Daily Thai Lesson</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #ff6b6b; color: white; padding: 20px; text-align: center; border-radius: 8px; margin-bottom: 20px;">
                    <h1>🇹🇭 Daily Thai Lesson</h1>
                    <p>Day %d: %s</p>
                    <p>%s</p>
                </div>
                
                <div style="border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                    <h2>%s</h2>
                    %s
                </div>
            """.formatted(
                lesson.getDay(),
                escapeHtml(lesson.getTopic()),
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")),
                escapeHtml(lesson.getLessonTitle() != null ? lesson.getLessonTitle() : "Thai Lesson"),
                lesson.getContentHtml() != null ? lesson.getContentHtml() : "Content not available"
            ));

        content.append("""
                <div style="text-align: center; margin-top: 30px; padding: 20px; background: #f8f9fa; border-radius: 8px;">
                    <p><strong>สู้ ๆ นะ! Keep Learning Thai!</strong></p>
                    <p><small>Generated on %s • Powered by Gemini AI</small></p>
                </div>
            </body>
            </html>
            """.formatted(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        return content.toString();
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
    public ThaiLesson processSpecificLesson(String topic, int day) {
        logger.info("Processing specific Thai lesson: {}", topic);
        
        try {
            ThaiLesson lesson = new ThaiLesson();
            lesson.setDay(day);
            lesson.setTopic(topic);
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
            
            logger.info("Successfully processed specific Thai lesson: {}", lesson.getLessonTitle());
            return lesson;
            
        } catch (Exception e) {
            logger.error("Error processing specific Thai lesson: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process specific Thai lesson", e);
        }
    }

    /**
     * Generate audio files for Thai vocabulary and listening practice
     */
    private void generateThaiAudioFiles(ThaiLesson lesson) {
        try {
            logger.info("Starting audio generation for Thai lesson: {}", lesson.getTopic());

            // Generate audio files for vocabulary words
            if (lesson.getVocabulary() != null && lesson.getVocabulary().length > 0) {
                generateVocabularyAudio(lesson);
            }

            // Generate listening practice audio
            generateListeningPracticeAudio(lesson);

            logger.info("Completed audio generation for Thai lesson");

        } catch (Exception e) {
            logger.error("Error generating Thai audio files: {}", e.getMessage(), e);
            // Don't fail the entire lesson processing, just log the error
        }
    }

    /**
     * Generate audio files for vocabulary words
     */
    private void generateVocabularyAudio(ThaiLesson lesson) {
        try {
            StringBuilder vocabularyScript = new StringBuilder();
            vocabularyScript.append("Thai Vocabulary for Day ").append(lesson.getDay()).append(".\n\n");

            for (ThaiLesson.ThaiVocabulary vocab : lesson.getVocabulary()) {
                if (vocab.getThai() != null && !vocab.getThai().trim().isEmpty()) {
                    
                    // Generate individual pronunciation audio
                    AudioService.AudioInfo audioInfo = audioService.generateAudioFiles(
                        vocab.getThai(), 
                        vocab.getExampleThai() != null ? vocab.getExampleThai() : vocab.getThai()
                    );
                    
                    if (audioInfo != null) {
                        vocab.setPronunciationAudioPath(audioInfo.getPronunciationPath());
                        vocab.setPronunciationAudioUrl(audioInfo.getPronunciationUrl());
                        vocab.setExampleAudioPath(audioInfo.getExamplePath());
                        vocab.setExampleAudioUrl(audioInfo.getExampleUrl());
                        
                        logger.debug("Generated audio for Thai word: {}", vocab.getThai());
                    }

                    // Add to vocabulary script for combined audio
                    vocabularyScript.append(vocab.getThai());
                    if (vocab.getEnglish() != null) {
                        vocabularyScript.append(". ").append(vocab.getEnglish()).append(".\n");
                    }
                    vocabularyScript.append("Example: ");
                    if (vocab.getExampleThai() != null) {
                        vocabularyScript.append(vocab.getExampleThai());
                    }
                    if (vocab.getExampleEnglish() != null) {
                        vocabularyScript.append(". ").append(vocab.getExampleEnglish());
                    }
                    vocabularyScript.append(".\n\n");
                }
            }

            // Generate combined vocabulary audio
            String vocabularyAudioPath = generateCombinedVocabularyAudio(vocabularyScript.toString(), lesson.getDay());
            lesson.setVocabularyAudioPath(vocabularyAudioPath);

            logger.info("Generated vocabulary audio for {} words", lesson.getVocabulary().length);

        } catch (Exception e) {
            logger.error("Error generating vocabulary audio: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate listening practice audio from example sentences and exercises
     */
    private void generateListeningPracticeAudio(ThaiLesson lesson) {
        try {
            StringBuilder listeningScript = new StringBuilder();
            listeningScript.append("Thai Listening Practice for Day ").append(lesson.getDay()).append(".\n\n");

            // Add example sentences
            if (lesson.getExampleSentences() != null && lesson.getExampleSentences().length > 0) {
                listeningScript.append("Example Sentences:\n");
                for (String sentence : lesson.getExampleSentences()) {
                    if (sentence != null && !sentence.trim().isEmpty()) {
                        // Extract Thai text from the sentence (remove English translations)
                        String thaiText = extractThaiText(sentence);
                        listeningScript.append(thaiText).append(".\n");
                    }
                }
                listeningScript.append("\n");
            }

            // Add listening exercises content
            if (lesson.getListeningExercises() != null && lesson.getListeningExercises().length > 0) {
                listeningScript.append("Listening Exercises:\n");
                for (ThaiLesson.ThaiExercise exercise : lesson.getListeningExercises()) {
                    if (exercise.getThai() != null && !exercise.getThai().trim().isEmpty()) {
                        listeningScript.append(exercise.getThai()).append(".\n");
                    }
                }
            }

            // Generate listening practice audio file
            String listeningAudioPath = generateCombinedListeningAudio(listeningScript.toString(), lesson.getDay());
            lesson.setListeningPracticeAudioPath(listeningAudioPath);

            logger.info("Generated listening practice audio for Thai lesson");

        } catch (Exception e) {
            logger.error("Error generating listening practice audio: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate combined vocabulary audio file
     */
    private String generateCombinedVocabularyAudio(String script, int day) {
        try {
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = String.format("thai_vocabulary_day_%d.mp3", day);
            String outputPath = String.format("vocabulary-audio/%s/%s", dateFolder, fileName);

            // Create directory if it doesn't exist
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();

            boolean success = audioService.generateSingleAudio(script, outputPath, "passage", 45);
            
            if (success) {
                logger.info("Generated combined vocabulary audio: {}", outputPath);
                return outputPath;
            } else {
                logger.warn("Failed to generate combined vocabulary audio");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating combined vocabulary audio: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate combined listening practice audio file
     */
    private String generateCombinedListeningAudio(String script, int day) {
        try {
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = String.format("thai_listening_practice_day_%d.mp3", day);
            String outputPath = String.format("vocabulary-audio/%s/%s", dateFolder, fileName);

            // Create directory if it doesn't exist
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();

            boolean success = audioService.generateSingleAudio(script, outputPath, "passage", 60);
            
            if (success) {
                logger.info("Generated listening practice audio: {}", outputPath);
                return outputPath;
            } else {
                logger.warn("Failed to generate listening practice audio");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating listening practice audio: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Extract Thai text from mixed Thai-English sentences
     * Removes English translations and keeps only Thai script
     */
    private String extractThaiText(String mixedText) {
        if (mixedText == null) return "";
        
        // Remove content in parentheses (usually English translations)
        String cleaned = mixedText.replaceAll("\\([^)]*\\)", "");
        
        // Remove common English words and punctuation that might be mixed in
        cleaned = cleaned.replaceAll("\\b[a-zA-Z]+\\b", ""); // Remove English words
        cleaned = cleaned.replaceAll("\\s*-\\s*", " "); // Clean up dashes
        cleaned = cleaned.replaceAll("\\s+", " "); // Normalize whitespace
        cleaned = cleaned.trim();
        
        return cleaned;
    }
}
