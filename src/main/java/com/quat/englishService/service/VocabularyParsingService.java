package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VocabularyParsingService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyParsingService.class);

    public ParsedVocabularyWord parseAIResponse(String word, String aiResponse) {
        logger.debug("Parsing AI response for word: {}", word);

        ParsedVocabularyWord parsed = new ParsedVocabularyWord(word, aiResponse);

        try {
            // Extract pronunciation (IPA)
            parsed.setPronunciation(extractPronunciation(aiResponse));

            // Extract part of speech
            parsed.setPartOfSpeech(extractPartOfSpeech(aiResponse));

            // Extract simple definition
            parsed.setSimpleDefinition(extractSimpleDefinition(aiResponse));

            // Extract advanced definition
            parsed.setAdvancedDefinition(extractAdvancedDefinition(aiResponse));

            // Extract example sentences
            parsed.setExampleSentences(extractExampleSentences(aiResponse));

            // Extract collocations with better formatting
            parsed.setCollocations(extractAndFormatCollocations(aiResponse));

            // Extract synonyms and antonyms with better formatting
            parsed.setSynonyms(extractAndFormatSynonyms(aiResponse));
            parsed.setAntonyms(extractAndFormatAntonyms(aiResponse));

            // Extract confused words
            parsed.setConfusedWords(extractAndFormatConfusedWords(aiResponse));

            // Extract word family
            parsed.setWordFamily(extractAndFormatWordFamily(aiResponse));

            // Extract Vietnamese translation
            parsed.setVietnameseTranslation(extractAndFormatVietnamese(aiResponse));

            logger.debug("Successfully parsed AI response for word: {}", word);

        } catch (Exception e) {
            logger.error("Error parsing AI response for word '{}': {}", word, e.getMessage(), e);
        }

        return parsed;
    }

    private String extractPronunciation(String text) {
        // Look for IPA pronunciation patterns
        Pattern[] patterns = {
            Pattern.compile("(?i)pronunciation[:\\s]*\\*?\\s*(/[^/]+/)", Pattern.MULTILINE),
            Pattern.compile("(?i)ipa[:\\s]*\\*?\\s*(/[^/]+/)", Pattern.MULTILINE),
            Pattern.compile("(/[^/]+/)")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(matcher.groupCount()).trim();
            }
        }
        return null;
    }

    private String extractPartOfSpeech(String text) {
        Pattern pattern = Pattern.compile("(?i)part of speech[:\\s]*\\*?\\s*([^\n.]+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("[*_]", "");
        }
        return null;
    }

    private String extractSimpleDefinition(String text) {
        Pattern pattern = Pattern.compile("(?i)simple[:\\s]*\\*?\\s*([^\n]+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("[*_]", "");
        }
        return null;
    }

    private String extractAdvancedDefinition(String text) {
        Pattern pattern = Pattern.compile("(?i)advanced[:\\s]*\\*?\\s*([^\n]+(?:\n\\s*[^\n*]+)*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim().replaceAll("[*_]", "");
        }
        return null;
    }

    private String[] extractExampleSentences(String text) {
        List<String> sentences = new ArrayList<>();

        // Look for the Example Sentences section with improved pattern
        // Updated pattern to look for section headers more specifically (e.g., ** Synonyms, ** Antonyms, etc.)
        Pattern sectionPattern = Pattern.compile("(?i)\\*\\*\\s*Example Sentences:\\*\\*(.*?)(?=\\*\\*\\s*[A-Z][a-z]+\\s*[:\\s]|$)", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String exampleSection = sectionMatcher.group(1);

            // Look for numbered examples with quotes
            Pattern numberedPattern = Pattern.compile("\\d+\\.\\s*([^\\n]+)", Pattern.MULTILINE);
            Matcher numberedMatcher = numberedPattern.matcher(exampleSection);

            while (numberedMatcher.find() && sentences.size() < 5) {
                String sentence = numberedMatcher.group(1).trim();
                // Clean up the sentence - remove extra quotes and formatting
                sentence = sentence.replaceAll("^[\"']|[\"']$", ""); // Remove quotes at start/end
                sentence = sentence.replaceAll("\\*\\*([^*]+)\\*\\*", "$1"); // Remove bold formatting
                if (!sentence.isEmpty() && sentence.length() > 10) {
                    sentences.add(sentence);
                }
            }
        }

        // If no numbered examples found, try alternative patterns
        if (sentences.isEmpty()) {
            Pattern bulletPattern = Pattern.compile("(?:^|\\n)\\s*[-*]\\s*\"?([^\\n\"]+)\"?", Pattern.MULTILINE);
            Matcher bulletMatcher = bulletPattern.matcher(text);

            while (bulletMatcher.find() && sentences.size() < 5) {
                String sentence = bulletMatcher.group(1).trim();
                sentence = sentence.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
                if (!sentence.isEmpty() && sentence.length() > 10 && sentence.matches(".*[a-zA-Z].*")) {
                    sentences.add(sentence);
                }
            }
        }

        return sentences.toArray(new String[0]);
    }

    private String extractAndFormatCollocations(String text) {
        String section = extractSection(text, "collocations?");
        if (section != null) {
            return formatListContent(section, "collocation");
        }
        return null;
    }

    private String extractAndFormatSynonyms(String text) {
        // First try to extract the full Synonyms section with improved pattern
        Pattern sectionPattern = Pattern.compile("(?i)\\*\\s*Synonyms:\\*\\*(.*?)(?=\\*\\s*Antonyms:|$)", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String synonymsSection = sectionMatcher.group(1).trim();
            return formatSynonymsContent(synonymsSection);
        }

        // Fallback to generic extraction
        String section = extractSection(text, "synonyms?");
        if (section != null) {
            return formatSynonymsContent(section);
        }
        return null;
    }

    private String extractAndFormatAntonyms(String text) {
        // First try to extract the full Antonyms section with improved pattern
        Pattern sectionPattern = Pattern.compile("(?i)\\*\\s*Antonyms:\\*\\*(.*?)(?=\\*\\*\\s*Commonly Confused Words:|\\*\\*\\s*Word Family:|$)", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String antonymsSection = sectionMatcher.group(1).trim();
            return formatAntonymsContent(antonymsSection);
        }

        // Fallback to generic extraction
        String section = extractSection(text, "antonyms?");
        if (section != null) {
            return formatAntonymsContent(section);
        }
        return null;
    }

    private String extractAndFormatConfusedWords(String text) {
        // First try to extract the full "Commonly Confused Words" section with improved pattern
        Pattern sectionPattern = Pattern.compile("(?i)\\*\\*\\s*Commonly Confused Words:\\*\\*(.*?)(?=\\*\\*\\s*Word Family:|\\*\\*\\s*Vietnamese|$)", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(text);

        if (sectionMatcher.find()) {
            String confusedSection = sectionMatcher.group(1).trim();
            return formatConfusedWordsContent(confusedSection);
        }

        // Fallback patterns for different formats
        Pattern altPattern1 = Pattern.compile("(?i)\\*\\*Commonly Confused Words:\\*\\*(.*?)(?=\\*\\*[A-Z]|$)", Pattern.DOTALL);
        Matcher altMatcher1 = altPattern1.matcher(text);

        if (altMatcher1.find()) {
            String confusedSection = altMatcher1.group(1).trim();
            return formatConfusedWordsContent(confusedSection);
        }

        // Generic section extraction as final fallback
        String section = extractSection(text, "(?:commonly )?confused");
        if (section != null) {
            return formatConfusedWordsContent(section);
        }

        logger.debug("No Commonly Confused Words section found in text");
        return null;
    }

    private String formatConfusedWordsContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Pattern 1: Look for standardized format "* **Word:** explanation"
        Pattern standardPattern = Pattern.compile("\\*\\s*\\*\\*([^:*]+?):\\*\\*\\s*([^*]+?)(?=\\*\\s*\\*\\*|$)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher standardMatcher = standardPattern.matcher(content);

        while (standardMatcher.find()) {
            String word = standardMatcher.group(1).trim();
            String explanation = standardMatcher.group(2).trim();

            // Clean up explanation
            explanation = explanation.replaceAll("\\*([^*]+?)\\*", "<em>$1</em>"); // Single asterisks to italics
            explanation = explanation.replaceAll("_([^_]+?)_", "<em>$1</em>"); // Underscores to italics
            explanation = explanation.replaceAll("\\s+", " "); // Normalize whitespace
            explanation = explanation.replaceAll("\\n", " "); // Replace newlines with spaces

            formatted.append("• <strong>").append(word).append(":</strong> ")
                     .append(explanation).append("<br>");
        }

        // If no standard format found, try alternative parsing
        if (formatted.length() == 0) {
            // Pattern 2: Look for lines that contain word comparisons
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("*") && line.length() < 10) {
                    continue;
                }

                // Clean up the line
                line = line.replaceAll("^\\*\\s*", ""); // Remove leading bullet
                line = line.replaceAll("\\*\\*([^*]+?)\\*\\*", "<strong>$1</strong>");
                line = line.replaceAll("\\*([^*]+?)\\*", "<em>$1</em>");

                if (line.length() > 15 && (line.contains(":") || line.contains("vs") || line.contains("differ"))) {
                    formatted.append("• ").append(line).append("<br>");
                }
            }
        }

        // Pattern 3: If still no content, try to parse as continuous text
        if (formatted.length() == 0 && content.length() > 20) {
            // Split by sentences and format each meaningful one
            String cleanContent = content.replaceAll("\\*\\*([^*]+?)\\*\\*", "<strong>$1</strong>");
            cleanContent = cleanContent.replaceAll("\\*([^*]+?)\\*", "<em>$1</em>");
            cleanContent = cleanContent.replaceAll("\\s+", " ").trim();

            String[] sentences = cleanContent.split("\\.");
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() > 20 && (sentence.contains("differ") || sentence.contains("vs") || sentence.contains("distinguish"))) {
                    formatted.append("• ").append(sentence).append(".<br>");
                }
            }
        }

        String result = formatted.length() > 0 ? formatted.toString() : null;
        logger.debug("Formatted confused words section: {}", result != null ? "success" : "failed");
        return result;
    }

    private String extractAndFormatWordFamily(String text) {
        String section = extractSection(text, "word family");
        if (section != null) {
            return formatWordFamilyContent(section);
        }
        return null;
    }

    private String extractAndFormatVietnamese(String text) {
        String section = extractSection(text, "vietnamese");
        if (section != null) {
            return formatVietnameseContent(section);
        }
        return null;
    }

    private String extractSection(String text, String sectionName) {
        try {
            // Enhanced pattern to capture section content more accurately
            String pattern = String.format(
                "(?i)\\*?\\*?\\s*\\d*\\.?\\s*%s[^\\n]*:?\\*?\\*?\\s*\n?((?:[^\\n]*\n?)*?)(?=\\n\\s*\\*?\\*?\\s*\\d+\\.|\\n\\s*$|$)",
                sectionName
            );

            Pattern p = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
            Matcher m = p.matcher(text);

            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            logger.debug("Could not extract section: {}", sectionName);
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

            String extracted = text.substring(startIndex, endIndex).trim();

            // Clean up markdown formatting
            extracted = extracted.replaceAll("\\*\\*([^*]+)\\*\\*", "$1"); // Remove bold
            extracted = extracted.replaceAll("\\[([^\\]]+)\\]", "$1"); // Remove brackets

            return extracted.isEmpty() ? null : extracted;

        } catch (Exception e) {
            logger.debug("Could not extract section from {} to {}", startMarker, endMarker);
            return null;
        }
    }

    private String formatSynonymsContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for individual synonym entries with explanations using improved pattern
        Pattern synonymPattern = Pattern.compile("\\*\\s*\\*\\*([^:*]+?):\\*\\*\\s*([^*]+?)(?=\\*\\s*\\*\\*|$)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = synonymPattern.matcher(content);

        while (matcher.find()) {
            String synonym = matcher.group(1).trim();
            String explanation = matcher.group(2).trim();

            // Clean up explanation - preserve formatting but normalize
            explanation = explanation.replaceAll("\\*([^*]+?)\\*", "<em>$1</em>"); // Single asterisks to italics
            explanation = explanation.replaceAll("_([^_]+?)_", "<em>$1</em>"); // Underscores to italics
            explanation = explanation.replaceAll("\\s+", " "); // Normalize whitespace
            explanation = explanation.replaceAll("\\n", " "); // Replace newlines with spaces

            formatted.append("• <strong>").append(synonym).append(":</strong> ")
                     .append(explanation).append("<br>");
        }

        // If no structured format found, try simpler parsing
        if (formatted.length() == 0) {
            // Split by lines and look for pattern Word: explanation
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.length() < 5) continue;

                // Remove leading bullets and formatting
                line = line.replaceAll("^\\*\\s*", "");
                line = line.replaceAll("\\*\\*([^*]+?)\\*\\*", "<strong>$1</strong>");

                if (line.contains(":") && line.length() > 10) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String word = parts[0].trim();
                        String explanation = parts[1].trim();
                        formatted.append("• <strong>").append(word).append(":</strong> ")
                                 .append(explanation).append("<br>");
                    }
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : null;
    }

    private String formatAntonymsContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for individual antonym entries with explanations using improved pattern
        Pattern antonymPattern = Pattern.compile("\\*\\s*\\*\\*([^:*]+?):\\*\\*\\s*([^*]+?)(?=\\*\\s*\\*\\*|$)", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = antonymPattern.matcher(content);

        while (matcher.find()) {
            String antonym = matcher.group(1).trim();
            String explanation = matcher.group(2).trim();

            // Clean up explanation - preserve formatting but normalize
            explanation = explanation.replaceAll("\\*([^*]+?)\\*", "<em>$1</em>"); // Single asterisks to italics
            explanation = explanation.replaceAll("_([^_]+?)_", "<em>$1</em>"); // Underscores to italics
            explanation = explanation.replaceAll("\\s+", " "); // Normalize whitespace
            explanation = explanation.replaceAll("\\n", " "); // Replace newlines with spaces

            formatted.append("• <strong>").append(antonym).append(":</strong> ")
                     .append(explanation).append("<br>");
        }

        // If no structured format found, try simpler parsing
        if (formatted.length() == 0) {
            // Split by lines and look for pattern Word: explanation
            String[] lines = content.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.length() < 5) continue;

                // Remove leading bullets and formatting
                line = line.replaceAll("^\\*\\s*", "");
                line = line.replaceAll("\\*\\*([^*]+?)\\*\\*", "<strong>$1</strong>");

                if (line.contains(":") && line.length() > 10) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        String word = parts[0].trim();
                        String explanation = parts[1].trim();
                        formatted.append("• <strong>").append(word).append(":</strong> ")
                                 .append(explanation).append("<br>");
                    }
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : null;
    }

    private String cleanFallbackContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // Clean up markdown formatting and return simple list
        String cleaned = content.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        cleaned = cleaned.replaceAll("\\*([^*]+)\\*", "$1");
        cleaned = cleaned.replaceAll("_([^_]+)_", "$1");
        cleaned = cleaned.replaceAll("\\n\\s*", "<br>• ");

        if (!cleaned.startsWith("• ")) {
            cleaned = "• " + cleaned;
        }

        return cleaned;
    }

    private String formatListContent(String content, String type) {
        StringBuilder formatted = new StringBuilder();

        // Split by common delimiters and format as list
        String[] items = content.split("(?:[.!]\\s*|\\n\\s*[-*]\\s*|\\s*;\\s*)");

        for (String item : items) {
            item = item.trim().replaceAll("[*_]", "");
            if (!item.isEmpty() && item.length() > 3) {
                // Clean up the item
                item = item.replaceAll("^[^A-Za-z]*", "").replaceAll("[^A-Za-z\\s:.,!?()\"'-]+$", "");
                if (!item.isEmpty()) {
                    formatted.append("• ").append(item).append("<br>");
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : content;
    }

    private String formatWordFamilyContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for word family patterns (Noun:, Verb:, Adjective:, etc.)
        Pattern familyPattern = Pattern.compile("([A-Za-z]+):\\s*([^\n]+)", Pattern.MULTILINE);
        Matcher matcher = familyPattern.matcher(content);

        while (matcher.find()) {
            String pos = matcher.group(1).trim();
            String words = matcher.group(2).trim().replaceAll("[*_]", "");
            formatted.append("• <strong>").append(pos).append(":</strong> ")
                     .append(words).append("<br>");
        }

        return formatted.length() > 0 ? formatted.toString() : content;
    }

    private String formatVietnameseContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for Vietnamese translation patterns
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim().replaceAll("[*_]", "");
            if (!line.isEmpty()) {
                if (line.contains(":")) {
                    // Format as explanation
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        formatted.append("• <strong>").append(parts[0].trim()).append(":</strong> ")
                                 .append(parts[1].trim()).append("<br>");
                    }
                } else {
                    formatted.append("• ").append(line).append("<br>");
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : content;
    }

    public MonologueInfo parseMonologue(String rawMonologue) {
        if (rawMonologue == null || rawMonologue.trim().isEmpty()) {
            return null;
        }

        try {
            String monologue = "";
            String explanation = "";
            String pronunciation = "";

            // Try to extract with markdown format first
            String monologueWithMarkdown = extractSection(rawMonologue, "**Monologue:**", "**Explanation:**");
            if (monologueWithMarkdown != null) {
                monologue = monologueWithMarkdown;
                explanation = extractSection(rawMonologue, "**Explanation:**", "**Pronunciation:**");
                pronunciation = extractSection(rawMonologue, "**Pronunciation:**", null);
            } else {
                // Try to extract with plain format (without markdown)
                monologue = extractSection(rawMonologue, "Monologue:", "Explanation:");
                if (monologue != null) {
                    explanation = extractSection(rawMonologue, "Explanation:", "Pronunciation:");
                    pronunciation = extractSection(rawMonologue, "Pronunciation:", null);
                } else {
                    // Fallback: assume the entire content is the monologue if no markers found
                    logger.warn("No clear monologue markers found, treating entire content as monologue");
                    monologue = rawMonologue;
                }
            }

            // Clean up the extracted content
            // Log the first part of cleaned monologue for debugging
            if (monologue.length() > 0) {
                logger.info("Cleaned monologue text: {}", monologue.substring(0, Math.min(200, monologue.length())) + "...");
            }
            pronunciation = pronunciation != null ? pronunciation.replaceAll("[/\\[\\]]", "").trim() : "";

            logger.debug("Parsed monologue - Length: {}, Has explanation: {}, Has pronunciation: {}",
                        monologue.length(), !explanation.isEmpty(), !pronunciation.isEmpty());

            return new MonologueInfo(monologue, explanation, pronunciation);

        } catch (Exception e) {
            logger.error("Error parsing monologue: {}", e.getMessage(), e);
            return null;
        }
    }

    private String cleanMonologueText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text.trim();

        // Remove any remaining section markers
        cleaned = cleaned.replaceAll("\\*\\*Monologue:\\*\\*", "");
        cleaned = cleaned.replaceAll("\\*\\*Explanation:\\*\\*", "");
        cleaned = cleaned.replaceAll("\\*\\*Pronunciation:\\*\\*", "");

        // Remove markdown formatting but preserve structure
        cleaned = cleaned.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1"); // Remove bold **text**
        cleaned = cleaned.replaceAll("\\*([^*]+?)\\*", "$1");       // Remove italic *text*
        cleaned = cleaned.replaceAll("_([^_]+?)_", "$1");           // Remove italic _text_

        // Remove stage directions and formatting artifacts
        cleaned = cleaned.replaceAll("\\([^)]*\\)", "");            // Remove parenthetical directions like "(Sighs, looking out the window)"

        // Clean up bullet points and list formatting
        cleaned = cleaned.replaceAll("^\\s*[*•-]\\s*", "");         // Remove bullet points at start of lines
        cleaned = cleaned.replaceAll("\\n\\s*[*•-]\\s*", "\n");     // Remove bullet points in middle

        // Remove any stray markdown artifacts
        cleaned = cleaned.replaceAll("#+\\s*", "");                 // Remove headers
        cleaned = cleaned.replaceAll("```[^`]*```", "");            // Remove code blocks
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1");            // Remove inline code

        // Normalize whitespace and line breaks
        cleaned = cleaned.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");  // Reduce multiple line breaks to max 2
        cleaned = cleaned.replaceAll("\\s+", " ");                  // Normalize spaces
        cleaned = cleaned.replaceAll("\\n\\s*", "\n");              // Clean line starts

        // Remove leading/trailing whitespace and ensure proper sentence structure
        cleaned = cleaned.trim();

        // Ensure sentences end properly for TTS
        if (!cleaned.isEmpty() && !cleaned.matches(".*[.!?]\\s*$")) {
            cleaned += ".";
        }

        logger.debug("Cleaned monologue text from {} chars to {} chars", text.length(), cleaned.length());

        return cleaned;
    }

    public static class MonologueInfo {
        private final String monologue;
        private final String explanation;
        private final String pronunciation;

        public MonologueInfo(String monologue, String explanation, String pronunciation) {
            this.monologue = monologue;
            this.explanation = explanation;
            this.pronunciation = pronunciation;
        }

        public String getMonologue() { return monologue; }
        public String getExplanation() { return explanation; }
        public String getPronunciation() { return pronunciation; }

        @Override
        public String toString() {
            return String.format("MonologueInfo{monologue='%s...', explanation='%s...', pronunciation='%s'}",
                               monologue.substring(0, Math.min(50, monologue.length())),
                               explanation.substring(0, Math.min(50, explanation.length())),
                               pronunciation);
        }
    }
}
