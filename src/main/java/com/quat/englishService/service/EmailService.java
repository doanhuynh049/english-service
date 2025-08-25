package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                        }
                        .header .date {
                            font-size: 1.1em;
                            opacity: 0.9;
                            margin-top: 10px;
                        }
                        .word-container {
                            padding: 0 30px;
                        }
                        .word-card {
                            margin: 30px 0;
                            border-left: 5px solid #667eea;
                            background: #f8f9ff;
                            border-radius: 10px;
                            overflow: hidden;
                            transition: transform 0.3s ease;
                        }
                        .word-header {
                            background: #667eea;
                            color: white;
                            padding: 20px;
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                        }
                        .word-title {
                            font-size: 1.8em;
                            font-weight: bold;
                            margin: 0;
                        }
                        .word-number {
                            background: rgba(255,255,255,0.2);
                            padding: 5px 15px;
                            border-radius: 20px;
                            font-weight: bold;
                        }
                        .word-content {
                            padding: 25px;
                        }
                        .pronunciation {
                            font-size: 1.3em;
                            color: #764ba2;
                            font-weight: 500;
                            margin-bottom: 15px;
                        }
                        .pos-badge {
                            display: inline-block;
                            background: #e8f0fe;
                            color: #1976d2;
                            padding: 5px 12px;
                            border-radius: 20px;
                            font-size: 0.9em;
                            font-weight: 500;
                            margin-bottom: 20px;
                        }
                        .definition-section {
                            margin: 20px 0;
                            padding: 20px;
                            background: white;
                            border-radius: 8px;
                            border-left: 3px solid #4caf50;
                        }
                        .section-title {
                            color: #2c3e50;
                            font-weight: bold;
                            font-size: 1.1em;
                            margin-bottom: 10px;
                            text-transform: uppercase;
                            letter-spacing: 0.5px;
                        }
                        .simple-def {
                            font-size: 1.1em;
                            color: #27ae60;
                            margin-bottom: 10px;
                        }
                        .advanced-def {
                            color: #555;
                            font-style: italic;
                        }
                        .example-sentence {
                            background: #fff3cd;
                            padding: 12px;
                            margin: 8px 0;
                            border-left: 3px solid #ffc107;
                            border-radius: 5px;
                            font-style: italic;
                        }
                        .info-grid {
                            display: grid;
                            grid-template-columns: 1fr 1fr;
                            gap: 20px;
                            margin: 20px 0;
                        }
                        .info-box {
                            background: white;
                            padding: 15px;
                            border-radius: 8px;
                            border: 1px solid #e1e8ed;
                        }
                        .vietnamese {
                            background: #e8f5e8;
                            color: #2e7d32;
                            padding: 15px;
                            border-radius: 8px;
                            margin: 15px 0;
                            border-left: 3px solid #4caf50;
                        }
                        .footer {
                            background: #2c3e50;
                            color: white;
                            padding: 30px;
                            text-align: center;
                            margin-top: 30px;
                        }
                        .footer h3 {
                            margin: 0 0 10px 0;
                            color: #3498db;
                        }
                        .stats {
                            background: #ecf0f1;
                            padding: 20px;
                            text-align: center;
                            color: #7f8c8d;
                        }
                        @media (max-width: 600px) {
                            .info-grid {
                                grid-template-columns: 1fr;
                            }
                            .container {
                                margin: 10px;
                                border-radius: 10px;
                            }
                            body {
                                padding: 10px;
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
                        <div class="word-container">
                """,
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
    }

    private String buildWordSection(ParsedVocabularyWord word, int wordNumber) {
        StringBuilder section = new StringBuilder();

        section.append(String.format("""
                <div class="word-card">
                    <div class="word-header">
                        <h2 class="word-title">%s</h2>
                        <div class="word-number">Word %d</div>
                    </div>
                    <div class="word-content">
                """, word.getWord().toUpperCase(), wordNumber));

        // Pronunciation
        if (word.getPronunciation() != null) {
            section.append(String.format("""
                    <div class="pronunciation">🔊 %s</div>
                    """, word.getPronunciation()));
        }

        // Part of Speech
        if (word.getPartOfSpeech() != null) {
            section.append(String.format("""
                    <div class="pos-badge">%s</div>
                    """, word.getPartOfSpeech()));
        }

        // Definitions
        if (word.getSimpleDefinition() != null || word.getAdvancedDefinition() != null) {
            section.append("""
                    <div class="definition-section">
                        <div class="section-title">📖 Definition</div>
                    """);

            if (word.getSimpleDefinition() != null) {
                section.append(String.format("""
                        <div class="simple-def"><strong>Simple:</strong> %s</div>
                        """, word.getSimpleDefinition()));
            }

            if (word.getAdvancedDefinition() != null) {
                section.append(String.format("""
                        <div class="advanced-def"><strong>Advanced:</strong> %s</div>
                        """, word.getAdvancedDefinition()));
            }

            section.append("</div>");
        }

        // Example Sentences
        if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) {
            section.append("""
                    <div class="definition-section">
                        <div class="section-title">💬 Example Sentences</div>
                    """);

            for (String example : word.getExampleSentences()) {
                section.append(String.format("""
                        <div class="example-sentence">"%s"</div>
                        """, example));
            }

            section.append("</div>");
        }

        // Info Grid for Synonyms, Antonyms, etc.
        section.append("""
                <div class="info-grid">
                """);

        if (word.getSynonyms() != null) {
            section.append(String.format("""
                    <div class="info-box">
                        <div class="section-title">✅ Synonyms</div>
                        <div>%s</div>
                    </div>
                    """, word.getSynonyms()));
        }

        if (word.getCollocations() != null) {
            section.append(String.format("""
                    <div class="info-box">
                        <div class="section-title">🔗 Collocations</div>
                        <div>%s</div>
                    </div>
                    """, word.getCollocations()));
        }

        if (word.getWordFamily() != null) {
            section.append(String.format("""
                    <div class="info-box">
                        <div class="section-title">👨‍👩‍👧‍👦 Word Family</div>
                        <div>%s</div>
                    </div>
                    """, word.getWordFamily()));
        }

        if (word.getConfusedWords() != null) {
            section.append(String.format("""
                    <div class="info-box">
                        <div class="section-title">⚠️ Commonly Confused</div>
                        <div>%s</div>
                    </div>
                    """, word.getConfusedWords()));
        }

        section.append("</div>");

        // Vietnamese Translation
        if (word.getVietnameseTranslation() != null) {
            section.append(String.format("""
                    <div class="vietnamese">
                        <div class="section-title">🇻🇳 Vietnamese Translation</div>
                        <div>%s</div>
                    </div>
                    """, word.getVietnameseTranslation()));
        }

        section.append("""
                    </div>
                </div>
                """);

        return section.toString();
    }

    private String buildEmailFooter() {
        return String.format("""
                        </div>
                        <div class="stats">
                            <p><strong>📊 Today's Learning Session Complete!</strong></p>
                            <p>You've learned 10 new vocabulary words • Keep up the great work! 🌟</p>
                        </div>
                        <div class="footer">
                            <h3>🎯 Keep Learning & Growing!</h3>
                            <p>Remember: Consistency is key to mastering English vocabulary.</p>
                            <p>Practice using these words in your daily conversations!</p>
                            <hr style="border: 1px solid #34495e; margin: 20px 0;">
                            <p><em>📧 This email was automatically generated by your English Vocabulary Service</em></p>
                            <p><small>Generated on %s</small></p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm")));
    }
}
