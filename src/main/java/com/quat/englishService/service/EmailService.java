package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
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

    @Value("${app.mail-from}")
    private String fromEmail;

    @Value("${app.mail-to}")
    private String toEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

            // Build word sections
            StringBuilder wordSections = new StringBuilder();
            for (int i = 0; i < vocabularyWords.size(); i++) {
                ParsedVocabularyWord word = vocabularyWords.get(i);
                wordSections.append(buildWordSection(word, i + 1));
            }

            // Replace placeholders
            String content = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
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
}
