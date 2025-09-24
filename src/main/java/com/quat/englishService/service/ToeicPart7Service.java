package com.quat.englishService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ToeicPart7Service {

    private static final Logger logger = LoggerFactory.getLogger(ToeicPart7Service.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;

    private static final String PART7_PASSAGE_PROMPT = """
        Create a TOEIC Part 7 Reading Comprehension passage. The passage must:

        • Length: 300–400 words, realistic business/professional tone.  
        • Level: Advanced (targeting TOEIC 900+).  
        • Document Type: Randomly choose ONE from the following authentic TOEIC formats:  
          - Business correspondence (emails, memos, letters)  
          - Advertisements and promotional materials  
          - Articles and reports (news, business, research)  
          - Notices, announcements, and instructions  
          - Forms, applications, and questionnaires  
          - Text message chains or instant messaging conversations  

        • Language: Include 15–20 advanced vocabulary words, idiomatic expressions, and sophisticated sentence structures that would challenge high-level test takers.  
        • Content: Contain implicit information requiring inference, not just direct facts.  
        • Realism: Context must be plausible for professional or workplace settings.  

        At the end of the passage, create exactly 5 multiple-choice comprehension questions with 4 options each (A–D). Questions must test:  
          1. Main idea / overall purpose  
          2. Specific detail recall  
          3. Inference / implication  
          4. Vocabulary-in-context  
          5. Tone, organization, or author’s purpose  

        • Difficulty: Make questions progressively more challenging, mixing direct factual questions with inferential and critical-thinking ones.  
        • Answer Randomization: Distribute correct answers across A, B, C, D (not always the same letter).  

        Output format exactly as follows:

        Document Type:
        <Insert chosen document type here>

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

        4. <Question 4>
        A. <Option A>
        B. <Option B>
        C. <Option C>
        D. <Option D>

        5. <Question 5>
        A. <Option A>
        B. <Option B>
        C. <Option C>
        D. <Option D>

        Answer Key:
        1. <Correct answer>
        2. <Correct answer>
        3. <Correct answer>
        4. <Correct answer>
        5. <Correct answer>
        """;

    private static final String VOCABULARY_EXPLANATION_PROMPT = """
            From the following TOEIC Part 7 passage, identify and explain the most challenging vocabulary words and expressions that would be important for achieving a 900+ TOEIC score.

            Please provide:
            1. Extract 10-15 of the most advanced/challenging words and phrases from the passage.
            2. For each word/phrase, provide:
               - Word/Phrase
               - IPA Pronunciation
               - Part of Speech
               - Definition in context (clear and concise)
               - Example sentence (different from the passage)
               - Common collocations or related expressions
               - Synonyms and commonly confused words
               - Word family (noun/verb/adjective/adverb forms if applicable)
               - TOEIC frequency level (Common / Frequent / Advanced)
               - Notes: usage tips, nuances, or common mistakes
               - (Optional) Vietnamese Translation

            Format the response as a structured vocabulary study guide suitable for high-level TOEIC preparation.

            Passage to analyze:
            %s

            Output format:
            TOEIC Part 7 Vocabulary Study Guide
            =====================================

            Word/Phrase: [word]
            IPA: [pronunciation]
            Part of Speech: [noun/verb/adjective/etc.]
            Definition: [clear definition in context]
            Example: [new example sentence]
            Collocations: [related word combinations]
            Synonyms/Confused Words: [list]
            Word Family: [noun/verb/adj/adv forms]
            TOEIC Level: [Common/Frequent/Advanced]
            Notes: [usage tips, nuances, or common mistakes]
            Vietnamese: [translation]

            ---

            [Repeat for each vocabulary item]
            """;

    public ToeicPart7Service(GeminiClient geminiClient, EmailService emailService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
    }

    public void processDailyToeicPart7() {
        logger.info("Starting TOEIC Part 7 Reading processing...");

        try {
            // Step 1: Generate TOEIC Part 7 passage with questions
            logger.info("Generating TOEIC Part 7 passage...");
            String passageResponse = geminiClient.generateContent(PART7_PASSAGE_PROMPT);
            
            if (passageResponse == null || passageResponse.trim().isEmpty()) {
                throw new RuntimeException("Failed to generate TOEIC Part 7 passage");
            }

            logger.info("Successfully generated TOEIC Part 7 passage");

            // Step 2: Extract the passage text for vocabulary analysis
            String passageText = extractPassageText(passageResponse);
            
            // Step 3: Generate vocabulary explanation
            logger.info("Generating vocabulary explanation...");
            String vocabularyPrompt = String.format(VOCABULARY_EXPLANATION_PROMPT, passageText);
            String vocabularyResponse = geminiClient.generateContent(vocabularyPrompt);
            
            if (vocabularyResponse == null || vocabularyResponse.trim().isEmpty()) {
                throw new RuntimeException("Failed to generate vocabulary explanation");
            }

            logger.info("Successfully generated vocabulary explanation");

            // Step 4: Combine both responses
            String combinedContent = formatFinalContent(passageResponse, vocabularyResponse);

            // Step 5: Send email with the content
            String subject = String.format("TOEIC Part 7 Reading Practice - %s", 
                LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            
            emailService.sendToeicEmail(subject, combinedContent);
            logger.info("Successfully sent TOEIC Part 7 email");

            logger.info("TOEIC Part 7 Reading processing completed successfully");

        } catch (Exception e) {
            logger.error("Error during TOEIC Part 7 processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process daily TOEIC Part 7", e);
        }
    }

    private String extractPassageText(String fullResponse) {
        try {
            // Extract the passage content between "Passage:" and "Questions:"
            int passageStart = fullResponse.indexOf("Passage:");
            int questionsStart = fullResponse.indexOf("Questions:");
            
            if (passageStart == -1 || questionsStart == -1) {
                logger.warn("Could not find passage boundaries, using full response for vocabulary analysis");
                return fullResponse;
            }
            
            String passage = fullResponse.substring(passageStart + 8, questionsStart).trim();
            return passage;
        } catch (Exception e) {
            logger.warn("Error extracting passage text: {}", e.getMessage());
            return fullResponse;
        }
    }

    private String formatFinalContent(String passageContent, String vocabularyContent) {
        StringBuilder content = new StringBuilder();
        
        content.append("📚 TOEIC Part 7 Reading Practice - Advanced Level (Target: 900+)\n");
        content.append("=================================================================\n\n");
        
        content.append(passageContent);
        content.append("\n\n");
        content.append("📖 VOCABULARY LEARNING SECTION\n");
        content.append("==============================\n\n");
        content.append(vocabularyContent);
        
        content.append("\n\n");
        content.append("💡 Study Tips:\n");
        content.append("- Read the passage carefully, paying attention to context clues\n");
        content.append("- Practice the vocabulary in your own sentences\n");
        content.append("- Focus on understanding implicit meanings and inferences\n");
        content.append("- Time yourself: aim for 10-12 minutes for this type of passage\n");
        content.append("- Review answer explanations to understand reasoning\n\n");
        
        content.append("Generated on: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        
        return content.toString();
    }

    public String generateManualToeicPart7() {
        logger.info("Manual TOEIC Part 7 generation requested");
        
        try {
            // Generate passage
            String passageResponse = geminiClient.generateContent(PART7_PASSAGE_PROMPT);
            String passageText = extractPassageText(passageResponse);
            
            // Generate vocabulary explanation
            String vocabularyPrompt = String.format(VOCABULARY_EXPLANATION_PROMPT, passageText);
            String vocabularyResponse = geminiClient.generateContent(vocabularyPrompt);
            
            // Return combined content
            return formatFinalContent(passageResponse, vocabularyResponse);
            
        } catch (Exception e) {
            logger.error("Error during manual TOEIC Part 7 generation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate TOEIC Part 7 content", e);
        }
    }
}
