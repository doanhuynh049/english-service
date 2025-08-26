package com.quat.englishService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quat.englishService.dto.GeminiRequest;
import com.quat.englishService.dto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    @Value("${app.llm-provider}")
    private String apiUrl;

    @Value("${app.llm-api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String getWordExplanation(String word) {
        try {
            String prompt = createPrompt(word);
            GeminiRequest request = new GeminiRequest(prompt);

            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                GeminiResponse geminiResponse = objectMapper.readValue(response.body(), GeminiResponse.class);
                return extractTextFromResponse(geminiResponse);
            } else {
                logger.error("Gemini API error for word '{}': Status {}, Body: {}",
                        word, response.statusCode(), response.body());
                return "Error retrieving explanation for: " + word;
            }

        } catch (Exception e) {
            logger.error("Exception calling Gemini API for word '{}': {}", word, e.getMessage(), e);
            return "Error retrieving explanation for: " + word;
        }
    }

    private String createPrompt(String word) {
        return String.format("""
                Provide a comprehensive explanation of the English word "%s" using EXACTLY this format:
                
                **IPA Pronunciation:** /pronunciation here/
                
                **Part of Speech:** (noun/verb/adjective/etc.)
                
                **Simple Definition:** Brief, clear definition
                
                **Advanced Definition:** More detailed, nuanced definition
                
                **Example Sentences:**
                
                1. First example sentence with the word in context.
                2. Second example sentence showing different usage.
                3. Third example sentence demonstrating another context.
                
                **Common Collocations and Fixed Expressions:**
                
                * **Expression 1:** Explanation of usage
                * **Expression 2:** Explanation of usage
                * **Expression 3:** Explanation of usage
                
                **Synonyms & Antonyms:**
                
                * **Synonyms:**
                    * **Synonym1:** Brief explanation of difference from main word
                    * **Synonym2:** Brief explanation of difference from main word
                    * **Synonym3:** Brief explanation of difference from main word
                * **Antonyms:**
                    * **Antonym1:** Brief explanation
                    * **Antonym2:** Brief explanation
                
                **Commonly Confused Words:**
                
                * **Word1:** Explain how this word differs from "%s" and when to use each
                * **Word2:** Explain how this word differs from "%s" and when to use each
                
                **Word Family:**
                
                * **Noun:** related noun forms
                * **Verb:** related verb forms  
                * **Adjective:** related adjective forms
                * **Adverb:** related adverb forms
                
                **Vietnamese Translation:**
                
                Primary translation and nuanced explanations of usage differences.
                
                Please follow this EXACT format for consistency and include ALL sections.
                """, word, word, word);
    }

    private String extractTextFromResponse(GeminiResponse response) {
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            GeminiResponse.Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return "No explanation available";
    }
}
