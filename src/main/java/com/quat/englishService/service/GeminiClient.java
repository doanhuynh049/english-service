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
                Teach me the word "%s" in detail. Include:
                
                - IPA pronunciation
                - Part of speech
                - English definition (simple + advanced if available)
                - 2–3 example sentences in natural English
                - Common collocations and fixed expressions with this word
                - Synonyms & antonyms (with slight differences explained)
                - Commonly confused words and how to distinguish them
                - Word family (e.g., noun, verb, adjective forms)
                - Vietnamese translation with nuance
                """, word);
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
