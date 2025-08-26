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

    private boolean generateSingleAudio(String text, String outputPath, String type) {
        return generateSingleAudio(text, outputPath, type, TIMEOUT_SECONDS);
    }

    private boolean generateSingleAudio(String text, String outputPath, String type, int timeoutSeconds) {
        try {
            // Ensure Python script exists
            File scriptFile = new File(pythonScriptPath);
            if (!scriptFile.exists()) {
                createPythonScript();
            }

            logger.debug("Generating {} audio with timeout {} seconds for text: {} chars",
                        type, timeoutSeconds, text.length());

            // Build Python command
            ProcessBuilder pb = new ProcessBuilder(
                "python3", pythonScriptPath, text, outputPath, type
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
                logger.debug("Successfully generated {} audio: {} ({} bytes)", type, outputPath, outputFile.length());
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

    private void createPythonScript() throws IOException {
        Path scriptDir = Paths.get("scripts");
        Files.createDirectories(scriptDir);

        String pythonScript = """
            #!/usr/bin/env python3
            import sys
            import os
            from gtts import gTTS
            import logging
            
            # Set up logging
            logging.basicConfig(level=logging.INFO)
            logger = logging.getLogger(__name__)
            
            def generate_audio(text, output_path, audio_type):
                try:
                    # Clean the text
                    text = text.strip()
                    if not text:
                        logger.error("Empty text provided")
                        return False
                    
                    # Create directory if it doesn't exist
                    os.makedirs(os.path.dirname(output_path), exist_ok=True)
                    
                    # Configure TTS settings based on type
                    if audio_type == "word":
                        # For single words, use slower speech
                        tts = gTTS(text=text, lang='en', slow=True)
                    else:
                        # For sentences and monologues, use normal speed
                        tts = gTTS(text=text, lang='en', slow=False)
                    
                    # Save the audio file
                    tts.save(output_path)
                    
                    # Verify file was created
                    if os.path.exists(output_path) and os.path.getsize(output_path) > 0:
                        logger.info(f"Successfully generated {audio_type} audio: {output_path}")
                        return True
                    else:
                        logger.error(f"Failed to create audio file: {output_path}")
                        return False
                        
                except Exception as e:
                    logger.error(f"Error generating TTS for '{text}': {str(e)}")
                    return False
            
            if __name__ == "__main__":
                if len(sys.argv) != 4:
                    print("Usage: python3 tts_generator.py <text> <output_path> <type>")
                    sys.exit(1)
                
                text = sys.argv[1]
                output_path = sys.argv[2]
                audio_type = sys.argv[3]
                
                success = generate_audio(text, output_path, audio_type)
                sys.exit(0 if success else 1)
            """;

        Files.write(Paths.get(pythonScriptPath), pythonScript.getBytes());
        logger.info("Created Python TTS script at: {}", pythonScriptPath);
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
