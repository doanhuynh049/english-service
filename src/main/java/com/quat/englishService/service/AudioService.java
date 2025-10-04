package com.quat.englishService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class AudioService {

    private static final Logger logger = LoggerFactory.getLogger(AudioService.class);

    @Value("${app.audio.storage-path:/tmp/vocabulary-audio}")
    private String audioStoragePath;

    @Value("${app.audio.base-url:http://localhost:8282/audio}")
    private String audioBaseUrl;

    @Value("${app.audio.python-script-path:scripts/tts_generator.py}")
    private String pythonScriptPath;

    @Value("${app.audio.vocabulary-script-path:scripts/vocabulary_tts_generator.py}")
    private String vocabularyScriptPath;

    @Value("${app.audio.speed.word:1.0}")
    private double wordSpeedFactor;

    @Value("${app.audio.speed.sentence:1.2}")
    private double sentenceSpeedFactor;

    @Value("${app.audio.speed.passage:1.3}")
    private double passageSpeedFactor;

    @Value("${app.audio.speed.monologue:1.25}")
    private double monologueSpeedFactor;

    private static final int TIMEOUT_SECONDS = 30;
    private static final int MONOLOGUE_TIMEOUT_SECONDS = 200; // 2 minutes for long monologues

    public AudioInfo generateAudioFiles(String word, String exampleSentence) {
        try {
            // Create storage directory structure
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dailyStoragePath = Paths.get(audioStoragePath, dateFolder);
            Files.createDirectories(dailyStoragePath);

            // Generate file names
            String wordFileName = sanitizeFilename(word) + "_pronunciation.mp3";
            String exampleFileName = sanitizeFilename(word) + "_example.mp3";

            Path wordAudioPath = dailyStoragePath.resolve(wordFileName);
            Path exampleAudioPath = dailyStoragePath.resolve(exampleFileName);

            // Generate pronunciation audio
            boolean wordSuccess = generateSingleAudio(word, wordAudioPath.toString(), "word", TIMEOUT_SECONDS);

            // Generate example sentence audio
            boolean exampleSuccess = generateSingleAudio(exampleSentence, exampleAudioPath.toString(), "sentence", TIMEOUT_SECONDS);

            if (wordSuccess && exampleSuccess) {
                String wordUrl = audioBaseUrl + "/" + dateFolder + "/" + wordFileName;
                String exampleUrl = audioBaseUrl + "/" + dateFolder + "/" + exampleFileName;

                logger.info("Successfully generated audio files for word: {}", word);
                return new AudioInfo(wordUrl, exampleUrl, wordAudioPath.toString(), exampleAudioPath.toString());
            } else {
                logger.error("Failed to generate audio files for word: {}", word);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating audio files for word '{}': {}", word, e.getMessage(), e);
            return null;
        }
    }

    public AudioInfo generateAudioFilesWithMonologue(String word, String monologue) {
        try {
            // Create storage directory structure
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dailyStoragePath = Paths.get(audioStoragePath, dateFolder);
            Files.createDirectories(dailyStoragePath);

            // Generate file names
            String wordFileName = sanitizeFilename(word) + "_pronunciation.mp3";
            String monologueFileName = sanitizeFilename(word) + "_monologue.mp3";

            Path wordAudioPath = dailyStoragePath.resolve(wordFileName);
            Path monologueAudioPath = dailyStoragePath.resolve(monologueFileName);

            // Clean the monologue text before generating audio
            String cleanMonologue = cleanTextForTTS(monologue);
            logger.info("Cleaned monologue for TTS - Original: {} chars, Cleaned: {} chars",
                       monologue.length(), cleanMonologue.length());

            // Generate pronunciation audio with standard timeout
            boolean wordSuccess = generateSingleAudio(word, wordAudioPath.toString(), "word", TIMEOUT_SECONDS);

            // Generate monologue audio with extended timeout for longer content
            boolean monologueSuccess = generateSingleAudio(cleanMonologue, monologueAudioPath.toString(), "monologue", MONOLOGUE_TIMEOUT_SECONDS);

            if (wordSuccess && monologueSuccess) {
                String wordUrl = audioBaseUrl + "/" + dateFolder + "/" + wordFileName;
                String monologueUrl = audioBaseUrl + "/" + dateFolder + "/" + monologueFileName;

                logger.info("Successfully generated audio files with monologue for word: {}", word);
                return new AudioInfo(wordUrl, monologueUrl, wordAudioPath.toString(), monologueAudioPath.toString());
            } else {
                logger.error("Failed to generate audio files with monologue for word: {}", word);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating audio files with monologue for word '{}': {}", word, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate vocabulary-specific audio files (pronunciation + combined)
     * Uses the specialized vocabulary TTS script for better control
     */
    public AudioInfo generateVocabularyAudioFiles(String word, String definition, String example) {
        try {
            // Create storage directory structure
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dailyStoragePath = Paths.get(audioStoragePath, dateFolder);
            Files.createDirectories(dailyStoragePath);

            // Generate file names
            String wordFileName = sanitizeFilename(word) + "_pronunciation.mp3";

            Path wordAudioPath = dailyStoragePath.resolve(wordFileName);

            // Use the vocabulary-specific script
            boolean success = generateVocabularyAudioPair(
                word, 
                example != null ? example : "",
                wordAudioPath.toString()
            );

            if (success) {
                String wordUrl = audioBaseUrl + "/" + dateFolder + "/" + wordFileName;

                logger.info("Successfully generated vocabulary audio files for word: {}", word);
                return new AudioInfo(wordUrl, "", wordAudioPath.toString(), "");
            } else {
                logger.error("Failed to generate vocabulary audio files for word: {}", word);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error generating vocabulary audio files for word '{}': {}", word, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate vocabulary audio pair using the specialized Python script
     */
    private boolean generateVocabularyAudioPair(String word, String example, String pronunciationPath) {
        try {
            StringBuilder combinedText = new StringBuilder();
            combinedText.append(word).append(". ");
            
            if (!example.isEmpty()) {
                combinedText.append("Example: ").append(example).append(".");
            }

            boolean pronunciationSuccess = generateSingleAudio(
                combinedText.toString(), pronunciationPath, "monologue", MONOLOGUE_TIMEOUT_SECONDS
            );

            return pronunciationSuccess;

        } catch (Exception e) {
            logger.error("Error in fallback vocabulary audio generation for word '{}': {}", word, e.getMessage(), e);
            return false;
        }
    }

    private String cleanTextForTTS(String text) {
        if (text == null) return "";

        // Remove markdown formatting
        String cleaned = text
                .replaceAll("\\*\\*([^*]+?)\\*\\*", "$1") // Remove bold **text**
                .replaceAll("\\*([^*]+?)\\*", "$1")       // Remove italic *text*
                .replaceAll("_([^_]+?)_", "$1")           // Remove italic _text_
                .replaceAll("#+\\s*", "")                 // Remove headers
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1") // Remove links [text](url)
                .replaceAll("\\[([^\\]]+)\\]", "$1")      // Remove brackets [text]
                .replaceAll("`([^`]+)`", "$1")            // Remove code `text`
                .replaceAll("^\\s*>\\s*", "")             // Remove quote markers
                .replaceAll("^\\s*[-*+]\\s*", "")         // Remove list markers
                .replaceAll("^\\s*\\d+\\.\\s*", "");      // Remove numbered list markers

        // Clean up extra whitespace and normalize
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // Ensure the text isn't too long for TTS (some providers have limits)
        if (cleaned.length() > 5000) {
            logger.warn("Monologue text is very long ({} chars), truncating to 5000 chars", cleaned.length());
            cleaned = cleaned.substring(0, 5000) + "...";
        }

        return cleaned;
    }

    public boolean generateSingleAudio(String text, String outputPath, String type, int timeoutSeconds) {
        try {
            // Ensure Python script exists
            File scriptFile = new File(pythonScriptPath);
            if (!scriptFile.exists()) {
                return false;
            }

            logger.debug("Generating {} audio with timeout {} seconds for text: {} chars",
                        type, timeoutSeconds, text.length());

            // Determine speed factor based on audio type
            double speedFactor = getSpeedFactor(type);
            
            // Build Python command with speed factor
            ProcessBuilder pb = new ProcessBuilder(
                "python3", pythonScriptPath, text, outputPath, type, String.valueOf(speedFactor)
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                logger.error("Python TTS process timed out after {} seconds for {} (text length: {} chars)",
                           timeoutSeconds, type, text.length());
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Python TTS process failed with exit code {} for: {} (length: {} chars)",
                           exitCode, type, text.length());
                return false;
            }

            // Verify file was created
            File outputFile = new File(outputPath);
            if (outputFile.exists() && outputFile.length() > 0) {
                logger.debug("Successfully generated {} audio: {} ({} bytes) with speed factor {}x", 
                           type, outputPath, outputFile.length(), speedFactor);
                return true;
            } else {
                logger.error("Audio file was not created or is empty: {}", outputPath);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error running Python TTS for {} (length: {} chars): {}", type, text.length(), e.getMessage(), e);
            return false;
        }
    }

    private double getSpeedFactor(String type) {
        // Configure speed based on audio type using configurable properties
        switch (type.toLowerCase()) {
            case "word":
                return wordSpeedFactor;
            case "sentence":
                return sentenceSpeedFactor;
            case "passage":
                return passageSpeedFactor;
            case "monologue":
                return monologueSpeedFactor;
            default:
                return sentenceSpeedFactor; // Default to sentence speed
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }

    public static class AudioInfo {
        private final String pronunciationUrl;
        private final String exampleUrl;
        private final String pronunciationPath;
        private final String examplePath;

        public AudioInfo(String pronunciationUrl, String exampleUrl, String pronunciationPath, String examplePath) {
            this.pronunciationUrl = pronunciationUrl;
            this.exampleUrl = exampleUrl;
            this.pronunciationPath = pronunciationPath;
            this.examplePath = examplePath;
        }

        public String getPronunciationUrl() { return pronunciationUrl; }
        public String getExampleUrl() { return exampleUrl; }
        public String getPronunciationPath() { return pronunciationPath; }
        public String getExamplePath() { return examplePath; }
    }
}
