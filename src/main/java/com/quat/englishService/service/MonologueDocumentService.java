package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MonologueDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(MonologueDocumentService.class);

    public String generateMonologueDocument(List<ParsedVocabularyWord> words) {
        try {
            // Create document content
            StringBuilder document = new StringBuilder();

            // Add header
            document.append(generateDocumentHeader());

            // Add each word's monologue content
            for (int i = 0; i < words.size(); i++) {
                ParsedVocabularyWord word = words.get(i);
                document.append(generateWordSection(word, i + 1));
            }

            // Add footer
            document.append(generateDocumentFooter());

            // Save to file
            String fileName = "vocabulary_monologues_" +
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
            Path filePath = Paths.get("logs", fileName);
            Files.createDirectories(filePath.getParent());

            Files.write(filePath, document.toString().getBytes("UTF-8"));

            logger.info("Generated monologue document: {} ({} words, {} characters)",
                       fileName, words.size(), document.length());

            return filePath.toAbsolutePath().toString();

        } catch (Exception e) {
            logger.error("Error generating monologue document: {}", e.getMessage(), e);
            return null;
        }
    }

    private String generateDocumentHeader() {
        return String.format("""
                ================================================================================
                                        📚 DAILY ENGLISH VOCABULARY 📚
                                         Audio Monologue Transcripts
                                              %s
                ================================================================================
                
                This document contains the full text of all audio monologues included in 
                today's vocabulary email. Use this to follow along while listening to the 
                audio files, or to review the content after listening.
                
                Each monologue demonstrates the target vocabulary word used in natural, 
                conversational context multiple times to help with comprehension and retention.
                
                ================================================================================
                
                """,
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
    }

    private String generateWordSection(ParsedVocabularyWord word, int index) {
        StringBuilder section = new StringBuilder();

        // Add word header
        section.append(String.format("""
                %d. WORD: %s
                %s
                Pronunciation: %s
                Part of Speech: %s
                
                """,
                index,
                word.getWord().toUpperCase(),
                "=".repeat(Math.max(20, word.getWord().length() + 10)),
                word.getPronunciation() != null ? word.getPronunciation() : "N/A",
                word.getPartOfSpeech() != null ? word.getPartOfSpeech() : "N/A"));

        // Add simple definition
        if (word.getSimpleDefinition() != null) {
            section.append(String.format("""
                    💡 SIMPLE DEFINITION:
                    %s
                    
                    """, word.getSimpleDefinition()));
        }

        // Check if we have monologue data in raw explanation
        String rawExplanation = word.getRawExplanation();
        if (rawExplanation != null && rawExplanation.contains("**Monologue:**")) {
            // Extract and format monologue content
            VocabularyParsingService.MonologueInfo monologueInfo = parseMonologueFromRaw(rawExplanation);

            if (monologueInfo != null && !monologueInfo.getMonologue().isEmpty()) {
                section.append("""
                        🎵 AUDIO MONOLOGUE TRANSCRIPT:
                        --------------------------------------------------------------------------------
                        
                        """);

                section.append(cleanAndFormatMonologue(monologueInfo.getMonologue()));

                section.append("""
                        
                        --------------------------------------------------------------------------------
                        
                        📝 USAGE EXPLANATION:
                        """);

                if (!monologueInfo.getExplanation().isEmpty()) {
                    section.append(monologueInfo.getExplanation());
                } else {
                    section.append("See how the word '" + word.getWord() + "' is used naturally in different contexts above.");
                }

                section.append("\n\n");
            }
        } else {
            // Fallback to example sentences if no monologue
            if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) {
                section.append("""
                        📝 EXAMPLE SENTENCES:
                        --------------------------------------------------------------------------------
                        
                        """);

                for (int i = 0; i < word.getExampleSentences().length; i++) {
                    section.append(String.format("%d. %s\n\n", i + 1, word.getExampleSentences()[i]));
                }
            }
        }

        section.append("================================================================================\n\n");

        return section.toString();
    }

    private VocabularyParsingService.MonologueInfo parseMonologueFromRaw(String rawContent) {
        try {
            // Simple extraction of monologue sections
            String monologue = extractSection(rawContent, "**Monologue:**", "**Explanation:**");
            String explanation = extractSection(rawContent, "**Explanation:**", "**Pronunciation:**");
            String pronunciation = extractSection(rawContent, "**Pronunciation:**", null);

            if (monologue != null) {
                monologue = cleanMonologueText(monologue);
                explanation = explanation != null ? explanation.trim() : "";
                pronunciation = pronunciation != null ? pronunciation.replaceAll("[/\\[\\]]", "").trim() : "";

                return new VocabularyParsingService.MonologueInfo(monologue, explanation, pronunciation);
            }
        } catch (Exception e) {
            logger.debug("Could not parse monologue from raw content");
        }
        return null;
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        try {
            int startIndex = text.indexOf(startMarker);
            if (startIndex == -1) {
                return null;
            }

            startIndex += startMarker.length();
            int endIndex = text.length();

            if (endMarker != null) {
                int markerIndex = text.indexOf(endMarker, startIndex);
                if (markerIndex != -1) {
                    endIndex = markerIndex;
                }
            }

            return text.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanMonologueText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();

        // Remove markdown formatting but keep readability
        cleaned = cleaned.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1"); // Remove bold **text**
        cleaned = cleaned.replaceAll("\\*([^*]+?)\\*", "$1");       // Remove italic *text*
        cleaned = cleaned.replaceAll("_([^_]+?)_", "$1");           // Remove italic _text_

        // Remove stage directions but keep them readable in document
        cleaned = cleaned.replaceAll("\\(([^)]*)\\)", "[$1]");      // Convert (stage direction) to [stage direction]

        // Clean up formatting while preserving paragraph structure
        cleaned = cleaned.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");  // Reduce multiple line breaks
        cleaned = cleaned.replaceAll("\\s+", " ");                  // Normalize spaces within lines
        cleaned = cleaned.replaceAll(" \\. ", ". ");                // Fix spacing around periods

        return cleaned.trim();
    }

    private String cleanAndFormatMonologue(String monologue) {
        String cleaned = cleanMonologueText(monologue);

        // Format for better readability in document
        // Split into paragraphs and indent slightly
        String[] paragraphs = cleaned.split("\\n\\n");
        StringBuilder formatted = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                formatted.append("    ").append(paragraph.trim()).append("\n\n");
            }
        }

        return formatted.toString();
    }

    private String generateDocumentFooter() {
        return String.format("""
                ================================================================================
                                                 📚 NOTES 📚
                ================================================================================
                
                • This document contains the transcripts of all audio monologues in today's
                  vocabulary email.
                  
                • Use this document to follow along while listening to the MP3 audio files
                  attached to the email.
                  
                • Each monologue uses the target vocabulary word multiple times in natural
                  conversation to demonstrate proper usage and context.
                  
                • Stage directions are shown in [brackets] - these are not spoken in the audio.
                
                • For pronunciation practice, listen to the audio first, then read along with
                  this transcript.
                
                ================================================================================
                Generated on: %s
                Powered by: Gemini AI & Google Text-to-Speech
                ================================================================================
                """,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
