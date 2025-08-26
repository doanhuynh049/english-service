package com.quat.englishService.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VocabularyGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyGeneratorService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    // Cache to store generated vocabulary lists by category/level
    private final Map<String, List<String>> vocabularyCache = new ConcurrentHashMap<>();

    @Value("${app.vocabulary.cache-size:500}")
    private int cacheSize;

    public VocabularyGeneratorService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Get a list of vocabulary words from Gemini AI based on specified criteria
     */
    public List<String> getVocabularyWords(VocabularyLevel level, VocabularyCategory category, int count) {
        String cacheKey = String.format("%s_%s_%d", level.name(), category.name(), count);

        // Check cache first
        if (vocabularyCache.containsKey(cacheKey)) {
            List<String> cached = vocabularyCache.get(cacheKey);
            logger.debug("Returning {} cached vocabulary words for {}/{}", cached.size(), level, category);
            return new ArrayList<>(cached);
        }

        try {
            logger.info("Generating {} {} vocabulary words from Gemini AI", count, level);

            String prompt = buildVocabularyPrompt(level, category, count);
            String response = geminiClient.getWordExplanation(prompt);

            List<String> words = parseVocabularyFromResponse(response);

            if (!words.isEmpty()) {
                // Cache the results
                vocabularyCache.put(cacheKey, new ArrayList<>(words));
                logger.info("Successfully generated and cached {} vocabulary words", words.size());
            } else {
                logger.warn("No vocabulary words extracted from Gemini response");
                // Return fallback words if Gemini fails
                return getFallbackWords(count);
            }

            return words;

        } catch (Exception e) {
            logger.error("Error generating vocabulary from Gemini: {}", e.getMessage(), e);
            return getFallbackWords(count);
        }
    }

    /**
     * Get random vocabulary words mixed from different levels and categories
     */
    public List<String> getRandomMixedVocabulary(int count) {
        List<String> allWords = new ArrayList<>();

        try {
            // Get words from different levels and categories
            allWords.addAll(getVocabularyWords(VocabularyLevel.INTERMEDIATE, VocabularyCategory.ACADEMIC, count / 3));
            allWords.addAll(getVocabularyWords(VocabularyLevel.ADVANCED, VocabularyCategory.BUSINESS, count / 3));
            allWords.addAll(getVocabularyWords(VocabularyLevel.INTERMEDIATE, VocabularyCategory.GENERAL, count / 3));

            // If we need more words, get additional ones
            if (allWords.size() < count) {
                int needed = count - allWords.size();
                allWords.addAll(getVocabularyWords(VocabularyLevel.ADVANCED, VocabularyCategory.SCIENTIFIC, needed));
            }

            // Remove duplicates and shuffle
            Set<String> uniqueWords = new LinkedHashSet<>(allWords);
            List<String> result = new ArrayList<>(uniqueWords);
            Collections.shuffle(result);

            // Return exactly the requested count
            return result.subList(0, Math.min(count, result.size()));

        } catch (Exception e) {
            logger.error("Error getting mixed vocabulary: {}", e.getMessage(), e);
            return getFallbackWords(count);
        }
    }

    private String buildVocabularyPrompt(VocabularyLevel level, VocabularyCategory category, int count) {
        return String.format("""
                Generate exactly %d English vocabulary words suitable for %s level learners.
                Focus on %s vocabulary.
                
                Requirements:
                - Words should be at %s difficulty level
                - Include a mix of nouns, verbs, adjectives, and adverbs
                - Words should be practical and commonly used in academic/professional contexts
                - Avoid very basic words (like "cat", "dog", "run")
                - Avoid extremely obscure words that are rarely used
                - Return ONLY the words, one per line, no definitions or explanations
                - No numbering or bullet points, just the words
                - Focus on target 900+ or IELTS 6.5+ level words
                Example format:
                eloquent
                meticulous
                comprehensive
                facilitate
                substantial
                
                Generate %d %s %s vocabulary words now:
                """, count, level.getDescription(), category.getDescription(),
                level.getDescription(), count, level.getDescription(), category.getDescription());
    }

    private List<String> parseVocabularyFromResponse(String response) {
        List<String> words = new ArrayList<>();

        try {
            // Split by lines and clean up
            String[] lines = response.split("\\r?\\n");

            for (String line : lines) {
                String word = cleanWord(line.trim());
                if (isValidWord(word)) {
                    words.add(word);
                }
            }

            // If parsing by lines doesn't work well, try regex extraction
            if (words.size() < 5) {
                words.clear();
                Pattern wordPattern = Pattern.compile("\\b([a-zA-Z]{4,15})\\b");
                Matcher matcher = wordPattern.matcher(response);

                Set<String> foundWords = new HashSet<>();
                while (matcher.find() && foundWords.size() < 100) {
                    String word = matcher.group(1).toLowerCase();
                    if (isValidWord(word) && !isCommonWord(word)) {
                        foundWords.add(word);
                    }
                }
                words.addAll(foundWords);
            }

            logger.debug("Parsed {} vocabulary words from Gemini response", words.size());

        } catch (Exception e) {
            logger.error("Error parsing vocabulary from response: {}", e.getMessage(), e);
        }

        return words;
    }

    private String cleanWord(String word) {
        if (word == null) return "";

        // Remove common prefixes/suffixes that might appear in responses
        word = word.replaceAll("^\\d+\\.?\\s*", ""); // Remove numbering
        word = word.replaceAll("^[-*]\\s*", ""); // Remove bullet points
        word = word.replaceAll("[^a-zA-Z]", ""); // Keep only letters

        return word.toLowerCase().trim();
    }

    private boolean isValidWord(String word) {
        if (word == null || word.length() < 4 || word.length() > 15) {
            return false;
        }

        // Check if it's only letters
        return word.matches("^[a-zA-Z]+$");
    }

    private boolean isCommonWord(String word) {
        // List of very basic words to avoid
        Set<String> commonWords = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out",
            "day", "get", "has", "him", "his", "how", "man", "new", "now", "old", "see", "two", "way", "who", "boy",
            "did", "its", "let", "put", "say", "she", "too", "use", "that", "with", "have", "this", "will", "your",
            "from", "they", "know", "want", "been", "good", "much", "some", "time", "very", "when", "come", "here",
            "just", "like", "long", "make", "many", "over", "such", "take", "than", "them", "well", "were", "what"
        );

        return commonWords.contains(word.toLowerCase());
    }

    private List<String> getFallbackWords(int count) {
        logger.info("Using fallback vocabulary words");

        List<String> fallbackWords = Arrays.asList(
            "eloquent", "resilient", "meticulous", "ubiquitous", "serendipity",
            "ephemeral", "pragmatic", "contemplative", "inevitable", "sophisticated",
            "ambiguous", "diligent", "substantiate", "inherent", "prominent",
            "comprehensive", "deteriorate", "facilitate", "accumulated", "preliminary",
            "substantial", "accommodate", "investigate", "demonstrate", "appreciate",
            "anticipate", "equivalent", "fundamental", "adequate", "appropriate",
            "consistent", "distinct", "establish", "maintain", "acquire",
            "significant", "relevant", "specific", "particular", "individual",
            "efficient", "effective", "beneficial", "advantageous", "crucial",
            "essential", "vital", "critical", "accurate", "precise",
            "reliable", "dependable", "trustworthy", "credible", "authentic",
            "innovative", "creative", "imaginative", "resourceful", "versatile",
            "adaptable", "flexible", "dynamic", "progressive", "advanced",
            "complex", "complicated", "intricate", "elaborate", "detailed",
            "thorough", "extensive", "widespread", "universal", "global"
        );

        Collections.shuffle(fallbackWords);
        return fallbackWords.subList(0, Math.min(count, fallbackWords.size()));
    }

    public enum VocabularyLevel {
        INTERMEDIATE("intermediate"),
        ADVANCED("advanced"),
        EXPERT("expert");

        private final String description;

        VocabularyLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum VocabularyCategory {
        ACADEMIC("academic"),
        BUSINESS("business"),
        SCIENTIFIC("scientific"),
        LITERARY("literary"),
        GENERAL("general");

        private final String description;

        VocabularyCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
