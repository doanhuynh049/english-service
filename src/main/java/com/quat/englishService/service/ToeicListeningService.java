package com.quat.englishService.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ToeicListeningService {

    private static final Logger logger = LoggerFactory.getLogger(ToeicListeningService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;
    private final AudioService audioService;
    private final CollocationHistoryService collocationHistoryService;
    private final ExecutorService executorService;
    private static final int NUMBER_PASSAGES = 3;

    private static final String PASSAGE_PROMPT_TEMPLATE = """
            Using the provided collocations, 
            %s
            . Create a TOEIC Part 4–style listening passage. The passage should:

            Be 150–180 words long.

            Be set in a realistic TOEIC-style context with a randomly chosen topic, 
            such as:
            - Travel and transportation (airport announcements, train stations, bus terminals, car rentals)
            - Hospitality and customer service (hotels, restaurants, tourism, retail stores, service centers)
            - Public announcements (events, museums, exhibitions, community centers, promotional offers)
            - Business and workplace (meetings, presentations, conference calls, training sessions)
            - Health and safety (medical facilities, health advisories, safety instructions)
            - Education and training (school announcements, course information, campus events)
            - Technology and services (product launches, service updates, tech support)
            - Environment and sustainability (green initiatives, recycling programs, conservation efforts)
            - Real estate and housing (property listings, open house announcements, rental information)
            - Finance and banking (account services, loan information, financial advice)
            Choose one of these topics randomly for each passage.
            Use 8–10 of the provided collocations naturally within the passage.
            Naturally include all of the given collocations.

            Match the tone and difficulty of TOEIC Listening Part 4 (score range 700–950).

            End with 3 multiple-choice comprehension questions (with 4 options each, A–D).

            Provide the correct answer key after the questions.

            Output Format clearly with:

            Chosen Topic:
            <Insert chosen topic here>

            Passage:
            <Insert passage text here>

            Questions:
            1. <Question 1>
            A. <Option A>
            B. <Option B>
            C. <Option C>
            D. <Option D>

            2. <Question 2>
            A. <Option A>
            B. <Option B>
            C. <Option C>
            D. <Option D>

            3. <Question 3>
            A. <Option A>
            B. <Option B>
            C. <Option C>
            D. <Option D>

            Answer Key:
            1. <Correct answer>
            2. <Correct answer>
            3. <Correct answer>

            Collocations to include:
            <Insert collocations here>""";


    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToeicListeningService(GeminiClient geminiClient, EmailService emailService, AudioService audioService, CollocationHistoryService collocationHistoryService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
        this.audioService = audioService;
        this.collocationHistoryService = collocationHistoryService;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void processDailyToeicListening() {
        logger.info("Starting TOEIC Listening collocation processing...");

        try {
            // Step 1: Get 3 review collocations from history
            List<Collocation> reviewCollocations = collocationHistoryService.getCollocationsForToday();
            logger.info("Retrieved {} review collocations", reviewCollocations.size());

            // Step 2: Generate 7 new collocations using updated prompt
            String newCollocationsPrompt = collocationHistoryService.generateNewCollocationsPrompt(reviewCollocations);
            String newCollocationsResponse = geminiClient.generateContent(newCollocationsPrompt);
            logger.info("Generated 7 new TOEIC collocations");

            // Step 3: Parse new collocations from JSON response
            List<Collocation> newCollocations = parseCollocationsFromJson(newCollocationsResponse);
            logger.info("Parsed {} new collocations from JSON", newCollocations.size());

            // Step 4: Combine review and new collocations (3 + 7 = 10 total)
            List<Collocation> allCollocations = new ArrayList<>();
            allCollocations.addAll(reviewCollocations);
            allCollocations.addAll(newCollocations);
            
            // Shuffle to mix review and new collocations
            Collections.shuffle(allCollocations);
            logger.info("Combined total {} collocations (3 review + 7 new)", allCollocations.size());

            // Step 5: Save new collocations to history
            collocationHistoryService.saveNewCollocations(newCollocations);
            logger.info("Saved new collocations to history");

            // Step 6: Build HTML content for email
            String collocationsHtmlContent = buildCollocationsHtmlContent(allCollocations);
            logger.info("Built HTML content for collocations");

            // Step 7: Extract simple collocation phrases for passage generation
            List<String> collocationPhrases = allCollocations.stream()
                    .map(Collocation::getCollocation)
                    .toList();
            logger.info("Extracted {} collocation phrases for passage generation", collocationPhrases.size());

            // Step 8: Generate 3 listening passages
            List<ListeningPassage> passages = generateListeningPassages(collocationPhrases);
            logger.info("Generated {} listening passages", passages.size());

            // Step 9: Generate audio files for all passages
            List<AudioFileInfo> audioFiles = generateAudioFiles(passages);
            logger.info("Generated {} audio files", audioFiles.size());

            // Step 10: Create text file with all passages
            String passagesFilePath = createPassagesTextFile(passages);

            // Step 11: Send email with structured collocations and attachments
            emailService.sendToeicListeningEmail(collocationsHtmlContent, audioFiles, passagesFilePath);
            logger.info("TOEIC Listening email sent successfully");

        } catch (Exception e) {
            logger.error("Error during TOEIC Listening processing: {}", e.getMessage(), e);
            throw new RuntimeException("TOEIC Listening processing failed", e);
        }
    }

    private List<Collocation> parseCollocationsFromJson(String jsonResponse) {
        List<Collocation> collocations = new ArrayList<>();
        try {
            // Clean the response to extract only the JSON part
            String cleanJson = extractJsonFromResponse(jsonResponse);
            
            JsonNode rootNode = objectMapper.readTree(cleanJson);
            JsonNode collocationsArray = rootNode.get("collocations");
            
            if (collocationsArray != null && collocationsArray.isArray()) {
                for (JsonNode collocationNode : collocationsArray) {
                    Collocation collocation = new Collocation(
                        collocationNode.get("collocation").asText(),
                        collocationNode.get("ipa").asText(),
                        collocationNode.get("meaning").asText(),
                        collocationNode.get("example").asText(),
                        collocationNode.get("vietnamese").asText()
                    );
                    collocations.add(collocation);
                }
            }
            
            logger.info("Successfully parsed {} collocations from JSON", collocations.size());
            
        } catch (Exception e) {
            logger.error("Error parsing collocations JSON: {}", e.getMessage(), e);
            // Fallback to old parsing method
            return parseCollocationsFromText(jsonResponse);
        }
        
        return collocations;
    }

    private String extractJsonFromResponse(String response) {
        // Find the start and end of JSON object
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        // If no valid JSON found, return the original response
        return response;
    }

    private List<Collocation> parseCollocationsFromText(String textResponse) {
        // Fallback method for non-JSON responses
        List<Collocation> collocations = new ArrayList<>();
        String[] lines = textResponse.split("\n");
        
        for (String line : lines) {
            if (line.trim().matches("^\\d+\\..*")) {
                // Extract basic collocation info from text format
                String collocationLine = line.trim().replaceFirst("^\\d+\\.", "").trim();
                if (collocationLine.contains(":")) {
                    String collocation = collocationLine.split(":")[0].trim();
                    if (!collocation.isEmpty()) {
                        collocations.add(new Collocation(
                            collocation, 
                            "N/A", 
                            "See email content", 
                            "See email content", 
                            "Xem nội dung email"
                        ));
                    }
                }
            }
        }
        
        logger.warn("Used fallback text parsing for {} collocations", collocations.size());
        return collocations;
    }

    public String buildCollocationsHtmlContent(List<Collocation> collocations) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class=\"collocations-container\">\n");
        html.append("  <h2 style=\"color: #2c5aa0; border-bottom: 2px solid #ff6b35; padding-bottom: 10px; margin-bottom: 20px;\">");
        html.append("📘 Business Collocations</h2>\n");
        
        for (int i = 0; i < collocations.size(); i++) {
            Collocation col = collocations.get(i);
            html.append("  <div class=\"collocation-item\" style=\"margin-bottom: 20px; padding: 15px; ");
            html.append("background-color: #f8f9fa; border-left: 4px solid #ff6b35; border-radius: 5px;\">\n");
            
            html.append("    <div class=\"collocation-header\" style=\"margin-bottom: 10px;\">\n");
            html.append("      <span class=\"number\" style=\"font-weight: bold; color: #2c5aa0; margin-right: 10px;\">");
            html.append(i + 1).append(".</span>\n");
            html.append("      <span class=\"phrase\" style=\"font-size: 18px; font-weight: bold; color: #333;\">");
            html.append(col.getCollocation()).append("</span>\n");
            html.append("      <span class=\"ipa\" style=\"margin-left: 10px; color: #666; font-style: italic;\">");
            html.append(col.getIpa()).append("</span>\n");
            html.append("    </div>\n");
            
            html.append("    <div class=\"meaning\" style=\"margin-bottom: 8px; color: #555;\">\n");
            html.append("      <strong>Meaning:</strong> ").append(col.getMeaning()).append("\n");
            html.append("    </div>\n");
            
            html.append("    <div class=\"example\" style=\"margin-bottom: 8px; font-style: italic; color: #444;\">\n");
            html.append("      <strong>Example:</strong> ").append(col.getExample()).append("\n");
            html.append("    </div>\n");
            
            html.append("    <div class=\"vietnamese\" style=\"color: #777; font-size: 14px;\">\n");
            html.append("      <strong>Vietnamese:</strong> ").append(col.getVietnamese()).append("\n");
            html.append("    </div>\n");
            
            html.append("  </div>\n");
        }
        
        html.append("</div>\n");
        
        return html.toString();
    }

    private List<ListeningPassage> generateListeningPassages(List<String> collocations) {
        List<ListeningPassage> passages = new ArrayList<>();
        String collocationsText = String.join("\n", collocations);

        // Generate 3 passages concurrently
        List<CompletableFuture<ListeningPassage>> futures = new ArrayList<>();
        
        for (int i = 1; i <= NUMBER_PASSAGES; i++) {
            final int passageNumber = i;
            CompletableFuture<ListeningPassage> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = String.format(PASSAGE_PROMPT_TEMPLATE, collocationsText);
                    logger.info("Generated prompt for passage {}: {}", passageNumber, prompt);
                    String passageResponse = geminiClient.generateContent(prompt);
                    return parseListeningPassage(passageResponse, passageNumber);
                } catch (Exception e) {
                    logger.error("Error generating passage {}: {}", passageNumber, e.getMessage(), e);
                    return null;
                }
            }, executorService);
            futures.add(future);
        }

        // Collect results
        for (CompletableFuture<ListeningPassage> future : futures) {
            try {
                ListeningPassage passage = future.get();
                if (passage != null) {
                    passages.add(passage);
                }
            } catch (Exception e) {
                logger.error("Error collecting passage result: {}", e.getMessage(), e);
            }
        }

        return passages;
    }

    private ListeningPassage parseListeningPassage(String response, int passageNumber) {
        // Parse the response to extract passage text and questions only
        String passageText = "";
        String questions = "";
        
        try {
            // First, find the "Passage:" section
            int passageStart = response.toLowerCase().indexOf("passage:");
            int questionsStart = response.toLowerCase().indexOf("questions:");
            int answerKeyStart = response.toLowerCase().indexOf("answer key:");
            
            if (passageStart != -1 && questionsStart != -1) {
                // Extract passage text between "Passage:" and "Questions:"
                passageText = response.substring(passageStart, questionsStart).trim();
                
                // Remove "Passage:" header
                passageText = passageText.replaceAll("(?i)^.*?passage\\s*:", "").trim();
                
                // Clean up any ** formatting around collocations (keep the collocations visible)
                passageText = passageText.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
                
                // Extract questions section
                if (answerKeyStart != -1) {
                    // Questions from "Questions:" to "Answer Key:"
                    String questionsOnly = response.substring(questionsStart, answerKeyStart).trim();
                    questionsOnly = questionsOnly.replaceAll("(?i)^.*?questions\\s*:", "").trim();
                    
                    // Answer key from "Answer Key:" to end (or until "Collocations to include:")
                    String answerKeySection = response.substring(answerKeyStart).trim();
                    answerKeySection = answerKeySection.replaceAll("(?i)^.*?answer key\\s*:", "").trim();
                    
                    // Remove "Collocations to include:" section if present
                    if (answerKeySection.toLowerCase().contains("collocations to include:")) {
                        int collocationsStart = answerKeySection.toLowerCase().indexOf("collocations to include:");
                        answerKeySection = answerKeySection.substring(0, collocationsStart).trim();
                    }
                    
                    // Combine questions and answer key
                    questions = questionsOnly + "\n\nAnswer Key:\n" + answerKeySection;
                } else {
                    // No separate answer key section, take everything after "Questions:"
                    questions = response.substring(questionsStart).trim();
                    questions = questions.replaceAll("(?i)^.*?questions\\s*:", "").trim();
                    
                    // Remove "Collocations to include:" section if present
                    if (questions.toLowerCase().contains("collocations to include:")) {
                        int collocationsStart = questions.toLowerCase().indexOf("collocations to include:");
                        questions = questions.substring(0, collocationsStart).trim();
                    }
                }
            } else {
                logger.warn("Could not find 'Passage:' and 'Questions:' sections in response for passage {}", passageNumber);
                
                // Fallback: try to extract using simple patterns
                String[] lines = response.split("\n");
                StringBuilder passageBuilder = new StringBuilder();
                StringBuilder questionsBuilder = new StringBuilder();
                boolean inPassage = false;
                boolean inQuestions = false;
                
                for (String line : lines) {
                    String lowerLine = line.toLowerCase().trim();
                    
                    if (lowerLine.startsWith("passage:")) {
                        inPassage = true;
                        inQuestions = false;
                        continue;
                    } else if (lowerLine.startsWith("questions:")) {
                        inPassage = false;
                        inQuestions = true;
                        continue;
                    } else if (lowerLine.startsWith("collocations to include:")) {
                        break; // Stop processing
                    }
                    
                    if (inPassage && !line.trim().isEmpty()) {
                        passageBuilder.append(line).append("\n");
                    } else if (inQuestions && !line.trim().isEmpty()) {
                        questionsBuilder.append(line).append("\n");
                    }
                }
                
                passageText = passageBuilder.toString().trim().replaceAll("\\*\\*(.*?)\\*\\*", "$1");
                questions = questionsBuilder.toString().trim();
            }
            
            logger.debug("Parsed passage {}: passageText length={}, questions length={}", 
                        passageNumber, passageText.length(), questions.length());
            
        } catch (Exception e) {
            logger.error("Error parsing passage {}: {}", passageNumber, e.getMessage(), e);
            
            // Last resort: use entire response but clean it
            passageText = response.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
            questions = "Could not parse questions from response.";
        }
        
        return new ListeningPassage(passageNumber, passageText, questions, response);
    }

    private List<AudioFileInfo> generateAudioFiles(List<ListeningPassage> passages) {
        List<AudioFileInfo> audioFiles = new ArrayList<>();
        
        // Generate audio files concurrently
        List<CompletableFuture<AudioFileInfo>> futures = passages.stream()
                .map(passage -> CompletableFuture.supplyAsync(() -> generateSingleAudioFile(passage), executorService))
                .toList();

        // Collect results
        for (CompletableFuture<AudioFileInfo> future : futures) {
            try {
                AudioFileInfo audioFile = future.get();
                if (audioFile != null) {
                    audioFiles.add(audioFile);
                }
            } catch (Exception e) {
                logger.error("Error collecting audio file result: {}", e.getMessage(), e);
            }
        }

        return audioFiles;
    }

    private AudioFileInfo generateSingleAudioFile(ListeningPassage passage) {
        try {
            // Combine passage text and questions for audio generation
            String audioText = passage.getPassageText() + "\n\n" + passage.getQuestions();
            
            // Create storage directory
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dailyStoragePath = Paths.get("/tmp/vocabulary-audio", dateFolder);
            Files.createDirectories(dailyStoragePath);
            
            String fileName = "toeic_passage_" + passage.getPassageNumber() + ".mp3";
            Path audioPath = dailyStoragePath.resolve(fileName);
            
            // Generate audio using AudioService's method
            boolean success = audioService.generateSingleAudio(audioText, audioPath.toString(), "passage", 120);
            
            if (success) {
                String audioUrl = "http://localhost:8282/audio/" + dateFolder + "/" + fileName;
                logger.info("Generated audio file for passage {}: {}", passage.getPassageNumber(), fileName);
                return new AudioFileInfo(passage.getPassageNumber(), audioPath.toString(), audioUrl, fileName);
            } else {
                logger.error("Failed to generate audio for passage {}", passage.getPassageNumber());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error generating audio for passage {}: {}", passage.getPassageNumber(), e.getMessage(), e);
            return null;
        }
    }

    private String createPassagesTextFile(List<ListeningPassage> passages) throws IOException {
        String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path dailyStoragePath = Paths.get("/tmp/vocabulary-audio", dateFolder);
        Files.createDirectories(dailyStoragePath);
        
        Path textFilePath = dailyStoragePath.resolve("toeic_listening_passages_" + dateFolder + ".txt");
        
        StringBuilder content = new StringBuilder();
        content.append("TOEIC Listening Passages - ").append(dateFolder).append("\n");
        content.append("=" .repeat(50)).append("\n\n");
        
        for (ListeningPassage passage : passages) {
            content.append("PASSAGE ").append(passage.getPassageNumber()).append("\n");
            content.append("-".repeat(20)).append("\n");
            content.append(passage.getFullContent()).append("\n\n");
        }
        
        Files.write(textFilePath, content.toString().getBytes());
        logger.info("Created passages text file: {}", textFilePath);
        
        return textFilePath.toString();
    }

    // Inner classes for data structures
    public static class ListeningPassage {
        private final int passageNumber;
        private final String passageText;
        private final String questions;
        private final String fullContent;

        public ListeningPassage(int passageNumber, String passageText, String questions, String fullContent) {
            this.passageNumber = passageNumber;
            this.passageText = passageText;
            this.questions = questions;
            this.fullContent = fullContent;
        }

        public int getPassageNumber() { return passageNumber; }
        public String getPassageText() { return passageText; }
        public String getQuestions() { return questions; }
        public String getFullContent() { return fullContent; }
    }

    public static class AudioFileInfo {
        private final int passageNumber;
        private final String filePath;
        private final String url;
        private final String fileName;

        public AudioFileInfo(int passageNumber, String filePath, String url, String fileName) {
            this.passageNumber = passageNumber;
            this.filePath = filePath;
            this.url = url;
            this.fileName = fileName;
        }

        public int getPassageNumber() { return passageNumber; }
        public String getFilePath() { return filePath; }
        public String getUrl() { return url; }
        public String getFileName() { return fileName; }
    }

    // Data class for Collocation
    public static class Collocation {
        private final String collocation;
        private final String ipa;
        private final String meaning;
        private final String example;
        private final String vietnamese;

        public Collocation(String collocation, String ipa, String meaning, String example, String vietnamese) {
            this.collocation = collocation;
            this.ipa = ipa;
            this.meaning = meaning;
            this.example = example;
            this.vietnamese = vietnamese;
        }

        public String getCollocation() { return collocation; }
        public String getIpa() { return ipa; }
        public String getMeaning() { return meaning; }
        public String getExample() { return example; }
        public String getVietnamese() { return vietnamese; }
    }
}
