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

        // Look for numbered examples or bullet points
        Pattern pattern = Pattern.compile("(?i)(?:\\*|\\d+\\.|-)\\s*\"([^\"]+)\"", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find() && sentences.size() < 5) {
            sentences.add(matcher.group(1).trim());
        }

        // If no quoted sentences found, look for sentences with the word in bold/asterisks
        if (sentences.isEmpty()) {
            Pattern boldPattern = Pattern.compile("\"([^\"]*\\*\\*[^*]+\\*\\*[^\"]*)\"|\"([^\"]*__[^_]+__[^\"]*)\"|\"([^\"]+)\"");
            Matcher boldMatcher = boldPattern.matcher(text);

            while (boldMatcher.find() && sentences.size() < 5) {
                for (int i = 1; i <= 3; i++) {
                    if (boldMatcher.group(i) != null) {
                        sentences.add(boldMatcher.group(i).trim());
                        break;
                    }
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
        String section = extractSection(text, "synonyms?");
        if (section != null) {
            return formatSynonymsContent(section);
        }
        return null;
    }

    private String extractAndFormatAntonyms(String text) {
        String section = extractSection(text, "antonyms?");
        if (section != null) {
            return formatAntonymsContent(section);
        }
        return null;
    }

    private String extractAndFormatConfusedWords(String text) {
        String section = extractSection(text, "(?:commonly )?confused");
        if (section != null) {
            return formatListContent(section, "confusion");
        }
        return null;
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

    private String formatSynonymsContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for synonym entries with explanations
        Pattern synonymPattern = Pattern.compile("([A-Za-z]+):\\s*([^.]+(?:\\.[^A-Z]*)*)", Pattern.MULTILINE);
        Matcher matcher = synonymPattern.matcher(content);

        while (matcher.find()) {
            String synonym = matcher.group(1).trim();
            String explanation = matcher.group(2).trim();
            formatted.append("• <strong>").append(synonym).append(":</strong> ")
                     .append(explanation).append("<br>");
        }

        // If no structured synonyms found, try to extract simple list
        if (formatted.length() == 0) {
            String[] words = content.split("[,;]");
            for (String word : words) {
                word = word.trim().replaceAll("[*_]", "");
                if (!word.isEmpty() && word.matches("^[A-Za-z\\s]+$")) {
                    formatted.append("• ").append(word).append("<br>");
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : content;
    }

    private String formatAntonymsContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // Look for antonym entries with explanations
        Pattern antonymPattern = Pattern.compile("([A-Za-z]+):\\s*([^.]+(?:\\.[^A-Z]*)*)", Pattern.MULTILINE);
        Matcher matcher = antonymPattern.matcher(content);

        while (matcher.find()) {
            String antonym = matcher.group(1).trim();
            String explanation = matcher.group(2).trim();
            formatted.append("• <strong>").append(antonym).append(":</strong> ")
                     .append(explanation).append("<br>");
        }

        // If no structured antonyms found, try to extract simple list
        if (formatted.length() == 0) {
            String[] words = content.split("[,;]");
            for (String word : words) {
                word = word.trim().replaceAll("[*_]", "");
                if (!word.isEmpty() && word.matches("^[A-Za-z\\s]+$")) {
                    formatted.append("• ").append(word).append("<br>");
                }
            }
        }

        return formatted.length() > 0 ? formatted.toString() : content;
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
}
