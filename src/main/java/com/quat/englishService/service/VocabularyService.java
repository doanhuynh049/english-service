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
    private final ExecutorService executorService;

    // Predefined list of English words for vocabulary learning
    private final List<String> vocabularyWords = Arrays.asList(
            "eloquent", "resilient", "meticulous", "ubiquitous", "serendipity",
            "ephemeral", "pragmatic", "contemplative", "inevitable", "sophisticated",
            "ambiguous", "diligent", "substantiate", "inherent", "prominent",
            "comprehensive", "deteriorate", "facilitate", "accumulated", "preliminary",
            "consecutive", "preliminary", "substantial", "accommodate", "investigate",
            "demonstrate", "appreciate", "anticipate", "equivalent", "fundamental",
            "adequate", "appropriate", "consistent", "distinct", "establish",
            "obtain", "maintain", "acquire", "require", "identify",
            "significant", "relevant", "specific", "particular", "individual",
            "considerable", "sufficient", "obvious", "apparent", "evident",
            "diverse", "various", "numerous", "extensive", "comprehensive",
            "efficient", "effective", "beneficial", "advantageous", "crucial",
            "essential", "vital", "critical", "important", "necessary",
            "accurate", "precise", "exact", "correct", "appropriate",
            "suitable", "relevant", "applicable", "compatible", "consistent",
            "reliable", "dependable", "trustworthy", "credible", "authentic",
            "valid", "legitimate", "genuine", "original", "innovative",
            "creative", "imaginative", "resourceful", "versatile", "adaptable",
            "flexible", "dynamic", "progressive", "advanced", "sophisticated",
            "complex", "complicated", "intricate", "elaborate", "detailed",
            "thorough", "comprehensive", "extensive", "widespread", "universal",
            "global", "international", "national", "regional", "local",
            "community", "society", "culture", "tradition", "heritage",
            "legacy", "impact", "influence", "effect", "consequence",
            "result", "outcome", "achievement", "success", "accomplishment"
    );

    public VocabularyService(GeminiClient geminiClient, EmailService emailService,
                           ExcelService excelService, VocabularyParsingService parsingService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.excelService = excelService;
        this.parsingService = parsingService;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public void processDailyVocabulary() {
        logger.info("Starting enhanced daily vocabulary processing...");

        try {
            // Select 10 random words
            List<String> selectedWords = selectRandomWords(1);
            logger.info("Selected words for today: {}", selectedWords);

            // Get explanations and parse them concurrently
            List<ParsedVocabularyWord> parsedWords = getAndParseWordExplanationsConcurrently(selectedWords);

            // Send enhanced HTML email
            emailService.sendVocabularyEmail(parsedWords);

            // Convert to VocabularyWord objects for Excel logging
            List<VocabularyWord> vocabularyWords = parsedWords.stream()
                    .map(parsed -> new VocabularyWord(parsed.getWord(), parsed.getRawExplanation()))
                    .toList();

            // Save to Excel file
            excelService.saveVocabularyWords(vocabularyWords);

            logger.info("Enhanced daily vocabulary processing completed successfully with {} words", parsedWords.size());

        } catch (Exception e) {
            logger.error("Error during enhanced daily vocabulary processing: {}", e.getMessage(), e);
        }
    }

    private List<String> selectRandomWords(int count) {
        List<String> shuffled = new ArrayList<>(vocabularyWords);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private List<ParsedVocabularyWord> getAndParseWordExplanationsConcurrently(List<String> words) {
        logger.info("Getting and parsing explanations for {} words concurrently", words.size());

        List<CompletableFuture<ParsedVocabularyWord>> futures = words.stream()
                .map(word -> CompletableFuture.supplyAsync(() -> {
                    logger.info("Processing and parsing word: {}", word);

                    // Get AI explanation
                    String aiResponse = geminiClient.getWordExplanation(word);

                    // Parse the AI response into structured data
                    ParsedVocabularyWord parsedWord = parsingService.parseAIResponse(word, aiResponse);

                    logger.debug("Successfully parsed word: {} with {} sections",
                            word, countNonNullFields(parsedWord));

                    return parsedWord;
                }, executorService))
                .toList();

        // Wait for all API calls and parsing to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.get(); // Wait for completion
            List<ParsedVocabularyWord> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            logger.info("Successfully processed {} words with enhanced parsing", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Error getting and parsing word explanations concurrently: {}", e.getMessage(), e);
            return Collections.emptyList();
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
        return count;
    }

    // Manual trigger method for testing
    public void triggerManualVocabularySession() {
        logger.info("Manual enhanced vocabulary session triggered");
        processDailyVocabulary();
    }
}
