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

            mailSender.send(message);
            logger.info("Enhanced vocabulary email sent successfully to {} with {} words", toEmail, vocabularyWords.size());

        } catch (Exception e) {
            logger.error("Failed to send vocabulary email: {}", e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e); // Re-throw for retry mechanism
        }
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
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 2.5em;
                            font-weight: 300;
                            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                        }
                        .date {
                            font-size: 1.1em;
                            margin-top: 10px;
                            opacity: 0.9;
                        }
                        .word-section {
                            padding: 30px;
                            border-bottom: 1px solid #eee;
                            transition: background-color 0.3s ease;
                        }
                        .word-section:hover {
                            background-color: #f8f9fa;
                        }
                        .word-section:last-child {
                            border-bottom: none;
                        }
                        .word-header {
                            display: flex;
                            align-items: center;
                            margin-bottom: 20px;
                            flex-wrap: wrap;
                            gap: 15px;
                        }
                        .word-number {
                            background: linear-gradient(135deg, #667eea, #764ba2);
                            color: white;
                            width: 40px;
                            height: 40px;
                            border-radius: 50%%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-weight: bold;
                            font-size: 1.2em;
                        }
                        .word-title {
                            font-size: 2.2em;
                            font-weight: bold;
                            color: #2c3e50;
                            margin: 0;
                        }
                        .pronunciation {
                            font-family: 'Courier New', monospace;
                            color: #7f8c8d;
                            font-size: 1.1em;
                        }
                        .pos {
                            background: #3498db;
                            color: white;
                            padding: 4px 12px;
                            border-radius: 15px;
                            font-size: 0.9em;
                            font-weight: bold;
                        }
                        .audio-buttons {
                            display: flex;
                            gap: 10px;
                            flex-wrap: wrap;
                        }
                        .audio-btn {
                            background: #e74c3c;
                            color: white;
                            padding: 8px 15px;
                            border-radius: 20px;
                            text-decoration: none;
                            font-size: 0.9em;
                            font-weight: bold;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 8px rgba(231, 76, 60, 0.3);
                        }
                        .audio-btn:hover {
                            background: #c0392b;
                            transform: translateY(-2px);
                            box-shadow: 0 6px 12px rgba(231, 76, 60, 0.4);
                        }
                        .definition-section {
                            margin: 20px 0;
                        }
                        .definition-label {
                            font-weight: bold;
                            color: #2c3e50;
                            margin-bottom: 8px;
                            font-size: 1.1em;
                        }
                        .definition-content {
                            background: #f8f9fa;
                            padding: 15px;
                            border-left: 4px solid #3498db;
                            border-radius: 0 8px 8px 0;
                            margin-bottom: 15px;
                        }
                        .examples {
                            background: #fff3cd;
                            padding: 15px;
                            border-radius: 8px;
                            border-left: 4px solid #ffc107;
                            margin: 15px 0;
                        }
                        .example-sentence {
                            font-style: italic;
                            margin: 8px 0;
                            padding: 5px 0;
                            border-bottom: 1px dotted #ddd;
                        }
                        .example-sentence:last-child {
                            border-bottom: none;
                        }
                        .section-content {
                            margin: 15px 0;
                            padding: 12px;
                            background: #f1f3f4;
                            border-radius: 6px;
                        }
                        .vietnamese {
                            background: #d4edda;
                            color: #155724;
                            padding: 12px;
                            border-radius: 6px;
                            border-left: 4px solid #28a745;
                            font-weight: 500;
                        }
                        .footer {
                            background: #2c3e50;
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        @media (max-width: 600px) {
                            .word-header {
                                flex-direction: column;
                                align-items: flex-start;
                            }
                            .word-title {
                                font-size: 1.8em;
                            }
                            .audio-buttons {
                                width: 100%%;
                                justify-content: center;
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

        section.append(String.format("""
                <div class="word-section">
                    <div class="word-header">
                        <div class="word-number">%d</div>
                        <div>
                            <h2 class="word-title">%s</h2>
                            %s
                            %s
                        </div>
                        <div class="audio-buttons">
                            %s
                            %s
                        </div>
                    </div>
                """,
                index,
                word.getWord(),
                word.getPronunciation() != null ? "<div class=\"pronunciation\">" + word.getPronunciation() + "</div>" : "",
                word.getPartOfSpeech() != null ? "<span class=\"pos\">" + word.getPartOfSpeech() + "</span>" : "",
                word.getPronunciationAudioUrl() != null ?
                    "<a href=\"" + word.getPronunciationAudioUrl() + "\" class=\"audio-btn\">🔊 Pronunciation</a>" : "",
                word.getExampleAudioUrl() != null ?
                    "<a href=\"" + word.getExampleAudioUrl() + "\" class=\"audio-btn\">🔊 Example</a>" : ""
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
                            <p style="font-size: 0.9em; opacity: 0.8;">
                                💡 Tip: Click the 🔊 buttons to hear the pronunciation and examples!
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
