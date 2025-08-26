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
import java.util.ArrayList;

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
        StringBuilder content = new StringBuilder();

        content.append(buildEmailHeader());

        for (int i = 0; i < vocabularyWords.size(); i++) {
            ParsedVocabularyWord word = vocabularyWords.get(i);
            content.append(buildWordSection(word, i + 1));
        }

        content.append(buildEmailFooter());

        return content.toString();
    }

    private String buildEmailHeader() {
        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Daily English Vocabulary</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            line-height: 1.6;
                            margin: 0;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: #333;
                        }
                        .container {
                            max-width: 800px;
                            margin: 0 auto;
                            background: white;
                            border-radius: 15px;
                            overflow: hidden;
                            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                            position: relative;
                            overflow: hidden;
                        }
                        .header::before {
                            content: '';
                            position: absolute;
                            top: -50%%;
                            left: -50%%;
                            width: 200%%;
                            height: 200%%;
                            background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
                            background-size: 20px 20px;
                            animation: float 20s ease-in-out infinite;
                        }
                        @keyframes float {
                            0%%, 100%% { transform: translateY(0px) translateX(0px); }
                            50%% { transform: translateY(-10px) translateX(10px); }
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 2.8em;
                            font-weight: 300;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                            position: relative;
                            z-index: 2;
                        }
                        .date {
                            font-size: 1.2em;
                            margin-top: 10px;
                            opacity: 0.9;
                            position: relative;
                            z-index: 2;
                        }
                        .word-section {
                            padding: 35px;
                            border-bottom: 2px solid #f0f0f0;
                            transition: all 0.3s ease;
                            position: relative;
                        }
                        .word-section::before {
                            content: '';
                            position: absolute;
                            left: 0;
                            top: 0;
                            height: 100%%;
                            width: 5px;
                            background: linear-gradient(180deg, #3498db, #8e44ad);
                            transform: scaleY(0);
                            transition: transform 0.3s ease;
                        }
                        .word-section:hover::before {
                            transform: scaleY(1);
                        }
                        .word-section:hover {
                            background: linear-gradient(135deg, #f8f9fa 0%%, #e9ecef 100%%);
                            transform: translateX(10px);
                        }
                        .word-section:last-child {
                            border-bottom: none;
                        }
                        .word-header {
                            display: flex;
                            align-items: center;
                            margin-bottom: 25px;
                            flex-wrap: wrap;
                            gap: 20px;
                        }
                        .word-number-container {
                            position: relative;
                            width: 50px;
                            height: 50px;
                        }
                        .word-number {
                            background: linear-gradient(135deg, #667eea, #764ba2);
                            color: white;
                            width: 50px;
                            height: 50px;
                            border-radius: 50%%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-weight: bold;
                            font-size: 1.4em;
                            position: relative;
                            z-index: 2;
                            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                        }
                        .word-number-shadow {
                            position: absolute;
                            top: 5px;
                            left: 5px;
                            width: 50px;
                            height: 50px;
                            background: rgba(0,0,0,0.1);
                            border-radius: 50%%;
                            z-index: 1;
                        }
                        .word-info {
                            flex: 1;
                        }
                        .word-title {
                            font-size: 2.5em;
                            font-weight: bold;
                            color: #2c3e50;
                            margin: 0 0 10px 0;
                            position: relative;
                            text-shadow: 1px 1px 2px rgba(0,0,0,0.1);
                        }
                        .word-text {
                            position: relative;
                            display: inline-block;
                        }
                        .title-underline {
                            position: absolute;
                            width: 100%%;
                            height: 4px;
                            bottom: -8px;
                            left: 0;
                            background: linear-gradient(135deg, #3498db, #8e44ad);
                            border-radius: 2px;
                            animation: expand 0.8s ease-out;
                        }
                        @keyframes expand {
                            from { width: 0%%; }
                            to { width: 100%%; }
                        }
                        .word-details {
                            display: flex;
                            align-items: center;
                            gap: 15px;
                            flex-wrap: wrap;
                            margin-top: 10px;
                        }
                        .pronunciation {
                            font-family: 'Courier New', monospace;
                            color: #7f8c8d;
                            font-size: 1.2em;
                            background: #f8f9fa;
                            padding: 5px 10px;
                            border-radius: 8px;
                            border: 1px solid #dee2e6;
                        }
                        .pos {
                            background: linear-gradient(135deg, #3498db, #2980b9);
                            color: white;
                            padding: 6px 15px;
                            border-radius: 20px;
                            font-size: 0.9em;
                            font-weight: bold;
                            box-shadow: 0 2px 8px rgba(52, 152, 219, 0.3);
                        }
                        .audio-section {
                            margin-top: 20px;
                            padding: 20px;
                            background: linear-gradient(135deg, #f1f8ff 0%%, #e8f4fd 100%%);
                            border-radius: 12px;
                            border: 2px solid #3498db;
                            position: relative;
                            overflow: hidden;
                        }
                        .audio-section::before {
                            content: '🎵';
                            position: absolute;
                            top: -10px;
                            right: -10px;
                            font-size: 3em;
                            opacity: 0.1;
                            transform: rotate(15deg);
                        }
                        .audio-header {
                            display: flex;
                            align-items: center;
                            margin-bottom: 15px;
                            position: relative;
                            z-index: 2;
                        }
                        .audio-icon {
                            font-size: 1.8em;
                            margin-right: 12px;
                            animation: pulse 2s ease-in-out infinite;
                        }
                        @keyframes pulse {
                            0%%, 100%% { transform: scale(1); }
                            50%% { transform: scale(1.1); }
                        }
                        .audio-title {
                            font-weight: bold;
                            color: #2c3e50;
                            font-size: 1.3em;
                        }
                        .audio-buttons-enhanced {
                            display: flex;
                            gap: 15px;
                            flex-wrap: wrap;
                            margin-bottom: 15px;
                            position: relative;
                            z-index: 2;
                        }
                        .audio-btn-enhanced {
                            background: linear-gradient(135deg, #e74c3c, #c0392b);
                            color: white;
                            padding: 12px 20px;
                            border-radius: 30px;
                            text-decoration: none;
                            font-size: 1em;
                            font-weight: bold;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 15px rgba(231, 76, 60, 0.3);
                            display: inline-flex;
                            align-items: center;
                            gap: 10px;
                            border: none;
                            cursor: pointer;
                            position: relative;
                            overflow: hidden;
                        }
                        .audio-btn-enhanced::before {
                            content: '';
                            position: absolute;
                            top: 0;
                            left: -100%%;
                            width: 100%%;
                            height: 100%%;
                            background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
                            transition: left 0.5s ease;
                        }
                        .audio-btn-enhanced:hover::before {
                            left: 100%%;
                        }
                        .audio-btn-enhanced:hover {
                            background: linear-gradient(135deg, #c0392b, #a93226);
                            transform: translateY(-3px) scale(1.05);
                            box-shadow: 0 8px 25px rgba(231, 76, 60, 0.5);
                        }
                        .pronunciation-btn {
                            background: linear-gradient(135deg, #3498db, #2980b9);
                            box-shadow: 0 4px 15px rgba(52, 152, 219, 0.3);
                        }
                        .pronunciation-btn:hover {
                            background: linear-gradient(135deg, #2980b9, #1f618d);
                            box-shadow: 0 8px 25px rgba(52, 152, 219, 0.5);
                        }
                        .example-btn {
                            background: linear-gradient(135deg, #f39c12, #e67e22);
                            box-shadow: 0 4px 15px rgba(243, 156, 18, 0.3);
                        }
                        .example-btn:hover {
                            background: linear-gradient(135deg, #e67e22, #d35400);
                            box-shadow: 0 8px 25px rgba(243, 156, 18, 0.5);
                        }
                        .audio-attachment-notice {
                            background: linear-gradient(135deg, #e8f5e8, #d4edda);
                            padding: 18px;
                            border-radius: 10px;
                            border-left: 5px solid #28a745;
                            margin-top: 15px;
                            position: relative;
                            z-index: 2;
                        }
                        .notice-header {
                            display: flex;
                            align-items: center;
                            margin-bottom: 12px;
                        }
                        .attachment-icon {
                            font-size: 1.4em;
                            margin-right: 10px;
                        }
                        .notice-title {
                            font-weight: bold;
                            color: #155724;
                            font-size: 1.1em;
                        }
                        .attachment-list {
                            margin: 12px 0;
                        }
                        .attachment-item {
                            display: flex;
                            align-items: center;
                            margin: 10px 0;
                            padding: 10px 15px;
                            background: rgba(255,255,255,0.8);
                            border-radius: 8px;
                            border: 1px solid #c3e6cb;
                            transition: all 0.3s ease;
                        }
                        .attachment-item:hover {
                            background: rgba(255,255,255,1);
                            transform: translateX(5px);
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        }
                        .file-icon {
                            font-size: 1.3em;
                            margin-right: 12px;
                        }
                        .file-name {
                            font-family: 'Courier New', monospace;
                            font-weight: bold;
                            color: #2c3e50;
                            margin-right: 15px;
                            flex: 1;
                        }
                        .file-description {
                            color: #6c757d;
                            font-size: 0.9em;
                            font-style: italic;
                        }
                        .usage-tip {
                            margin-top: 15px;
                            padding: 12px 15px;
                            background: rgba(40, 167, 69, 0.1);
                            border-radius: 8px;
                            font-size: 0.95em;
                            color: #155724;
                            border: 1px solid #28a745;
                        }
                        .definition-section {
                            margin: 20px 0;
                            padding: 15px;
                            background: #f8f9fa;
                            border-radius: 8px;
                            border-left: 4px solid #6c757d;
                        }
                        .definition-label {
                            font-weight: bold;
                            color: #495057;
                            margin-bottom: 8px;
                            font-size: 1.1em;
                        }
                        .definition-content, .section-content {
                            color: #212529;
                            line-height: 1.6;
                        }
                        .examples {
                            margin: 20px 0;
                            padding: 15px;
                            background: #fff3cd;
                            border-radius: 8px;
                            border-left: 4px solid #ffc107;
                        }
                        .example-sentence {
                            margin: 8px 0;
                            padding: 8px 0;
                            color: #856404;
                            font-style: italic;
                        }
                        .vietnamese {
                            margin: 20px 0;
                            padding: 15px;
                            background: #ffeeba;
                            border-radius: 8px;
                            border-left: 4px solid #fd7e14;
                        }
                        .footer {
                            background: linear-gradient(135deg, #2c3e50, #34495e);
                            color: white;
                            padding: 40px;
                            text-align: center;
                            position: relative;
                        }
                        .footer::before {
                            content: '';
                            position: absolute;
                            top: 0;
                            left: 0;
                            right: 0;
                            height: 4px;
                            background: linear-gradient(135deg, #3498db, #8e44ad);
                        }
                        @media (max-width: 600px) {
                            .word-header {
                                flex-direction: column;
                                align-items: flex-start;
                            }
                            .word-title {
                                font-size: 2em;
                            }
                            .audio-buttons-enhanced {
                                width: 100%%;
                                justify-content: center;
                            }
                            .word-section {
                                padding: 20px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📚 Daily English Vocabulary</h1>
                            <div class="date">%s</div>
                        </div>
                """, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
    }

    private String buildWordSection(ParsedVocabularyWord word, int index) {
        StringBuilder section = new StringBuilder();

        // Check which audio files exist
        boolean hasPronunciationAudio = word.getPronunciationAudioPath() != null &&
                                       new File(word.getPronunciationAudioPath()).exists();
        boolean hasExampleAudio = word.getExampleAudioPath() != null &&
                                 new File(word.getExampleAudioPath()).exists();

        section.append(String.format("""
                <div class="word-section">
                    <!-- Decorative Word Header with Enhanced Styling -->
                    <div class="word-header">
                        <div class="word-number-container">
                            <div class="word-number">%d</div>
                            <div class="word-number-shadow"></div>
                        </div>
                        <div class="word-info">
                            <h2 class="word-title">
                                <span class="word-text">%s</span>
                                <div class="title-underline"></div>
                            </h2>
                            <div class="word-details">
                                %s
                                %s
                            </div>
                        </div>
                    </div>
                """,
                index,
                word.getWord(),
                word.getPronunciation() != null ?
                    "<div class=\"pronunciation\">/🔊 " + word.getPronunciation() + "/</div>" : "",
                word.getPartOfSpeech() != null ?
                    "<span class=\"pos\">🏷️ " + word.getPartOfSpeech() + "</span>" : ""
        ));

        // Add definitions
        if (word.getSimpleDefinition() != null) {
            section.append(String.format("""
                    <div class="definition-section">
                        <div class="definition-label">💡 Simple Definition:</div>
                        <div class="definition-content">%s</div>
                    </div>
                    """, word.getSimpleDefinition()));
        }

        if (word.getAdvancedDefinition() != null) {
            section.append(String.format("""
                    <div class="definition-section">
                        <div class="definition-label">🎓 Advanced Definition:</div>
                        <div class="definition-content">%s</div>
                    </div>
                    """, word.getAdvancedDefinition()));
        }

        // Add example sentences
        if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) {
            section.append("""
                    <div class="examples">
                        <div class="definition-label">📝 Example Sentences:</div>
                    """);
            for (String example : word.getExampleSentences()) {
                section.append(String.format("<div class=\"example-sentence\">• %s</div>", example));
            }
            section.append("</div>");
        }

        // Add other sections
        addSection(section, "🤝 Collocations:", word.getCollocations());
        addSection(section, "🔄 Synonyms:", word.getSynonyms());
        addSection(section, "⚖️ Antonyms:", word.getAntonyms());
        addSection(section, "❓ Confused Words:", word.getConfusedWords());
        addSection(section, "👨‍👩‍👧‍👦 Word Family:", word.getWordFamily());

        // Add Vietnamese translation
        if (word.getVietnameseTranslation() != null) {
            section.append(String.format("""
                    <div class="vietnamese">
                        <div class="definition-label">🇻🇳 Vietnamese Translation:</div>
                        %s
                    </div>
                    """, word.getVietnameseTranslation()));
        }

        section.append("</div>");
        return section.toString();
    }

    private boolean hasAudioFiles(ParsedVocabularyWord word) {
        return (word.getPronunciationAudioPath() != null && new File(word.getPronunciationAudioPath()).exists()) ||
               (word.getExampleAudioPath() != null && new File(word.getExampleAudioPath()).exists());
    }

    private String buildEnhancedAudioAttachmentNotice(ParsedVocabularyWord word) {
        if (!hasAudioFiles(word)) {
            return "";
        }

        StringBuilder notice = new StringBuilder();
        notice.append("""
            <div class="audio-attachment-notice">
                <div class="notice-header">
                    <span class="attachment-icon">📎</span>
                    <span class="notice-title">Audio Files Attached</span>
                </div>
                <div class="attachment-list">
            """);

        if (word.getPronunciationAudioPath() != null && new File(word.getPronunciationAudioPath()).exists()) {
            notice.append(String.format("""
                <div class="attachment-item">
                    <span class="file-icon">🎤</span>
                    <span class="file-name">%s_pronunciation.mp3</span>
                    <span class="file-description">Word pronunciation</span>
                </div>
                """, word.getWord().toLowerCase()));
        }

        if (word.getExampleAudioPath() != null && new File(word.getExampleAudioPath()).exists()) {
            notice.append(String.format("""
                <div class="attachment-item">
                    <span class="file-icon">💬</span>
                    <span class="file-name">%s_example.mp3</span>
                    <span class="file-description">Example sentence</span>
                </div>
                """, word.getWord().toLowerCase()));
        }

        notice.append("""
                </div>
                <div class="usage-tip">
                    💡 <strong>How to play:</strong> Click the 🔊 buttons above or find the 📎 attachments in your email client
                </div>
            </div>
            """);

        return notice.toString();
    }

    private void addSection(StringBuilder section, String label, String content) {
        if (content != null && !content.trim().isEmpty()) {
            section.append(String.format("""
                    <div class="definition-section">
                        <div class="definition-label">%s</div>
                        <div class="section-content">%s</div>
                    </div>
                    """, label, content));
        }
    }

    private String buildEmailFooter() {
        return String.format("""
                        <div class="footer">
                            <p>🌟 <strong>Keep Learning Every Day!</strong> 🌟</p>
                            <p>Generated on %s | Powered by Gemini AI & gTTS</p>
                        </div>
                    </div>
                </body>
                </html>
                """, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
