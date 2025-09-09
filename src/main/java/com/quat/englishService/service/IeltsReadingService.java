package com.quat.englishService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class IeltsReadingService {

    private static final Logger logger = LoggerFactory.getLogger(IeltsReadingService.class);

    private final GeminiClient geminiClient;
    private final EmailService emailService;

    private static final String PASSAGE_GENERATION_PROMPT = """
            Create an IELTS Academic Reading passage. The passage should:

            Be between 250–300 words.

            Cover a realistic topic suitable for IELTS Reading, randomly chosen from areas such as science, technology, environment, education, history, health, or culture.

            Include a clear structure with an introduction, main body, and conclusion.

            Contain a mix of complex and simple sentences, suitable for intermediate to advanced English learners.

            Naturally include at least 8–10 academic or topic-related vocabulary words that could appear in IELTS reading comprehension questions.

            Include implicit and explicit information suitable for testing comprehension, inference, and vocabulary.

            Avoid being conversational; maintain a formal academic tone.

            End with 5 multiple-choice comprehension questions with 4 options each, testing both factual information and inference.

            Ensure the topic is different each time the prompt is used, to create diverse passages for practice.

            Output Format clearly with:

            Topic:
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

    private static final String EXPLANATION_PROMPT_TEMPLATE = """
            Provide a detailed explanation for the following IELTS Academic Reading passage.
            
            The explanation should include:
            - Summarize the main idea and each paragraph in simple, clear English
            - Highlight the purpose of the passage and the author's perspective
            - Identify key academic collocations (word combinations) from the passage with definitions and examples
            - Identify implicit and explicit information in the passage
            - Clarify any inferences, assumptions, or relationships between ideas that may be tested in comprehension questions
            - Provide a brief guide on how to answer potential multiple-choice or matching questions based on this passage
            - Maintain formal, academic language suitable for IELTS learners

            Output Format clearly with section headers:

            Main Idea:
            <Brief summary of the main concept and purpose of the passage>

            Paragraph Summary:
            **Paragraph 1:** <Summary of first paragraph content>
            **Paragraph 2:** <Summary of second paragraph content>
            **Paragraph 3:** <Summary of third paragraph content if exists>

            Key Collocations:
            **collocation phrase:** clear definition and meaning in context
            *Example:* natural example sentence using the collocation
            *Academic usage:* how it's typically used in academic writing

            **another collocation:** definition and contextual meaning
            *Example:* example sentence showing proper usage
            *Academic usage:* academic context explanation

            Implicit vs Explicit Information:
            **Explicit Information:**
            • Directly stated fact or information from the passage
            • Another explicitly mentioned point
            • Clear factual statement from the text

            **Implicit Information:**
            • Implied meaning or inference that readers must deduce
            • Unstated assumption that underlies the argument
            • Relationship between ideas that must be inferred

            Question Strategy:
            **Strategy 1:** <Specific tip for answering comprehension questions>
            **Strategy 2:** <Method for identifying correct answers>
            **Strategy 3:** <Technique for avoiding common mistakes>

            Use **bold formatting** for important terms, concepts, and section headings.
            Focus on academic collocations that are commonly tested in IELTS reading.
            Keep explanations clear and suitable for intermediate to advanced English learners.

            Passage:
            %s
            """;

    public IeltsReadingService(GeminiClient geminiClient, EmailService emailService) {
        this.geminiClient = geminiClient;
        this.emailService = emailService;
    }

    public void processDailyIeltsReading() {
        logger.info("Starting IELTS Reading practice processing...");

        try {
            // Step 1: Generate IELTS reading passage with questions
            String passageResponse = geminiClient.generateContent(PASSAGE_GENERATION_PROMPT);
            logger.info("Generated IELTS reading passage");

            // Step 2: Parse the passage response
            IeltsPassage passage = parseIeltsPassage(passageResponse);
            logger.info("Parsed IELTS passage: {}", passage.getTopic());

            // Step 3: Generate detailed explanation
            String explanationPrompt = String.format(EXPLANATION_PROMPT_TEMPLATE, passage.getPassageText());
            String explanationResponse = geminiClient.generateContent(explanationPrompt);
            logger.info("Generated passage explanation");

            // Step 4: Parse the explanation
            IeltsExplanation explanation = parseIeltsExplanation(explanationResponse);
            logger.info("Parsed explanation content");

            // Step 5: Build HTML email content
            String htmlContent = buildIeltsEmailContent(passage, explanation);
            logger.info("Built HTML email content");

            // Step 6: Send email
            emailService.sendIeltsReadingEmail(htmlContent);
            logger.info("IELTS Reading email sent successfully");

        } catch (Exception e) {
            logger.error("Error during IELTS Reading processing: {}", e.getMessage(), e);
            throw new RuntimeException("IELTS Reading processing failed", e);
        }
    }

    private IeltsPassage parseIeltsPassage(String response) {
        String topic = "";
        String passageText = "";
        String questions = "";

        try {
            // Find sections
            int topicStart = response.toLowerCase().indexOf("topic:");
            int passageStart = response.toLowerCase().indexOf("passage:");
            int questionsStart = response.toLowerCase().indexOf("questions:");
            int answerKeyStart = response.toLowerCase().indexOf("answer key:");

            // Extract topic
            if (topicStart != -1 && passageStart != -1) {
                topic = response.substring(topicStart, passageStart).trim();
                topic = topic.replaceAll("(?i)^.*?topic\\s*:", "").trim();
            }

            // Extract passage
            if (passageStart != -1 && questionsStart != -1) {
                passageText = response.substring(passageStart, questionsStart).trim();
                passageText = passageText.replaceAll("(?i)^.*?passage\\s*:", "").trim();
            }

            // Extract questions and answers
            if (questionsStart != -1) {
                if (answerKeyStart != -1) {
                    String questionsOnly = response.substring(questionsStart, answerKeyStart).trim();
                    questionsOnly = questionsOnly.replaceAll("(?i)^.*?questions\\s*:", "").trim();

                    String answerKeySection = response.substring(answerKeyStart).trim();
                    answerKeySection = answerKeySection.replaceAll("(?i)^.*?answer key\\s*:", "").trim();

                    questions = questionsOnly + "\n\nAnswer Key:\n" + answerKeySection;
                } else {
                    questions = response.substring(questionsStart).trim();
                    questions = questions.replaceAll("(?i)^.*?questions\\s*:", "").trim();
                }
            }

            logger.debug("Parsed IELTS passage - Topic: {}, Passage length: {}, Questions length: {}",
                        topic, passageText.length(), questions.length());

        } catch (Exception e) {
            logger.error("Error parsing IELTS passage: {}", e.getMessage(), e);
            // Fallback
            topic = "Academic Reading Practice";
            passageText = response.length() > 500 ? response.substring(0, 500) + "..." : response;
            questions = "Questions could not be parsed.";
        }

        return new IeltsPassage(topic, passageText, questions, response);
    }

    private IeltsExplanation parseIeltsExplanation(String response) {
        String mainIdea = "";
        String paragraphSummary = "";
        String keyVocabulary = "";
        String implicitExplicit = "";
        String questionStrategy = "";

        try {
            // Extract sections using case-insensitive matching
            String[] sections = response.split("(?i)(Main Idea:|Paragraph Summary:|Key Collocations:|Implicit vs Explicit Information:|Question Strategy:)");
            
            if (sections.length >= 6) {
                mainIdea = sections[2].trim();
                paragraphSummary = sections[3].trim();
                keyVocabulary = sections[4].trim();
                implicitExplicit = sections[5].trim();
                if (sections.length > 6) {
                    questionStrategy = sections[6].trim();
                }
            } else {
                // Fallback: use the entire response
                mainIdea = "Please refer to the full explanation below.";
                paragraphSummary = response;
            }

        } catch (Exception e) {
            logger.error("Error parsing IELTS explanation: {}", e.getMessage(), e);
            mainIdea = "Explanation parsing failed.";
            paragraphSummary = response;
        }

        return new IeltsExplanation(mainIdea, paragraphSummary, keyVocabulary, implicitExplicit, questionStrategy);
    }

    private String buildIeltsEmailContent(IeltsPassage passage, IeltsExplanation explanation) {
        try {
            // Load the IELTS HTML template
            String template = loadIeltsEmailTemplate();
            
            // Build the main content sections
            StringBuilder content = new StringBuilder();
            
            // Topic section
            content.append("<div class=\"section\">\n");
            content.append("  <div class=\"topic-badge\">").append(passage.getTopic()).append("</div>\n");
            content.append("</div>\n");
            
            // Passage section
            content.append("<div class=\"section\">\n");
            content.append("  <h2 class=\"section-title\">\n");
            content.append("    <span class=\"section-icon\">📄</span>\n");
            content.append("    Reading Passage\n");
            content.append("  </h2>\n");
            content.append("  <div class=\"passage-content\">\n");
            content.append("    ").append(formatPassageText(passage.getPassageText())).append("\n");
            content.append("  </div>\n");
            content.append("</div>\n");
            
            // Questions section
            content.append("<div class=\"section\">\n");
            content.append("  <h2 class=\"section-title\">\n");
            content.append("    <span class=\"section-icon\">❓</span>\n");
            content.append("    Comprehension Questions\n");
            content.append("  </h2>\n");
            content.append("  <div class=\"questions-content\">\n");
            content.append("    ").append(formatQuestions(passage.getQuestions())).append("\n");
            content.append("  </div>\n");
            content.append("</div>\n");
            
            // Explanation sections
            if (!explanation.getMainIdea().isEmpty()) {
                content.append(buildExplanationSection("🎯", "Main Idea", explanation.getMainIdea()));
            }
            
            if (!explanation.getParagraphSummary().isEmpty()) {
                content.append(buildExplanationSection("📋", "Paragraph Summary", explanation.getParagraphSummary()));
            }
            
            if (!explanation.getKeyVocabulary().isEmpty()) {
                content.append(buildExplanationSection("📚", "Key Collocations", explanation.getKeyVocabulary()));
            }
            
            if (!explanation.getImplicitExplicit().isEmpty()) {
                content.append(buildExplanationSection("🔍", "Implicit vs Explicit Information", explanation.getImplicitExplicit()));
            }
            
            if (!explanation.getQuestionStrategy().isEmpty()) {
                content.append(buildExplanationSection("💡", "Question Strategy", explanation.getQuestionStrategy()));
            }
            
            // Replace placeholders in template
            String emailContent = template
                .replace("{{DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")))
                .replace("{{CONTENT}}", content.toString())
                .replace("{{GENERATION_DATE}}", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                
            return emailContent;
            
        } catch (Exception e) {
            logger.error("Failed to build IELTS email content using template, falling back to simple format", e);
            return buildSimpleIeltsContent(passage, explanation);
        }
    }
    
    private String loadIeltsEmailTemplate() throws Exception {
        try (var inputStream = getClass().getResourceAsStream("/ielts-email-template.html")) {
            if (inputStream == null) {
                throw new RuntimeException("IELTS email template not found");
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    private String formatPassageText(String passageText) {
        if (passageText == null || passageText.trim().isEmpty()) {
            return "";
        }
        
        // Split into paragraphs and format them
        String[] paragraphs = passageText.split("\\n\\s*\\n");
        StringBuilder formatted = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                formatted.append("<p>").append(paragraph.trim()).append("</p>\n");
            }
        }
        
        return formatted.toString();
    }
    
    private String formatQuestions(String questions) {
        if (questions == null || questions.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = questions.split("\\n");
        String currentQuestion = "";
        boolean inAnswerKey = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.toLowerCase().contains("answer key")) {
                inAnswerKey = true;
                if (!currentQuestion.isEmpty()) {
                    formatted.append("</div>\n");
                    currentQuestion = "";
                }
                formatted.append("<div class=\"answer-key\">\n");
                formatted.append("  <div class=\"answer-key-title\">Answer Key</div>\n");
                formatted.append("  <div class=\"answers\">\n");
                continue;
            }
            
            if (inAnswerKey) {
                if (line.matches("\\d+\\.\\s*[A-D].*")) {
                    String[] parts = line.split("\\.", 2);
                    if (parts.length == 2) {
                        formatted.append("    <div class=\"answer-item\">")
                                .append(parts[0].trim()).append(". ")
                                .append(parts[1].trim()).append("</div>\n");
                    }
                }
            } else {
                if (line.matches("\\d+\\..*")) {
                    // New question
                    if (!currentQuestion.isEmpty()) {
                        formatted.append("</div>\n");
                    }
                    formatted.append("<div class=\"question-item\">\n");
                    formatted.append("  <div class=\"question-text\">").append(line).append("</div>\n");
                    formatted.append("  <div class=\"question-options\">\n");
                    currentQuestion = line;
                } else if (line.matches("[A-D]\\..*")) {
                    // Option
                    formatted.append("    <div class=\"option\">").append(line).append("</div>\n");
                }
            }
        }
        
        if (inAnswerKey) {
            formatted.append("  </div>\n");
            formatted.append("</div>\n");
        } else if (!currentQuestion.isEmpty()) {
            formatted.append("  </div>\n");
            formatted.append("</div>\n");
        }
        
        return formatted.toString();
    }
    
    private String buildExplanationSection(String icon, String title, String content) {
        StringBuilder section = new StringBuilder();
        section.append("<div class=\"section\">\n");
        section.append("  <div class=\"explanation-section\">\n");
        section.append("    <div class=\"explanation-title\">\n");
        section.append("      <span>").append(icon).append("</span>\n");
        section.append("      ").append(title).append("\n");
        section.append("    </div>\n");
        section.append("    <div class=\"explanation-content\">\n");
        
        // Format content with bold text and proper structure
        String formattedContent = formatExplanationContent(content);
        section.append(formattedContent);
        
        section.append("    </div>\n");
        section.append("  </div>\n");
        section.append("</div>\n");
        
        return section.toString();
    }
    
    private String formatExplanationContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] paragraphs = content.split("\\n\\s*\\n");
        
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                // Process bold formatting and bullet points
                String processedParagraph = processBoldTextAndBullets(paragraph.trim());
                formatted.append("      <p>").append(processedParagraph).append("</p>\n");
            }
        }
        
        return formatted.toString();
    }
    
    private String processBoldTextAndBullets(String text) {
        // Convert **text** to <strong>text</strong>
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        
        // Handle collocations formatting - look for specific patterns
        if (text.contains("*Example:*") || text.contains("*Academic usage:*")) {
            return processCollocationFormat(text);
        }
        
        // Convert bullet points (•) to HTML list items
        if (text.contains("•")) {
            String[] lines = text.split("\\n");
            StringBuilder result = new StringBuilder();
            boolean inList = false;
            
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("•")) {
                    if (!inList) {
                        result.append("<ul>\n");
                        inList = true;
                    }
                    result.append("        <li>").append(line.substring(1).trim()).append("</li>\n");
                } else {
                    if (inList) {
                        result.append("      </ul>\n");
                        inList = false;
                    }
                    if (!line.isEmpty()) {
                        result.append(line).append(" ");
                    }
                }
            }
            
            if (inList) {
                result.append("      </ul>\n");
            }
            
            return result.toString().trim();
        }
        
        return text;
    }
    
    private String processCollocationFormat(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check if this is a collocation definition line (starts with **)
            if (line.startsWith("<strong>") && line.contains(":</strong>")) {
                // This is a collocation term and definition
                result.append("<div class=\"collocation-item\">\n");
                
                // Extract term and definition
                int colonIndex = line.indexOf(":</strong>");
                if (colonIndex != -1) {
                    String term = line.substring(8, colonIndex); // Remove <strong>
                    String definition = line.substring(colonIndex + 10).trim(); // Remove :</strong>
                    
                    result.append("  <div class=\"collocation-term\">").append(term).append("</div>\n");
                    result.append("  <div class=\"collocation-definition\">").append(definition).append("</div>\n");
                }
            } else if (line.startsWith("*Example:*")) {
                // Example sentence
                String example = line.replace("*Example:*", "").trim();
                result.append("  <div class=\"collocation-example\"><strong>Example:</strong> ").append(example).append("</div>\n");
            } else if (line.startsWith("*Academic usage:*")) {
                // Academic usage explanation
                String usage = line.replace("*Academic usage:*", "").trim();
                result.append("  <div class=\"collocation-usage\"><strong>Academic usage:</strong> ").append(usage).append("</div>\n");
                result.append("</div>\n"); // Close collocation-item
            } else {
                // Regular text
                if (!line.startsWith("<div class=\"collocation-item\">") && !line.startsWith("</div>")) {
                    result.append(line).append(" ");
                }
            }
        }
        
        return result.toString().trim();
    }
    
    private String buildSimpleIeltsContent(IeltsPassage passage, IeltsExplanation explanation) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        html.append("<h1 style='color: #3182ce;'>IELTS Reading Practice</h1>");
        html.append("<h2>Topic: ").append(passage.getTopic()).append("</h2>");
        html.append("<h3>Passage:</h3>");
        html.append("<p style='line-height: 1.6;'>").append(passage.getPassageText().replace("\n", "<br>")).append("</p>");
        html.append("<h3>Questions:</h3>");
        html.append("<pre style='white-space: pre-wrap;'>").append(passage.getQuestions()).append("</pre>");
        html.append("<h3>Explanation:</h3>");
        html.append("<p>").append(explanation.getMainIdea().replace("\n", "<br>")).append("</p>");
        html.append("</div>");
        return html.toString();
    }

    // Inner classes for data structures
    public static class IeltsPassage {
        private final String topic;
        private final String passageText;
        private final String questions;
        private final String fullResponse;

        public IeltsPassage(String topic, String passageText, String questions, String fullResponse) {
            this.topic = topic;
            this.passageText = passageText;
            this.questions = questions;
            this.fullResponse = fullResponse;
        }

        public String getTopic() { return topic; }
        public String getPassageText() { return passageText; }
        public String getQuestions() { return questions; }
        public String getFullResponse() { return fullResponse; }
    }

    public static class IeltsExplanation {
        private final String mainIdea;
        private final String paragraphSummary;
        private final String keyVocabulary;
        private final String implicitExplicit;
        private final String questionStrategy;

        public IeltsExplanation(String mainIdea, String paragraphSummary, String keyVocabulary, 
                               String implicitExplicit, String questionStrategy) {
            this.mainIdea = mainIdea;
            this.paragraphSummary = paragraphSummary;
            this.keyVocabulary = keyVocabulary;
            this.implicitExplicit = implicitExplicit;
            this.questionStrategy = questionStrategy;
        }

        public String getMainIdea() { return mainIdea; }
        public String getParagraphSummary() { return paragraphSummary; }
        public String getKeyVocabulary() { return keyVocabulary; }
        public String getImplicitExplicit() { return implicitExplicit; }
        public String getQuestionStrategy() { return questionStrategy; }
    }
}
