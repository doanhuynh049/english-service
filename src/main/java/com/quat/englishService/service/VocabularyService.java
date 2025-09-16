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
    private final MonologueDocumentService monologueDocumentService;
    private final ExecutorService executorService;
    private static final int NUMBER_VOCABULARY_WORDS = 3; // Total words: 3 fresh + 1 used
    // private static final int FRESH_WORDS_COUNT = 2; // Number of new words to generate
    private static final int USED_WORDS_COUNT = 1; // Number of used words to include for review
    public VocabularyService(GeminiClient geminiClient, EmailService emailService,
                           ExcelService excelService, VocabularyParsingService parsingService,
                           AudioService audioService, VocabularyGeneratorService vocabularyGenerator,
                           MonologueDocumentService monologueDocumentService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.excelService = excelService;
        this.parsingService = parsingService;
        this.audioService = audioService;
        this.vocabularyGenerator = vocabularyGenerator;
        this.monologueDocumentService = monologueDocumentService;
        this.executorService = Executors.newFixedThreadPool(8); // Increased for audio processing
    }

    public void processDailyVocabulary() {
        logger.info("Starting comprehensive daily vocabulary processing with AI-generated words and audio...");

        try {
            // Get previously used words from Excel to avoid duplicates
            Set<String> usedWords = excelService.getUsedWords();
            logger.info("Found {} previously used words", usedWords.size());

            // Generate vocabulary words with mix of fresh and used words (3 new + 1 used)
            List<String> selectedWords = generateMixedVocabularyWords(NUMBER_VOCABULARY_WORDS, usedWords);
            logger.info("Generated {} vocabulary words for today (mix of new and used): {}", selectedWords.size(), selectedWords);

            if (selectedWords.isEmpty()) {
                logger.warn("No fresh words generated, using fallback vocabulary");
                selectedWords = vocabularyGenerator.getRandomToeicVocabulary(3);
            }

            // Process words with AI, audio generation, and parsing
            List<ParsedVocabularyWord> processedWords = processWordsComprehensively(selectedWords);

            if (!processedWords.isEmpty()) {
                // Generate monologue document for email attachment
                String documentPath = monologueDocumentService.generateMonologueDocument(processedWords);

                // Send enhanced HTML email with audio links and monologue transcript
                emailService.sendVocabularyEmailWithDocument(processedWords, documentPath);

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
            List<String> candidateWords = vocabularyGenerator.getRandomToeicVocabulary(wordsNeeded);

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

    private List<String> generateMixedVocabularyWords(int totalCount, Set<String> usedWords) {
        logger.info("Generating {} vocabulary words with mix of fresh and used words", totalCount);
        
        // Calculate how many fresh vs used words to generate
        int freshWordsCount = totalCount - USED_WORDS_COUNT; // 3 fresh words
        int usedWordsCount = USED_WORDS_COUNT; // 1 used word
        
        List<String> result = new ArrayList<>();
        
        // Step 1: Generate fresh words (avoiding used words)
        if (freshWordsCount > 0) {
            List<String> freshWords = generateFreshVocabularyWords(freshWordsCount, usedWords);
            result.addAll(freshWords);
            logger.info("Added {} fresh words: {}", freshWords.size(), freshWords);
        }
        
        // Step 2: Add one used word for review/reinforcement
        if (usedWordsCount > 0 && !usedWords.isEmpty()) {
            List<String> usedWordsList = new ArrayList<>(usedWords);
            Collections.shuffle(usedWordsList);
            String selectedUsedWord = usedWordsList.get(0);
            result.add(selectedUsedWord);
            logger.info("Added 1 used word for review: {}", selectedUsedWord);
        } else if (usedWordsCount > 0 && usedWords.isEmpty()) {
            // Fallback: if no used words available, generate one more fresh word
            logger.info("No used words available, generating additional fresh word");
            List<String> additionalFresh = generateFreshVocabularyWords(1, usedWords);
            result.addAll(additionalFresh);
        }
        
        // Shuffle the final result to mix fresh and used words
        Collections.shuffle(result);
        
        logger.info("Successfully generated {} mixed vocabulary words: {} fresh, {} used", 
                   result.size(), freshWordsCount, Math.min(usedWordsCount, usedWords.size()));
        
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
            logger.info("Processing word comprehensively with AI monologue: {}", word);

            // Step 1: Get detailed AI explanation using the existing method
            String aiResponse = geminiClient.getWordExplanation(word);
            logger.info("Received AI response for word: {}", aiResponse);

            // Step 2: Parse the AI response into structured data
            ParsedVocabularyWord parsedWord = parsingService.parseAIResponse(word, aiResponse);
            logger.info("Parsed AI response for word: {}", word);

            // Step 3: Generate AI monologue for better audio examples
            String monologueResponse = geminiClient.getWordMonologue(word);
            logger.info("Received AI monologue for monologueResponse: {}", monologueResponse);
            if (monologueResponse != null) {
                // Store the monologue response in the parsed word for document generation
                // Format it properly so MonologueDocumentService can extract it
                String formattedMonologue = formatMonologueForStorage(monologueResponse);
                logger.info("Formatted monologue for storage: {}", formattedMonologue.substring(0, Math.min(100, formattedMonologue.length())) + "...");
                parsedWord.setRawExplanation(parsedWord.getRawExplanation() + "\n\n" + formattedMonologue);

                VocabularyParsingService.MonologueInfo monologueInfo = parsingService.parseMonologue(monologueResponse);
                logger.info("Parsed monologue for monologueInfo.getMonologue(): {}", monologueInfo != null ? monologueInfo.getMonologue().substring(0, Math.min(100, monologueInfo.getMonologue().length())) + "..." : "null");
                if (monologueInfo != null && !monologueInfo.getMonologue().isEmpty()) {
                    // Step 4: Generate audio files using the monologue
                    AudioService.AudioInfo audioInfo = audioService.generateAudioFilesWithMonologue(word, monologueInfo.getMonologue());

                    if (audioInfo != null) {
                        parsedWord.setPronunciationAudioUrl(audioInfo.getPronunciationUrl());
                        parsedWord.setExampleAudioUrl(audioInfo.getExampleUrl());
                        parsedWord.setPronunciationAudioPath(audioInfo.getPronunciationPath());
                        parsedWord.setExampleAudioPath(audioInfo.getExamplePath());

                        logger.info("Generated audio files with AI monologue for word: {}", word);
                    } else {
                        logger.warn("Failed to generate audio files with monologue for word: {}", word);
                        // Fallback to traditional approach
                        generateFallbackAudio(word, parsedWord);
                    }
                } else {
                    logger.warn("Failed to parse monologue for word: {}, falling back to traditional audio", word);
                    generateFallbackAudio(word, parsedWord);
                }
            } else {
                logger.warn("Failed to generate monologue for word: {}, falling back to traditional audio", word);
                generateFallbackAudio(word, parsedWord);
            }

            logger.debug("Successfully processed word: {} with {} parsed sections",
                    word, countNonNullFields(parsedWord));

            return parsedWord;

        } catch (Exception e) {
            logger.error("Error processing word '{}': {}", word, e.getMessage(), e);
            return null; // Will be filtered out
        }
    }

    private void generateFallbackAudio(String word, ParsedVocabularyWord parsedWord) {
        try {
            String exampleSentence = getFirstExampleSentence(parsedWord);
            if (exampleSentence != null) {
                AudioService.AudioInfo audioInfo = audioService.generateAudioFiles(word, exampleSentence);

                if (audioInfo != null) {
                    parsedWord.setPronunciationAudioUrl(audioInfo.getPronunciationUrl());
                    parsedWord.setExampleAudioUrl(audioInfo.getExampleUrl());
                    parsedWord.setPronunciationAudioPath(audioInfo.getPronunciationPath());
                    parsedWord.setExampleAudioPath(audioInfo.getExamplePath());

                    logger.info("Generated fallback audio files for word: {}", word);
                }
            }
        } catch (Exception e) {
            logger.error("Error generating fallback audio for word '{}': {}", word, e.getMessage(), e);
        }
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

    // Manual processing method with email sending
    public List<ParsedVocabularyWord> processSpecificWordsWithEmail(List<String> words) {
        logger.info("Processing {} specific words manually with email sending", words.size());
        
        try {
            // Process words with AI, audio generation, and parsing
            List<ParsedVocabularyWord> processedWords = processWordsComprehensively(words);

            if (!processedWords.isEmpty()) {
                // Generate monologue document for email attachment
                String documentPath = monologueDocumentService.generateMonologueDocument(processedWords);

                // Send enhanced HTML email with audio links and monologue transcript
                emailService.sendVocabularyEmailWithDocument(processedWords, documentPath);

                // Log to Excel with all details including audio paths
                logWordsToExcel(processedWords);

                logger.info("Manual vocabulary processing with email completed successfully with {} words", processedWords.size());
            } else {
                logger.error("No words were successfully processed");
            }

            return processedWords;

        } catch (Exception e) {
            logger.error("Error during manual vocabulary processing with email: {}", e.getMessage(), e);
            throw new RuntimeException("Manual vocabulary processing with email failed", e);
        }
    }

    /**
     * Generate TOEIC vocabulary words using weighted categories
     */
    public List<String> generateToeicVocabulary(int count) {
        logger.info("Generating {} TOEIC vocabulary words with weighted categories", count);
        return vocabularyGenerator.getRandomToeicVocabulary(count);
    }

    private String formatMonologueForStorage(String rawMonologue) {
        if (rawMonologue == null || rawMonologue.trim().isEmpty()) {
            return "";
        }

        // If the raw monologue already has the proper markdown format, return as-is
        if (rawMonologue.contains("**Monologue:**")) {
            return rawMonologue;
        }

        // Otherwise, format it properly for MonologueDocumentService extraction
        StringBuilder formatted = new StringBuilder();
        formatted.append("=== AI MONOLOGUE DATA ===\n");

        // Ensure proper markdown formatting
        if (rawMonologue.contains("Monologue:")) {
            // Replace plain format with markdown format
            String formattedContent = rawMonologue
                    .replaceAll("(?i)Monologue:", "**Monologue:**")
                    .replaceAll("(?i)Explanation:", "**Explanation:**")
                    .replaceAll("(?i)Pronunciation:", "**Pronunciation:**");
            formatted.append(formattedContent);
        } else {
            // If no clear structure, wrap the entire content as a monologue
            formatted.append("**Monologue:**\n");
            formatted.append(rawMonologue);
            formatted.append("\n**Explanation:**\n");
            formatted.append("Natural conversational monologue demonstrating word usage in context.");
        }

        return formatted.toString();
    }
}
