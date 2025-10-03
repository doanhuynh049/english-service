package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
import com.quat.englishService.dto.ListeningPractice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.File;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final GeminiClient geminiClient;

    @Value("${app.mail-from}")
    private String fromEmail;

    @Value("${app.mail-to}")
    private String toEmail;

    public EmailService(JavaMailSender mailSender, GeminiClient geminiClient) {
        this.mailSender = mailSender;
        this.geminiClient = geminiClient;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendVocabularyEmail(List<ParsedVocabularyWord> vocabularyWords) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("📚 Daily English Vocabulary - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            helper.setText(buildVocabularyEmailContent(vocabularyWords), true);

            // Attach audio files directly to the email
            attachAudioFiles(helper, vocabularyWords);

            mailSender.send(message);
            logger.info("Enhanced vocabulary email sent successfully to {} with {} words and {} audio attachments",
                       toEmail, vocabularyWords.size(), countAudioFiles(vocabularyWords));

        } catch (Exception e) {
            logger.error("Failed to send vocabulary email: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e); // Re-throw for retry mechanism
        }
    }

    public void sendVocabularyEmailWithDocument(List<ParsedVocabularyWord> vocabularyWords, String documentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("📚 Daily English Vocabulary - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            helper.setText(buildVocabularyEmailContent(vocabularyWords), true);

            // Attach audio files directly to the email
            attachAudioFiles(helper, vocabularyWords);

            // Attach monologue transcript document
            if (documentPath != null) {
                attachMonologueDocument(helper, documentPath);
            }

            mailSender.send(message);
            logger.info("Enhanced vocabulary email sent successfully to {} with {} words, {} audio attachments, and transcript document",
                       toEmail, vocabularyWords.size(), countAudioFiles(vocabularyWords));

        } catch (Exception e) {
            logger.error("Failed to send vocabulary email with document: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendToeicListeningEmail(String collocationsContent, List<ToeicListeningService.AudioFileInfo> audioFiles, String passagesFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎧 TOEIC Listening Practice - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            helper.setText(buildToeicEmailContent(collocationsContent), true);

            // Attach audio files
            attachToeicAudioFiles(helper, audioFiles);

            // Attach passages text file
            if (passagesFilePath != null) {
                attachToeicPassagesFile(helper, passagesFilePath);
            }

            mailSender.send(message);
            logger.info("TOEIC Listening email sent successfully to {} with {} audio files and passages document",
                       toEmail, audioFiles.size());

        } catch (Exception e) {
            logger.error("Failed to send TOEIC Listening email: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC Email sending failed", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendToeicListeningEmailWithEml(String collocationsContent, List<ToeicListeningService.AudioFileInfo> audioFiles, 
                                              String passagesFilePath, String emlFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎧 TOEIC Listening Practice - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            helper.setText(buildToeicEmailContent(collocationsContent), true);

            // Attach audio files
            attachToeicAudioFiles(helper, audioFiles);

            // Attach passages text file
            if (passagesFilePath != null) {
                attachToeicPassagesFile(helper, passagesFilePath);
            }

            // Attach collocation history EML file
            if (emlFilePath != null) {
                attachEmlFile(helper, emlFilePath);
            }

            mailSender.send(message);
            logger.info("TOEIC Listening email sent successfully to {} with {} audio files, passages document, and EML attachment",
                       toEmail, audioFiles.size());

        } catch (Exception e) {
            logger.error("Failed to send TOEIC Listening email with EML: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC Email sending failed", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendIeltsReadingEmail(String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("📖 IELTS Reading Practice - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("IELTS Reading email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send IELTS Reading email: {}", e.getMessage(), e);
            throw new RuntimeException("IELTS Reading Email sending failed", e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendToeicEmail(String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlContent = buildToeicPart7EmailContent(content);
            // Format content as HTML for better readability
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("TOEIC Part 7 email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send TOEIC Part 7 email: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC Part 7 Email sending failed", e);
        }
    }

    /**
     * Send TOEIC vocabulary email with HTML content
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendToeicVocabularyEmail(String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("TOEIC vocabulary email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send TOEIC vocabulary email: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC vocabulary email sending failed", e);
        }
    }

    /**
     * Send TOEIC vocabulary email with HTML content and Excel attachment
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendToeicVocabularyEmailWithAttachment(String subject, String htmlContent, String excelFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach Excel file if it exists
            attachToeicExcelFile(helper, excelFilePath);

            mailSender.send(message);
            logger.info("TOEIC vocabulary email with Excel attachment sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send TOEIC vocabulary email with attachment: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC vocabulary email with attachment sending failed", e);
        }
    }

    /**
     * Send Japanese lesson email with HTML content
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendJapaneseLessonEmail(String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Japanese lesson email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Japanese lesson email: {}", e.getMessage(), e);
            throw new RuntimeException("Japanese lesson email sending failed", e);
        }
    }

    /**
     * Send Japanese lesson email with HTML content and Excel attachment
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendJapaneseLessonEmailWithAttachment(String subject, String htmlContent, String excelFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach Excel file if it exists
            attachLearningExcelFile(helper, excelFilePath);

            mailSender.send(message);
            logger.info("Japanese lesson email with Excel attachment sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Japanese lesson email with attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Japanese lesson email with attachment sending failed", e);
        }
    }

    /**
     * Send Japanese lesson email with HTML content, Excel attachment, and audio files
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendJapaneseLessonEmailWithAttachments(String subject, String htmlContent, String excelFilePath, ListeningPractice listeningPractice) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach Excel file if it exists
            attachLearningExcelFile(helper, excelFilePath);

            // Attach audio files if listening practice exists
            if (listeningPractice != null) {
                attachJapaneseAudioFiles(helper, listeningPractice);
            }

            mailSender.send(message);
            logger.info("Japanese lesson email with Excel and audio attachments sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Japanese lesson email with attachments: {}", e.getMessage(), e);
            throw new RuntimeException("Japanese lesson email with attachments sending failed", e);
        }
    }

    /**
     * Send Japanese lesson email with HTML content, Excel attachment, audio files, and vocabulary audio
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendJapaneseLessonEmailWithAttachments(String subject, String htmlContent, String excelFilePath, 
                                                       ListeningPractice listeningPractice, String vocabularyAudioPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach Excel file if it exists
            attachLearningExcelFile(helper, excelFilePath);

            // Attach audio files if listening practice exists
            if (listeningPractice != null) {
                attachJapaneseAudioFiles(helper, listeningPractice);
            }

            // Attach vocabulary audio file if it exists
            if (vocabularyAudioPath != null) {
                attachVocabularyAudioFile(helper, vocabularyAudioPath);
            }

            mailSender.send(message);
            logger.info("Japanese lesson email with Excel, audio, and vocabulary audio attachments sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Japanese lesson email with attachments: {}", e.getMessage(), e);
            throw new RuntimeException("Japanese lesson email with attachments sending failed", e);
        }
    }

    /**
     * Send Thai lesson email with HTML content
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendThaiLessonEmail(String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Thai lesson email sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Thai lesson email: {}", e.getMessage(), e);
            throw new RuntimeException("Thai lesson email sending failed", e);
        }
    }

    /**
     * Send Thai lesson email with HTML content and Excel attachment
     */
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendThaiLessonEmailWithAttachment(String subject, String htmlContent, String excelFilePath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Attach Excel file if it exists
            attachThaiExcelFile(helper, excelFilePath);

            mailSender.send(message);
            logger.info("Thai lesson email with Excel attachment sent successfully to {}", toEmail);

        } catch (Exception e) {
            logger.error("Failed to send Thai lesson email with attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Thai lesson email with attachment sending failed", e);
        }
    }

    private void attachAudioFiles(MimeMessageHelper helper, List<ParsedVocabularyWord> vocabularyWords) {
        for (ParsedVocabularyWord word : vocabularyWords) {
            try {
                // Attach pronunciation audio file
                if (word.getPronunciationAudioPath() != null) {
                    File pronunciationFile = new File(word.getPronunciationAudioPath());
                    if (pronunciationFile.exists() && pronunciationFile.isFile()) {
                        String pronunciationFileName = word.getWord().toLowerCase() + "_pronunciation.mp3";
                        helper.addAttachment(pronunciationFileName, pronunciationFile);
                        logger.debug("Attached pronunciation audio: {}", pronunciationFileName);
                    }
                }

                // Attach example sentence audio file
                if (word.getExampleAudioPath() != null) {
                    File exampleFile = new File(word.getExampleAudioPath());
                    if (exampleFile.exists() && exampleFile.isFile()) {
                        String exampleFileName = word.getWord().toLowerCase() + "_example.mp3";
                        helper.addAttachment(exampleFileName, exampleFile);
                        logger.debug("Attached example audio: {}", exampleFileName);
                    }
                }
            } catch (Exception e) {
                logger.error("Error attaching audio files for word '{}': {}", word.getWord(), e.getMessage(), e);
            }
        }
    }

    private void attachMonologueDocument(MimeMessageHelper helper, String documentPath) {
        try {
            File documentFile = new File(documentPath);
            if (documentFile.exists() && documentFile.isFile()) {
                String fileName = "Vocabulary_Audio_Transcripts_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
                helper.addAttachment(fileName, documentFile);
                logger.info("Attached monologue transcript document: {} ({} bytes)", fileName, documentFile.length());
            } else {
                logger.warn("Monologue document file not found: {}", documentPath);
            }
        } catch (Exception e) {
            logger.error("Error attaching monologue document: {}", e.getMessage(), e);
        }
    }

    private void attachToeicAudioFiles(MimeMessageHelper helper, List<ToeicListeningService.AudioFileInfo> audioFiles) {
        for (ToeicListeningService.AudioFileInfo audioFile : audioFiles) {
            try {
                File file = new File(audioFile.getFilePath());
                if (file.exists() && file.isFile()) {
                    helper.addAttachment(audioFile.getFileName(), file);
                    logger.debug("Attached TOEIC audio: {}", audioFile.getFileName());
                } else {
                    logger.warn("TOEIC audio file not found: {}", audioFile.getFilePath());
                }
            } catch (Exception e) {
                logger.error("Error attaching TOEIC audio file '{}': {}", audioFile.getFileName(), e.getMessage(), e);
            }
        }
    }

    private void attachToeicExcelFile(MimeMessageHelper helper, String excelFilePath) {
        try {
            File excelFile = new File(excelFilePath);
            if (excelFile.exists() && excelFile.isFile()) {
                String fileName = "TOEIC_Vocabulary_History_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
                helper.addAttachment(fileName, excelFile);
                logger.info("Attached TOEIC Excel file: {} ({} bytes)", fileName, excelFile.length());
            } else {
                logger.warn("TOEIC Excel file not found: {}", excelFilePath);
            }
        } catch (Exception e) {
            logger.error("Error attaching TOEIC Excel file: {}", e.getMessage(), e);
        }
    }

    private void attachToeicPassagesFile(MimeMessageHelper helper, String passagesFilePath) {
        try {
            File passagesFile = new File(passagesFilePath);
            if (passagesFile.exists() && passagesFile.isFile()) {
                String fileName = "TOEIC_Listening_Passages_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
                helper.addAttachment(fileName, passagesFile);
                logger.info("Attached TOEIC passages file: {} ({} bytes)", fileName, passagesFile.length());
            } else {
                logger.warn("TOEIC passages file not found: {}", passagesFilePath);
            }
        } catch (Exception e) {
            logger.error("Error attaching TOEIC passages file: {}", e.getMessage(), e);
        }
    }

    private void attachLearningExcelFile(MimeMessageHelper helper, String excelFilePath) {
        try {
            File excelFile = new File(excelFilePath);
            if (excelFile.exists() && excelFile.isFile()) {
                String fileName = "Learning_Summary_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
                helper.addAttachment(fileName, excelFile);
                logger.info("Attached learning summary Excel file: {} ({} bytes)", fileName, excelFile.length());
            } else {
                logger.warn("Learning summary Excel file not found: {}", excelFilePath);
            }
        } catch (Exception e) {
            logger.error("Error attaching learning summary Excel file: {}", e.getMessage(), e);
        }
    }

    private void attachJapaneseAudioFiles(MimeMessageHelper helper, ListeningPractice listeningPractice) {
        int attachmentCount = 0;
        
        try {
            // Attach word audio files
            if (listeningPractice.getWords() != null) {
                for (int i = 0; i < listeningPractice.getWords().size(); i++) {
                    ListeningPractice.Word word = listeningPractice.getWords().get(i);
                    
                    // Attach word pronunciation audio
                    if (word.getWordAudioUrl() != null) {
                        String audioPath = extractAudioPathFromUrl(word.getWordAudioUrl());
                        if (audioPath != null) {
                            File audioFile = new File(audioPath);
                            if (audioFile.exists() && audioFile.isFile()) {
                                String fileName = String.format("%d_%s_pronunciation.mp3", 
                                    i + 1, sanitizeFileName(word.getJapanese()));
                                helper.addAttachment(fileName, audioFile);
                                attachmentCount++;
                                logger.debug("Attached word audio: {} ({} bytes)", fileName, audioFile.length());
                            }
                        }
                    }
                    
                    // Attach word example audio
                    if (word.getExampleAudioUrl() != null) {
                        String audioPath = extractAudioPathFromUrl(word.getExampleAudioUrl());
                        if (audioPath != null) {
                            File audioFile = new File(audioPath);
                            if (audioFile.exists() && audioFile.isFile()) {
                                String fileName = String.format("%d_%s_example.mp3", 
                                    i + 1, sanitizeFileName(word.getJapanese()));
                                helper.addAttachment(fileName, audioFile);
                                attachmentCount++;
                                logger.debug("Attached example audio: {} ({} bytes)", fileName, audioFile.length());
                            }
                        }
                    }
                }
            }
            
            // Attach listening paragraph audio
            if (listeningPractice.getListeningParagraph() != null && 
                listeningPractice.getListeningParagraph().getAudioUrl() != null) {
                String audioPath = extractAudioPathFromUrl(listeningPractice.getListeningParagraph().getAudioUrl());
                if (audioPath != null) {
                    File audioFile = new File(audioPath);
                    if (audioFile.exists() && audioFile.isFile()) {
                        String fileName = "listening_paragraph.mp3";
                        helper.addAttachment(fileName, audioFile);
                        attachmentCount++;
                        logger.debug("Attached paragraph audio: {} ({} bytes)", fileName, audioFile.length());
                    }
                }
            }
            
            logger.info("Attached {} Japanese audio files to email", attachmentCount);
            
        } catch (Exception e) {
            logger.error("Error attaching Japanese audio files: {}", e.getMessage(), e);
        }
    }

    private void attachVocabularyAudioFile(MimeMessageHelper helper, String vocabularyAudioPath) {
        try {
            File audioFile = new File(vocabularyAudioPath);
            if (audioFile.exists() && audioFile.isFile()) {
                String fileName = "vocabulary_review_" + 
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".mp3";
                helper.addAttachment(fileName, audioFile);
                logger.info("Attached vocabulary audio file: {} ({} bytes)", fileName, audioFile.length());
            } else {
                logger.warn("Vocabulary audio file not found: {}", vocabularyAudioPath);
            }
        } catch (Exception e) {
            logger.error("Error attaching vocabulary audio file: {}", e.getMessage(), e);
        }
    }

    private void attachEmlFile(MimeMessageHelper helper, String emlFilePath) {
        try {
            File emlFile = new File(emlFilePath);
            if (emlFile.exists() && emlFile.isFile()) {
                String fileName = "Complete_TOEIC_Collocations_History_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".eml";
                helper.addAttachment(fileName, emlFile);
                logger.info("Attached EML collocation history file: {} ({} bytes)", fileName, emlFile.length());
            } else {
                logger.warn("EML collocation history file not found: {}", emlFilePath);
            }
        } catch (Exception e) {
            logger.error("Error attaching EML collocation history file: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract file path from audio URL
     * Converts http://localhost:8282/audio/2025-10-01/filename.mp3 to actual file path
     */
    private String extractAudioPathFromUrl(String audioUrl) {
        try {
            if (audioUrl == null || !audioUrl.contains("/audio/")) {
                return null;
            }
            
            // Extract the date and filename from URL
            // URL format: http://localhost:8282/audio/2025-10-01/filename.mp3
            String[] parts = audioUrl.split("/audio/");
            if (parts.length < 2) {
                return null;
            }
            
            String pathPart = parts[1]; // "2025-10-01/filename.mp3"
            
            // Construct the full file path
            // Assuming audio files are stored in /tmp/vocabulary-audio/date/filename
            String audioStoragePath = System.getProperty("app.audio.storage-path", "/tmp/vocabulary-audio");
            return audioStoragePath + "/" + pathPart;
            
        } catch (Exception e) {
            logger.warn("Error extracting audio path from URL '{}': {}", audioUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Sanitize filename for safe attachment names
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "japanese";

        // Allow: English letters, digits, dot, underscore, dash, and Japanese ranges
        // \u3040-\u309F  => Hiragana
        // \u30A0-\u30FF  => Katakana
        // \u4E00-\u9FFF  => Common Kanji (CJK Unified Ideographs)
        String sanitized = name.replaceAll("[^a-zA-Z0-9._\\-\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]", "_");

        // If nothing left except underscores → fallback
        if (sanitized.replace("_", "").isEmpty()) {
            return "japanese";
        }

        return sanitized;
    }

    private int countAudioFiles(List<ParsedVocabularyWord> vocabularyWords) {
        int count = 0;
        for (ParsedVocabularyWord word : vocabularyWords) {
            if (word.getPronunciationAudioPath() != null) {
                File file = new File(word.getPronunciationAudioPath());
                if (file.exists()) count++;
            }
            if (word.getExampleAudioPath() != null) {
                File file = new File(word.getExampleAudioPath());
                if (file.exists()) count++;
            }
        }
        return count;
    }

    private String buildVocabularyEmailContent(List<ParsedVocabularyWord> vocabularyWords) {
        try {
            // Load the HTML template
            String template = loadEmailTemplate();

            // Generate AI motivational subtitle
            String subtitle = geminiClient.generateMotivationalSubtitle();

            // Build word sections
            StringBuilder wordSections = new StringBuilder();
            for (int i = 0; i < vocabularyWords.size(); i++) {
                ParsedVocabularyWord word = vocabularyWords.get(i);
                wordSections.append(buildWordSection(word, i + 1));
            }

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{SUBTITLE}}", subtitle)
                .replace("{{WORD_SECTIONS}}", wordSections.toString())
                .replace("{{GENERATION_DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            return content;

        } catch (Exception e) {
            logger.error("Failed to build email content, falling back to simple template", e);
            // Fallback to simple template if template loading fails
            return buildSimpleEmailContent(vocabularyWords);
        }
    }

    private String buildToeicEmailContent(String collocationsContent) {
        try {
            // Load the TOEIC HTML template
            String template = loadToeicEmailTemplate();

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{COLLOCATIONS_CONTENT}}", collocationsContent)
                .replace("{{GENERATION_DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            return content;

        } catch (Exception e) {
            logger.error("Failed to build TOEIC email content, falling back to simple template", e);
            return null;
        }
    }

    private String loadEmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("Email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String buildWordSection(ParsedVocabularyWord word, int index) {
        StringBuilder section = new StringBuilder();

        section.append(String.format("""
            <div class="word-entry">
                <div class="word-number">%d</div>
                
                <!-- Word Header -->
                <div class="word-header">
                    <h2 class="word-title">%s</h2>
                    <div class="word-meta">
                        %s
                        %s
                    </div>
                </div>
                
                """,
                index,
                word.getWord(),
                word.getPronunciation() != null ?
                    String.format("<span class=\"pronunciation\">/%s/</span>", word.getPronunciation()) : "",
                word.getPartOfSpeech() != null ?
                    String.format("<span class=\"part-of-speech\">%s</span>", word.getPartOfSpeech()) : ""
        ));

        // Definitions
        if (word.getSimpleDefinition() != null) {
            section.append(String.format("""
                <div class="content-section">
                    <div class="definition-simple">
                        <div class="section-header">
                            <span class="section-icon">💡</span>
                            <span>Simple Definition</span>
                        </div>
                        <div class="definition-text">%s</div>
                    </div>
                </div>
                """, word.getSimpleDefinition()));
        }

        if (word.getAdvancedDefinition() != null) {
            section.append(String.format("""
                <div class="content-section">
                    <div class="definition-advanced">
                        <div class="section-header">
                            <span class="section-icon">🎓</span>
                            <span>Advanced Definition</span>
                        </div>
                        <div class="definition-text">%s</div>
                    </div>
                </div>
                """, word.getAdvancedDefinition()));
        }

        // Example sentences
        if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) {
            section.append("""
                <div class="content-section">
                    <div class="section-header">
                        <span class="section-icon">📝</span>
                        <span>Example Sentences</span>
                    </div>
                    <div class="examples-list">
                """);
            for (String example : word.getExampleSentences()) {
                section.append(String.format("""
                    <div class="example-item">
                        <span class="example-bullet">▶</span>%s
                    </div>
                    """, example));
            }
            section.append("""
                    </div>
                </div>
                """);
        }

        // Word relations in a grid
        StringBuilder relations = new StringBuilder();

        addRelation(relations, "🔄", "Synonyms", word.getSynonyms());
        addRelation(relations, "⚖️", "Antonyms", word.getAntonyms());
        addRelation(relations, "🤝", "Collocations", word.getCollocations());
        addRelation(relations, "❓", "Confused Words", word.getConfusedWords());
        addRelation(relations, "👨‍👩‍👧‍👦", "Word Family", word.getWordFamily());

        if (relations.length() > 0) {
            section.append("""
                <div class="content-section">
                    <div class="word-relations">
                """);
            section.append(relations);
            section.append("""
                    </div>
                </div>
                """);
        }

        // Vietnamese translation
        if (word.getVietnameseTranslation() != null) {
            section.append(String.format("""
                <div class="content-section">
                    <div class="vietnamese-section">
                        <div class="section-header">
                            <span class="section-icon">🇻🇳</span>
                            <span>Vietnamese Translation</span>
                        </div>
                        <div class="vietnamese-text">%s</div>
                    </div>
                </div>
                """, word.getVietnameseTranslation()));
        }

        section.append("</div>");
        return section.toString();
    }

    private void addRelation(StringBuilder relations, String icon, String title, String content) {
        if (content != null && !content.trim().isEmpty()) {
            relations.append(String.format("""
                <div class="relation-box">
                    <div class="relation-title">%s %s</div>
                    <div class="relation-content">%s</div>
                </div>
                """, icon, title, content));
        }
    }

    private String buildSimpleEmailContent(List<ParsedVocabularyWord> vocabularyWords) {
        StringBuilder content = new StringBuilder();
        content.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Daily English Vocabulary</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #4f46e5; color: white; padding: 20px; text-align: center; border-radius: 8px; margin-bottom: 20px;">
                    <h1>📚 Daily English Vocabulary</h1>
                    <p>""").append(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))).append("""
                    </p>
                </div>
            """);

        for (int i = 0; i < vocabularyWords.size(); i++) {
            ParsedVocabularyWord word = vocabularyWords.get(i);
            content.append(String.format("""
                <div style="border: 1px solid #ddd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                    <h2 style="color: #4f46e5; margin-top: 0;">%d. %s</h2>
                    %s
                    %s
                    %s
                </div>
                """,
                i + 1,
                word.getWord(),
                word.getPronunciation() != null ? "<p><strong>Pronunciation:</strong> /" + word.getPronunciation() + "/</p>" : "",
                word.getSimpleDefinition() != null ? "<p><strong>Definition:</strong> " + word.getSimpleDefinition() + "</p>" : "",
                word.getPartOfSpeech() != null ? "<p><strong>Part of Speech:</strong> " + word.getPartOfSpeech() + "</p>" : ""
            ));
        }

        content.append("""
                <div style="text-align: center; margin-top: 30px; padding: 20px; background: #f8f9fa; border-radius: 8px;">
                    <p><strong>Keep Learning Every Day!</strong></p>
                    <p><small>Generated on """).append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("""
                     • Powered by Gemini AI</small></p>
                </div>
            </body>
            </html>
            """);

        return content.toString();
    }

    private boolean hasAudioFiles(ParsedVocabularyWord word) {
        return (word.getPronunciationAudioPath() != null && new File(word.getPronunciationAudioPath()).exists()) ||
               (word.getExampleAudioPath() != null && new File(word.getExampleAudioPath()).exists());
    }

    private String loadToeicEmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/toeic-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("TOEIC email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private String buildToeicPart7EmailContent(String content) {
        try {
            // Load the TOEIC Part 7 HTML template
            String template = loadToeicPart7EmailTemplate();

            String passageContent = extractSection(content, "Passage:", "Questions:");
            String questionsContent = extractSection(content, "Questions:", "VOCABULARY LEARNING SECTION");
            String vocabularyContent = extractSection(content, "VOCABULARY LEARNING SECTION", null);

            // If parsing fails, use the entire content as passage
            if (passageContent.isEmpty() && questionsContent.isEmpty() && vocabularyContent.isEmpty()) {
                passageContent = content;
                questionsContent = "Questions will be provided with the passage.";
                vocabularyContent = "Vocabulary explanations included in the content above.";
            }

            // Replace placeholders in the template
            String emailContent = template
                    .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                    .replace("{{PASSAGE_CONTENT}}", escapeHtml(passageContent))
                    .replace("{{QUESTIONS_CONTENT}}", escapeHtml(questionsContent))
                    .replace("{{VOCABULARY_CONTENT}}", escapeHtml(vocabularyContent))
                    .replace("{{GENERATION_DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            return emailContent;

        } catch (Exception e) {
            logger.error("Failed to build TOEIC Part 7 email content", e);
            // Fallback to simple HTML format
            return String.format("""
                    <html><body>
                    <h1>TOEIC Part 7 Reading Practice</h1>
                    <pre>%s</pre>
                    </body></html>
                    """, escapeHtml(content));
        }
    }

    private String extractSection(String content, String startMarker, String endMarker) {
        try {
            int startIndex = content.indexOf(startMarker);
            if (startIndex == -1) return "";

            startIndex += startMarker.length();

            int endIndex;
            if (endMarker == null) {
                endIndex = content.length();
            } else {
                endIndex = content.indexOf(endMarker, startIndex);
                if (endIndex == -1) endIndex = content.length();
            }

            return content.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            logger.warn("Error extracting section from {} to {}: {}", startMarker, endMarker, e.getMessage());
            return "";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private String loadToeicPart7EmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/toeic-part7-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("TOEIC Part 7 email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private void attachThaiExcelFile(MimeMessageHelper helper, String excelFilePath) {
        try {
            File excelFile = new File(excelFilePath);
            if (excelFile.exists() && excelFile.isFile()) {
                String fileName = "Thai_Learning_Progress_" +
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
                helper.addAttachment(fileName, excelFile);
                logger.info("Attached Thai learning Excel file: {} ({} bytes)", fileName, excelFile.length());
            } else {
                logger.warn("Thai learning Excel file not found: {}", excelFilePath);
            }
        } catch (Exception e) {
            logger.error("Error attaching Thai learning Excel file: {}", e.getMessage(), e);
        }
    }

}
