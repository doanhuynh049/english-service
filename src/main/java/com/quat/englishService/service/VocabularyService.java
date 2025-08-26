package com.quat.englishService.service;

import com.quat.englishService.dto.ParsedVocabularyWord;
import com.quat.englishService.model.VocabularyWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VocabularyService {

    private static final Logger logger = LoggerFactory.getLogger(VocabularyService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;
    private final ExcelService excelService;
    private final VocabularyParsingService parsingService;
    private final AudioService audioService;
    private final VocabularyGeneratorService vocabularyGenerator;
    private final ExecutorService executorService;

    public VocabularyService(GeminiClient geminiClient, EmailService emailService,
                           ExcelService excelService, VocabularyParsingService parsingService,
                           AudioService audioService, VocabularyGeneratorService vocabularyGenerator) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.excelService = excelService;
        this.parsingService = parsingService;
        this.audioService = audioService;
        this.vocabularyGenerator = vocabularyGenerator;
        this.executorService = Executors.newFixedThreadPool(8); // Increased for audio processing
    }

    public void processDailyVocabulary() {
        logger.info("Starting comprehensive daily vocabulary processing with AI-generated words and audio...");

        try {
            // Get previously used words from Excel to avoid duplicates
            Set<String> usedWords = excelService.getUsedWords();
            logger.info("Found {} previously used words", usedWords.size());

            // Generate fresh vocabulary words from Gemini AI
            List<String> selectedWords = generateFreshVocabularyWords(1, usedWords);
            logger.info("Generated {} new AI vocabulary words for today: {}", selectedWords.size(), selectedWords);

            if (selectedWords.isEmpty()) {
                logger.warn("No fresh words generated, using fallback vocabulary");
                selectedWords = vocabularyGenerator.getRandomMixedVocabulary(1);
            }

            // Process words with AI, audio generation, and parsing
            List<ParsedVocabularyWord> processedWords = processWordsComprehensively(selectedWords);

            if (!processedWords.isEmpty()) {
                // Send enhanced HTML email with audio links
                emailService.sendVocabularyEmail(processedWords);

                // Log to Excel with all details including audio paths
                logWordsToExcel(processedWords);

                logger.info("Daily vocabulary processing completed successfully with {} words", processedWords.size());
            } else {
                logger.error("No words were successfully processed");
            }

        } catch (Exception e) {
            logger.error("Error during daily vocabulary processing: {}", e.getMessage(), e);
            throw new RuntimeException("Daily vocabulary processing failed", e);
        }
    }

    private List<String> generateFreshVocabularyWords(int count, Set<String> usedWords) {
        logger.info("Generating {} fresh vocabulary words using AI, avoiding {} used words", count, usedWords.size());
        List<String> freshWords = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 5;

        while (freshWords.size() < count && attempts < maxAttempts) {
            attempts++;
            logger.debug("Attempt {} to generate vocabulary words", attempts);

            // Get mixed vocabulary from different categories and levels
            int wordsNeeded = Math.max(count * 2, 20); // Generate more than needed to filter duplicates
            List<String> candidateWords = vocabularyGenerator.getRandomMixedVocabulary(wordsNeeded);

            // Filter out used words and add to fresh list
            for (String word : candidateWords) {
                if (!usedWords.contains(word.toLowerCase()) && !freshWords.contains(word.toLowerCase())) {
                    freshWords.add(word);
                    if (freshWords.size() >= count) {
                        break;
                    }
                }
            }

            logger.debug("After attempt {}: {} fresh words found", attempts, freshWords.size());
        }

        // If we still don't have enough, supplement with different vocabulary categories
        if (freshWords.size() < count) {
            logger.info("Need {} more words, trying different vocabulary categories", count - freshWords.size());

            VocabularyGeneratorService.VocabularyLevel[] levels = VocabularyGeneratorService.VocabularyLevel.values();
            VocabularyGeneratorService.VocabularyCategory[] categories = VocabularyGeneratorService.VocabularyCategory.values();

            for (VocabularyGeneratorService.VocabularyLevel level : levels) {
                for (VocabularyGeneratorService.VocabularyCategory category : categories) {
                    if (freshWords.size() >= count) break;

                    List<String> categoryWords = vocabularyGenerator.getVocabularyWords(level, category, 10);
                    for (String word : categoryWords) {
                        if (!usedWords.contains(word.toLowerCase()) && !freshWords.contains(word.toLowerCase())) {
                            freshWords.add(word);
                            if (freshWords.size() >= count) break;
                        }
                    }
                }
                if (freshWords.size() >= count) break;
            }
        }

        // Shuffle and limit to requested count
        Collections.shuffle(freshWords);
        List<String> result = freshWords.subList(0, Math.min(count, freshWords.size()));

        logger.info("Successfully generated {} fresh vocabulary words using AI", result.size());
        return result;
    }

    private List<ParsedVocabularyWord> processWordsComprehensively(List<String> words) {
        logger.info("Processing {} words with AI explanation, audio generation, and parsing", words.size());

        List<CompletableFuture<ParsedVocabularyWord>> futures = words.stream()
                .map(word -> CompletableFuture.supplyAsync(() -> processWordCompletely(word), executorService))
                .toList();

        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(); // Wait for completion

            List<ParsedVocabularyWord> results = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull) // Filter out failed words
                    .toList();

            logger.info("Successfully processed {} out of {} words", results.size(), words.size());
            return results;

        } catch (Exception e) {
            logger.error("Error processing words comprehensively: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ParsedVocabularyWord processWordCompletely(String word) {
        try {
            logger.info("Processing word comprehensively: {}", word);

            // Step 1: Get detailed AI explanation using the existing method
            String aiResponse = geminiClient.getWordExplanation(word);
            logger.info("Received AI response for word: {}", aiResponse);

            // Step 2: Parse the AI response into structured data
            ParsedVocabularyWord parsedWord = parsingService.parseAIResponse(word, aiResponse);
            logger.info("Parsed AI response for word: {}", parsedWord);

            // Step 3: Generate audio files for pronunciation and example sentence
            String exampleSentence = getFirstExampleSentence(parsedWord);
            if (exampleSentence != null) {
                AudioService.AudioInfo audioInfo = audioService.generateAudioFiles(word, exampleSentence);

                if (audioInfo != null) {
                    parsedWord.setPronunciationAudioUrl(audioInfo.getPronunciationUrl());
                    parsedWord.setExampleAudioUrl(audioInfo.getExampleUrl());
                    parsedWord.setPronunciationAudioPath(audioInfo.getPronunciationPath());
                    parsedWord.setExampleAudioPath(audioInfo.getExamplePath());

                    logger.info("Generated audio files for word: {}", word);
                } else {
                    logger.warn("Failed to generate audio files for word: {}", word);
                }
            } else {
                logger.warn("No example sentence found for word: {}", word);
            }

            logger.debug("Successfully processed word: {} with {} parsed sections",
                    word, countNonNullFields(parsedWord));

            return parsedWord;

        } catch (Exception e) {
            logger.error("Error processing word '{}': {}", word, e.getMessage(), e);
            return null; // Will be filtered out
        }
    }

    private String buildDetailedPrompt(String word) {
        return String.format("""
                Teach me the word "%s" in detail. Include:
                - IPA pronunciation
                - Part of speech
                - English definition (simple + advanced if available)
                - 2–3 example sentences in natural English
                - Common collocations and fixed expressions with this word
                - Synonyms & antonyms (with slight differences explained)
                - Commonly confused words and how to distinguish them
                - Word family (noun, verb, adjective forms)
                - Vietnamese translation with nuance
                
                Please format your response clearly with labeled sections for easy parsing.
                """, word);
    }

    private String getFirstExampleSentence(ParsedVocabularyWord word) {
        if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) {
            return word.getExampleSentences()[0];
        }
        return null;
    }

    private void logWordsToExcel(List<ParsedVocabularyWord> words) {
        try {
            // Convert ParsedVocabularyWord to VocabularyWord for Excel logging
            List<VocabularyWord> vocabularyWords = words.stream()
                    .map(parsed -> {
                        VocabularyWord vocabWord = new VocabularyWord(parsed.getWord(), parsed.getRawExplanation());
                        // Add additional metadata if your VocabularyWord model supports it
                        return vocabWord;
                    })
                    .toList();

            excelService.saveVocabularyWords(vocabularyWords);

            // Also log detailed information with audio paths
            excelService.saveVocabularyWordsDetailed(words);

            logger.info("Successfully logged {} words to Excel with detailed information", words.size());

        } catch (Exception e) {
            logger.error("Error logging words to Excel: {}", e.getMessage(), e);
        }
    }

    private int countNonNullFields(ParsedVocabularyWord word) {
        int count = 0;
        if (word.getPronunciation() != null) count++;
        if (word.getPartOfSpeech() != null) count++;
        if (word.getSimpleDefinition() != null) count++;
        if (word.getAdvancedDefinition() != null) count++;
        if (word.getExampleSentences() != null && word.getExampleSentences().length > 0) count++;
        if (word.getCollocations() != null) count++;
        if (word.getSynonyms() != null) count++;
        if (word.getAntonyms() != null) count++;
        if (word.getConfusedWords() != null) count++;
        if (word.getWordFamily() != null) count++;
        if (word.getVietnameseTranslation() != null) count++;
        if (word.getPronunciationAudioUrl() != null) count++;
        if (word.getExampleAudioUrl() != null) count++;
        return count;
    }

    // Manual processing method for testing
    public List<ParsedVocabularyWord> processSpecificWords(List<String> words) {
        logger.info("Processing {} specific words manually", words.size());
        return processWordsComprehensively(words);
    }
}
