package com.quat.englishService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

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
            Answer should be mixed in terms of difficulty, with some questions focusing on direct information and others requiring inference or understanding of vocabulary in context and random order.
            Provide random answer A, B, C, D for each question.
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

            1. **Main Idea & Purpose:** Summarize the central theme and purpose of the passage in clear, concise English.

            2. **Paragraph Summaries:** Provide a brief summary of each paragraph, capturing the key points and supporting details.

            3. **Academic Collocations & Vocabulary:**  
            - Identify 8–10 key academic collocations (word combinations) and important topic-specific vocabulary from the passage.  
            - For each collocation/vocabulary word, include:  
                - **Definition & Meaning in Context:** Explain how it is used in the passage.  
                - **Vietnamese Meaning:** Provide the Vietnamese translation and meaning.  
                - **Example Sentence:** Provide a natural sentence using the word/collocation.  
                - **Academic Usage:** Explain how it is typically used in academic or formal writing.  
            - Highlight bold words or phrases as they appear in the passage.

            4. **Implicit vs Explicit Information:**  
            - **Explicit Information:** Directly stated facts or details from the text.  
            - **Implicit Information:** Implied meanings, assumptions, or relationships that the reader must infer.

            5. **Question Strategy:** Provide strategies for answering IELTS reading comprehension questions, such as:  
            - Techniques for identifying explicit and implicit information.  
            - How to recognize paraphrasing or distractor options.  
            - Tips for efficiently tackling multiple-choice and matching questions.

            **Output Format with Clear Section Headers:**

            Main Idea & Purpose:
            <Concise summary of the main concept and author's intention>

            Paragraph Summaries:
            **Paragraph 1:** <Summary>  
            **Paragraph 2:** <Summary>  
            **Paragraph 3:** <Summary if applicable>  

            Key Collocations & Vocabulary:
            **collocation or word:** definition & meaning in context  
            *Vietnamese Meaning:* Vietnamese translation and meaning  
            *Example:* natural sentence using it  
            *Academic usage:* typical academic use  

            Implicit vs Explicit Information:
            **Explicit Information:**  
            • Directly stated fact 1  
            • Fact 2  
            • Fact 3  

            **Implicit Information:**  
            • Implied meaning or inference 1  
            • Inference 2  
            • Relationships between ideas

            Question Strategy:
            **Strategy 1:** <Tip>  
            **Strategy 2:** <Tip>  
            **Strategy 3:** <Tip>

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
            // Use more robust section extraction method
            mainIdea = extractSection(response, "Main Idea & Purpose:", new String[]{"Paragraph Summaries:"});
            paragraphSummary = extractSection(response, "Paragraph Summaries:", new String[]{"Key Collocations & Vocabulary:"});
            keyVocabulary = extractSection(response, "Key Collocations & Vocabulary:", new String[]{"Implicit vs Explicit Information:"});
            implicitExplicit = extractSection(response, "Implicit vs Explicit Information:", new String[]{"Question Strategy:"});
            questionStrategy = extractSection(response, "Question Strategy:", new String[]{});

            // Fallback logic if sections not found properly
            if (mainIdea.isEmpty() && paragraphSummary.isEmpty() && keyVocabulary.isEmpty()) {
                logger.warn("Section parsing failed, using fallback method");
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

    private String extractSection(String response, String sectionHeader, String[] nextSectionHeaders) {
        int sectionStart = findSectionStart(response, sectionHeader);
        if (sectionStart == -1) {
            return "";
        }

        // Find the start of the next section
        int sectionEnd = response.length();
        for (String nextHeader : nextSectionHeaders) {
            int nextStart = findSectionStart(response, nextHeader);
            if (nextStart != -1 && nextStart > sectionStart) {
                sectionEnd = Math.min(sectionEnd, nextStart);
            }
        }

        // Extract content and clean it
        String content = response.substring(sectionStart, sectionEnd);
        // Remove the section header
        content = content.replaceAll("(?i)^.*?" + Pattern.quote(sectionHeader) + "\\s*", "").trim();
        
        return content;
    }

    private int findSectionStart(String text, String sectionHeader) {
        // Try case-insensitive search for the section header
        int index = text.toLowerCase().indexOf(sectionHeader.toLowerCase());
        return index;
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
            
            // Parse questions and answer key separately
            String[] questionParts = formatQuestionsAndAnswers(passage.getQuestions());
            content.append("    ").append(questionParts[0]).append("\n"); // Questions only
            content.append("  </div>\n");
            content.append("</div>\n");
            
            // Answer Key section (if available)
            if (questionParts.length > 1 && !questionParts[1].trim().isEmpty()) {
                content.append("<div class=\"section\">\n");
                content.append("  <h2 class=\"section-title\">\n");
                content.append("    <span class=\"section-icon\">🔑</span>\n");
                content.append("    Answer Key\n");
                content.append("  </h2>\n");
                content.append("  <div class=\"answer-key-section\">\n");
                content.append("    ").append(questionParts[1]).append("\n"); // Answer key only
                content.append("  </div>\n");
                content.append("</div>\n");
            }
            
            // Explanation sections
            if (!explanation.getMainIdea().isEmpty()) {
                content.append(buildExplanationSection("🎯", "Main Idea & Purpose", explanation.getMainIdea()));
            }
            
            if (!explanation.getParagraphSummary().isEmpty()) {
                content.append(buildExplanationSection("📋", "Paragraph Summaries", explanation.getParagraphSummary()));
            }
            
            if (!explanation.getKeyVocabulary().isEmpty()) {
                content.append(buildExplanationSection("📚", "Key Collocations & Vocabulary", explanation.getKeyVocabulary()));
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
                // Format single asterisk words (*word*) as bold for passage text
                String formattedParagraph = paragraph.trim().replaceAll("\\*(.*?)\\*", "<strong>$1</strong>");
                formatted.append("<p>").append(formattedParagraph).append("</p>\n");
            }
        }
        
        return formatted.toString();
    }
    
    private String[] formatQuestionsAndAnswers(String questions) {
        if (questions == null || questions.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        
        StringBuilder formattedQuestions = new StringBuilder();
        StringBuilder formattedAnswers = new StringBuilder();
        String[] lines = questions.split("\\n");
        String currentQuestion = "";
        boolean inAnswerKey = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.toLowerCase().contains("answer key")) {
                inAnswerKey = true;
                if (!currentQuestion.isEmpty()) {
                    formattedQuestions.append("    </div>\n");
                    formattedQuestions.append("  </div>\n");
                    currentQuestion = "";
                }
                continue;
            }
            
            if (inAnswerKey) {
                if (line.matches("\\d+\\.\\s*[A-D].*")) {
                    String[] parts = line.split("\\.", 2);
                    if (parts.length == 2) {
                        formattedAnswers.append("    <div class=\"answer-item\">")
                                .append("<strong>").append(parts[0].trim()).append(".</strong> ")
                                .append(parts[1].trim()).append("</div>\n");
                    }
                }
            } else {
                if (line.matches("\\d+\\..*")) {
                    // New question
                    if (!currentQuestion.isEmpty()) {
                        formattedQuestions.append("    </div>\n");
                        formattedQuestions.append("  </div>\n");
                    }
                    formattedQuestions.append("  <div class=\"question-item\">\n");
                    formattedQuestions.append("    <div class=\"question-text\">").append(line).append("</div>\n");
                    formattedQuestions.append("    <div class=\"question-options\">\n");
                    currentQuestion = line;
                } else if (line.matches("[A-D]\\..*")) {
                    // Option
                    formattedQuestions.append("      <div class=\"option\">").append(line).append("</div>\n");
                }
            }
        }
        
        if (!currentQuestion.isEmpty()) {
            formattedQuestions.append("    </div>\n");
            formattedQuestions.append("  </div>\n");
        }
        
        return new String[]{formattedQuestions.toString(), formattedAnswers.toString()};
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
        
        // Handle special sections differently based on their type
        if (content.contains("**Paragraph") && content.contains(":**")) {
            // This is paragraph summaries - handle them specially
            return formatParagraphSummaries(content);
        } else if (content.matches(".*\\d+\\.\\s+\\*\\*.*\\*\\*:.*")) {
            // This is collocations with numbered format
            return formatCollocations(content);
        } else if (content.contains("**Explicit Information:**") || content.contains("**Implicit Information:**")) {
            // This is implicit vs explicit section
            return formatImplicitExplicit(content);
        } else if (content.contains("**Strategy")) {
            // This is question strategy section
            return formatQuestionStrategies(content);
        } else {
            // Default formatting for main idea and other sections
            StringBuilder formatted = new StringBuilder();
            String[] paragraphs = content.split("\\n\\s*\\n");
            
            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    // Remove markdown headers from paragraph
                    String cleanParagraph = paragraph.replaceAll("^#+\\s*", "").trim();
                    if (!cleanParagraph.isEmpty()) {
                        String processedParagraph = processBoldTextAndBullets(cleanParagraph);
                        formatted.append("      <p>").append(processedParagraph).append("</p>\n");
                    }
                }
            }
            
            return formatted.toString();
        }
    }
    
    private String processBoldTextAndBullets(String text) {
        // Remove markdown headers (###, ##, etc.)
        text = text.replaceAll("^#+\\s*", "");
        
        // Convert **text** to <strong>text</strong> (double asterisk)
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        
        // Convert *text* to <strong>text</strong> (single asterisk) - but be more careful about context
        // Only apply to lines that look like formatting, not content with asterisks
        text = text.replaceAll("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)", "<strong>$1</strong>");
        
        // Handle collocations formatting - look for specific patterns
        if (text.contains("Example:") || text.contains("Academic usage:")) {
            return processCollocationFormat(text);
        }
        
        // Handle strategy formatting
        if (text.contains("Strategy") && text.contains(":")) {
            return processStrategyFormat(text);
        }
        
        // Handle paragraph summaries with special formatting
        if (text.contains("Paragraph") && text.contains(":")) {
            return processParagraphSummaryFormat(text);
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
    
    private String processStrategyFormat(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.contains("**Strategy") && line.contains(":**")) {
                // Extract strategy number and content
                int colonIndex = line.indexOf(":**");
                if (colonIndex != -1) {
                    String strategyHeader = line.substring(0, colonIndex + 3);
                    String strategyContent = line.substring(colonIndex + 3).trim();
                    
                    result.append("<div class=\"strategy-item\">\n");
                    result.append("  <div class=\"strategy-number\">").append(strategyHeader).append("</div>\n");
                    result.append("  <div>").append(strategyContent).append("</div>\n");
                    result.append("</div>\n");
                }
            } else {
                result.append(line).append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    private String processParagraphSummaryFormat(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.contains("**Paragraph") && line.contains(":**")) {
                // This is a paragraph header with summary
                result.append("<div class=\"paragraph-summary\">\n");
                result.append("  ").append(line).append("\n");
                result.append("</div>\n");
            } else {
                result.append(line).append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    private String processCollocationFormat(String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\\n");
        boolean inCollocationItem = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check for numbered collocation items (e.g., "1. **sustainable urban infrastructure:**")
            if (line.matches("\\d+\\.\\s+\\*\\*.*?\\*\\*:.*")) {
                if (inCollocationItem) {
                    result.append("</div>\n"); // Close previous collocation item
                }
                
                // Extract number, term, and definition
                String[] parts = line.split("\\*\\*", 3);
                if (parts.length >= 3) {
                    String number = parts[0].trim();
                    String term = parts[1].trim();
                    String definition = parts[2].replace(":", "").trim();
                    
                    result.append("<div class=\"collocation-item\">\n");
                    result.append("  <div class=\"collocation-term\">").append(number).append(" ").append(term).append("</div>\n");
                    result.append("  <div class=\"collocation-definition\">").append(definition).append("</div>\n");
                    inCollocationItem = true;
                }
            } else if (line.contains("*Definition & Meaning in Context:*")) {
                // Definition line
                String definition = line.replace("*Definition & Meaning in Context:*", "").trim();
                if (!definition.isEmpty()) {
                    result.append("  <div class=\"collocation-definition\">").append(definition).append("</div>\n");
                }
            } else if (line.contains("*Vietnamese Meaning:*")) {
                // Vietnamese meaning line
                String vietnamese = line.replace("*Vietnamese Meaning:*", "").trim();
                if (!vietnamese.isEmpty()) {
                    result.append("  <div class=\"collocation-vietnamese\"><strong>Vietnamese:</strong> ").append(vietnamese).append("</div>\n");
                }
            } else if (line.contains("*Example:*")) {
                // Example sentence
                String example = line.replace("*Example:*", "").trim();
                result.append("  <div class=\"collocation-example\"><strong>Example:</strong> ").append(example).append("</div>\n");
            } else if (line.contains("*Academic usage:*")) {
                // Academic usage explanation
                String usage = line.replace("*Academic usage:*", "").trim();
                result.append("  <div class=\"collocation-usage\"><strong>Academic usage:</strong> ").append(usage).append("</div>\n");
            } else if (!line.startsWith("<div") && !line.equals("</div>")) {
                // Regular content line
                result.append("  <div>").append(line).append("</div>\n");
            }
        }
        
        if (inCollocationItem) {
            result.append("</div>\n"); // Close last collocation item
        }
        
        return result.toString().trim();
    }
    
    private String formatParagraphSummaries(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Remove markdown headers
            if (line.matches("^#+\\s*.*")) {
                continue; // Skip markdown headers
            }
            
            if (line.contains("Paragraph") && line.contains(":")) {
                // Convert **text** to <strong>text</strong> and handle paragraph headers
                line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                result.append("      <div class=\"paragraph-summary\">").append(line).append("</div>\n");
            } else {
                // Regular content
                line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                if (!line.isEmpty()) {
                    result.append("      <p>").append(line).append("</p>\n");
                }
            }
        }
        
        return result.toString();
    }
    
    private String formatCollocations(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\n");
        boolean inCollocationItem = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Remove markdown headers
            if (line.matches("^#+\\s*.*")) {
                continue; // Skip markdown headers
            }
            
            // Check for numbered collocation items (e.g., "1. captivating natural phenomenon:")
            if (line.matches("\\d+\\.\\s+.*?:.*")) {
                if (inCollocationItem) {
                    result.append("      </div>\n"); // Close previous collocation item
                }
                
                // Extract number, term, and definition
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String termPart = line.substring(0, colonIndex).trim();
                    String definition = line.substring(colonIndex + 1).trim();
                    
                    // Remove number and format term
                    String term = termPart.replaceFirst("^\\d+\\.\\s*", "");
                    // Apply bold formatting
                    term = term.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                    
                    result.append("      <div class=\"collocation-item\">\n");
                    result.append("        <div class=\"collocation-term\">").append(term).append("</div>\n");
                    if (!definition.isEmpty()) {
                        result.append("        <div class=\"collocation-definition\">").append(definition).append("</div>\n");
                    }
                    inCollocationItem = true;
                }
            } else if (line.contains("Definition & Meaning in Context:") || line.contains("*Definition & Meaning in Context:*")) {
                // Definition line
                String definition = line.replaceAll("\\*?Definition & Meaning in Context:\\*?", "").trim();
                if (!definition.isEmpty() && inCollocationItem) {
                    result.append("        <div class=\"collocation-definition\">").append(definition).append("</div>\n");
                }
            } else if (line.contains("Vietnamese Meaning:") || line.contains("*Vietnamese Meaning:*")) {
                // Vietnamese meaning line
                String vietnamese = line.replaceAll("\\*?Vietnamese Meaning:\\*?", "").trim();
                if (!vietnamese.isEmpty() && inCollocationItem) {
                    result.append("        <div class=\"collocation-vietnamese\"><strong>Vietnamese:</strong> ").append(vietnamese).append("</div>\n");
                }
            } else if (line.contains("Example:") || line.contains("*Example:*")) {
                // Example sentence
                String example = line.replaceAll("\\*?Example:\\*?", "").trim();
                if (!example.isEmpty() && inCollocationItem) {
                    result.append("        <div class=\"collocation-example\"><strong>Example:</strong> ").append(example).append("</div>\n");
                }
            } else if (line.contains("Academic usage:") || line.contains("*Academic usage:*")) {
                // Academic usage explanation
                String usage = line.replaceAll("\\*?Academic usage:\\*?", "").trim();
                if (!usage.isEmpty() && inCollocationItem) {
                    result.append("        <div class=\"collocation-usage\"><strong>Academic usage:</strong> ").append(usage).append("</div>\n");
                }
            } else if (!line.startsWith("<div") && !line.equals("</div>")) {
                // Regular content line - could be continuation of definition
                if (inCollocationItem && !line.matches("\\d+\\.\\s+.*")) {
                    // Apply formatting and add as continuation
                    line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                    result.append("        <div class=\"collocation-definition\">").append(line).append("</div>\n");
                } else if (!inCollocationItem) {
                    line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                    result.append("      <p>").append(line).append("</p>\n");
                }
            }
        }
        
        if (inCollocationItem) {
            result.append("      </div>\n"); // Close last collocation item
        }
        
        return result.toString();
    }
    
    private String formatImplicitExplicit(String content) {
        StringBuilder result = new StringBuilder();
        
        // Split content by section headers, handling various formats
        String[] lines = content.split("\\n");
        boolean inExplicitSection = false;
        boolean inImplicitSection = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Check for section headers (with or without asterisks)
            if (line.toLowerCase().contains("explicit information")) {
                if (inImplicitSection) {
                    result.append("        </ul>\n");
                    result.append("      </div>\n");
                    inImplicitSection = false;
                }
                
                result.append("      <div class=\"info-section\">\n");
                result.append("        <h4 class=\"info-title\">Explicit Information</h4>\n");
                result.append("        <ul class=\"info-list\">\n");
                inExplicitSection = true;
                continue;
            } else if (line.toLowerCase().contains("implicit information")) {
                if (inExplicitSection) {
                    result.append("        </ul>\n");
                    result.append("      </div>\n");
                    inExplicitSection = false;
                }
                
                result.append("      <div class=\"info-section\">\n");
                result.append("        <h4 class=\"info-title\">Implicit Information</h4>\n");
                result.append("        <ul class=\"info-list\">\n");
                inImplicitSection = true;
                continue;
            }
            
            // Process content lines
            if (inExplicitSection || inImplicitSection) {
                // Format bold text **text** to <strong>text</strong>
                String formattedLine = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
                
                // Handle bullet points or list items
                if (line.startsWith("•") || line.startsWith("*") || line.startsWith("-")) {
                    // Remove bullet and add as list item
                    String contentText = formattedLine.replaceFirst("^[•*-]\\s*", "");
                    result.append("          <li>").append(contentText).append("</li>\n");
                } else {
                    // Regular content line
                    result.append("          <li>").append(formattedLine).append("</li>\n");
                }
            }
        }
        
        // Close any open section
        if (inExplicitSection || inImplicitSection) {
            result.append("        </ul>\n");
            result.append("      </div>\n");
        }
        
        return result.toString();
    }
    
    private String formatQuestionStrategies(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\\n");
        boolean inStrategy = false;
        boolean inBulletList = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Remove markdown headers
            if (line.matches("^#+\\s*.*")) {
                continue; // Skip markdown headers
            }
            
            // Apply bold formatting to the line first
            line = line.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
            
            // Check for strategy headers (e.g., "Strategy 1:" or "**Strategy 1:**")
            if (line.toLowerCase().matches(".*strategy\\s+\\d+.*:.*")) {
                // Close previous strategy or list
                if (inBulletList) {
                    result.append("        </ul>\n");
                    inBulletList = false;
                }
                if (inStrategy) {
                    result.append("      </div>\n");
                }
                
                // Extract strategy header and content
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String strategyHeader = line.substring(0, colonIndex + 1);
                    String strategyContent = line.substring(colonIndex + 1).trim();
                    
                    result.append("      <div class=\"strategy-item\">\n");
                    result.append("        <div class=\"strategy-header\">").append(strategyHeader).append("</div>\n");
                    if (!strategyContent.isEmpty()) {
                        result.append("        <div class=\"strategy-content\">").append(strategyContent).append("</div>\n");
                    }
                    inStrategy = true;
                }
            } else if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                // Bullet point with bullet symbol
                if (!inBulletList && inStrategy) {
                    result.append("        <ul class=\"strategy-list\">\n");
                    inBulletList = true;
                }
                if (inBulletList) {
                    String bulletContent = line.replaceFirst("^[•*-]\\s*", "");
                    result.append("          <li>").append(bulletContent).append("</li>\n");
                } else {
                    result.append("      <p>").append(line).append("</p>\n");
                }
            } else if (!line.startsWith("<div") && !line.equals("</div>")) {
                // Regular content
                if (inBulletList) {
                    result.append("        </ul>\n");
                    inBulletList = false;
                }
                if (inStrategy) {
                    result.append("        <div class=\"strategy-content\">").append(line).append("</div>\n");
                } else {
                    result.append("      <p>").append(line).append("</p>\n");
                }
            }
        }
        
        // Close any open lists or strategies
        if (inBulletList) {
            result.append("        </ul>\n");
        }
        if (inStrategy) {
            result.append("      </div>\n");
        }
        
        return result.toString();
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
