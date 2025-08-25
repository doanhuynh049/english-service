package com.quat.englishService.service;

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

    public VocabularyService(GeminiClient geminiClient, EmailService emailService, ExcelService excelService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.excelService = excelService;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public void processDailyVocabulary() {
        logger.info("Starting daily vocabulary processing...");

        try {
            // Select 10 random words
            List<String> selectedWords = selectRandomWords(10);
            logger.info("Selected words for today: {}", selectedWords);

            // Get explanations for all words concurrently
            List<VocabularyWord> vocabularyWords = getWordExplanationsConcurrently(selectedWords);

            // Format and send email
            String emailContent = formatVocabularyForEmail(vocabularyWords);
            emailService.sendVocabularyEmail(emailContent);

            // Save to Excel file
            excelService.saveVocabularyWords(vocabularyWords);

            logger.info("Daily vocabulary processing completed successfully");

        } catch (Exception e) {
            logger.error("Error during daily vocabulary processing: {}", e.getMessage(), e);
        }
    }

    private List<String> selectRandomWords(int count) {
        List<String> shuffled = new ArrayList<>(vocabularyWords);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private List<VocabularyWord> getWordExplanationsConcurrently(List<String> words) {
        logger.info("Getting explanations for {} words concurrently", words.size());

        List<CompletableFuture<VocabularyWord>> futures = words.stream()
                .map(word -> CompletableFuture.supplyAsync(() -> {
                    logger.info("Processing word: {}", word);
                    String explanation = geminiClient.getWordExplanation(word);
                    return new VocabularyWord(word, explanation);
                }, executorService))
                .toList();

        // Wait for all API calls to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.get(); // Wait for completion
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting word explanations concurrently: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String formatVocabularyForEmail(List<VocabularyWord> words) {
        StringBuilder content = new StringBuilder();
        content.append("Today's English Vocabulary Learning Session\n");
        content.append("==========================================\n\n");

        for (int i = 0; i < words.size(); i++) {
            VocabularyWord word = words.get(i);
            content.append(String.format("Word %d: %s\n", i + 1, word.getWord().toUpperCase()));
            content.append("─".repeat(50)).append("\n");
            content.append(word.getExplanation());
            content.append("\n\n");

            if (i < words.size() - 1) {
                content.append("═".repeat(80)).append("\n\n");
            }
        }

        return content.toString();
    }

    // Manual trigger method for testing
    public void triggerManualVocabularySession() {
        logger.info("Manual vocabulary session triggered");
        processDailyVocabulary();
    }
}
