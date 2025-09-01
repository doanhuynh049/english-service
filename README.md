# English Vocabulary Notification Service 📚

A Spring Boot application that automatically sends you detailed English vocabulary lessons every day at 5 AM via email, powered by Google's Gemini AI.

## 🌟 Features

- **Daily Automated Learning**: Receives 10 English vocabulary words every day at 5:00 AM
- **AI-Powered Explanations**: Uses Google Gemini AI to provide comprehensive word explanations including:
  - IPA pronunciation
  - Part of speech
  - English definitions (simple + advanced)
  - Example sentences in natural English
  - Common collocations and fixed expressions
  - Synonyms & antonyms with differences explained
  - Commonly confused words
  - Word family variations
  - Vietnamese translations with nuance
- **Email Notifications**: Beautifully formatted HTML emails sent to your Gmail
- **Excel Logging**: Persistent storage of all vocabulary words and explanations in Excel format
- **Manual Trigger**: REST API endpoint for testing and manual vocabulary sessions
- **Comprehensive Word Database**: 100+ carefully selected English vocabulary words

## 🏗️ Architecture

```
├── VocabularyScheduler    → Automated daily scheduling at 5 AM
├── GeminiClient          → Google Gemini AI API integration
├── EmailService          → HTML email formatting and sending via Gmail SMTP
├── ExcelService          → Excel file management using Apache POI
├── VocabularyService     → Core business logic coordinator
└── VocabularyController  → REST API endpoints for manual testing
```

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Gmail account with App Password enabled
- Google Gemini AI API key

### 1. Clone and Setup

```bash
git clone <your-repo-url>
cd english-service
```

### 2. Configure Application Properties

Update `src/main/resources/application.properties`:

```properties
# Change server port if needed
server.port=8282

# Update with your Gemini AI API key
app.llm-api-key=YOUR_GEMINI_API_KEY

# Update with your Gmail credentials
app.smtp-user=your-email@gmail.com
app.smtp-pass=your-app-password
app.mail-from=your-email@gmail.com
app.mail-to=your-email@gmail.com
```

### 3. Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/english-service-0.0.1-SNAPSHOT.jar
```

### 4. Test the Service

```bash
# Trigger a manual vocabulary session
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily
```

## 📧 Email Setup (Gmail)

### Enable App Passwords

1. Go to your Google Account settings
2. Navigate to **Security** → **2-Step Verification**
3. Enable 2-Step Verification if not already enabled
4. Go to **App passwords**
5. Generate a new app password for "Mail"
6. Use this 16-character password in `app.smtp-pass`

### Email Configuration

```properties
# Gmail SMTP Configuration (already configured)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 🤖 Gemini AI Setup

### Get API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Update `app.llm-api-key` in application.properties

### API Configuration

```properties
# Gemini AI Configuration (already configured)
app.llm-provider=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
```

## 📊 Excel Logging

The application automatically creates and maintains a `vocabulary_log.xlsx` file in the project root directory with:

- **Date**: When the vocabulary was learned
- **Word**: The English word
- **Explanation**: Full AI-generated explanation

### Excel File Structure

| Date | Word | Explanation |
|------|------|-------------|
| 2025-08-25 05:00:00 | eloquent | [Full AI explanation with pronunciation, examples, etc.] |
| 2025-08-25 05:00:00 | resilient | [Full AI explanation with pronunciation, examples, etc.] |

## 🕐 Scheduling

The application runs automatically at **5:00 AM every day** using Spring's `@Scheduled` annotation:

```java
@Scheduled(cron = "0 0 5 * * ?") // 5:00 AM daily
public void scheduledVocabularySession() {
    // Processes 10 random vocabulary words
}
```

### Cron Expression Breakdown
- `0 0 5 * * ?` = Every day at 5:00:00 AM
- Modify in `VocabularyScheduler.java` if you want a different time

## 🔧 API Endpoints

### Manual Vocabulary Trigger

**POST** `/api/vocabulary/trigger`

Manually triggers a vocabulary session for testing purposes.

```bash
curl -X POST http://localhost:8282/api/vocabulary/trigger
```

**Response:**
```json
"Vocabulary session triggered successfully! Check your email and Excel file."
```

## 📱 Usage Examples

### 1. Daily Automatic Usage

Just leave the application running! Every day at 5 AM, you'll receive:

1. **Email**: HTML-formatted vocabulary lesson with 10 words
2. **Excel Log**: Updated with new vocabulary entries
3. **Console Logs**: Detailed processing information

### 2. Manual Testing

```bash
# Test the service immediately
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily

# Check application status
curl http://localhost:8282/actuator/health  # If actuator is enabled
```

### 3. Customizing Word Selection

Edit the `vocabularyWords` list in `VocabularyService.java`:

```java
private final List<String> vocabularyWords = Arrays.asList(
    "your-custom-word-1",
    "your-custom-word-2",
    // Add more words here
);
```

## 🎨 Email Template

The emails are sent in beautiful HTML format with:

- **Header**: Date and vocabulary session title
- **Word Sections**: Each word with complete AI explanation
- **Footer**: Motivational message and service attribution
- **Styling**: Professional CSS styling with colors and formatting

### Sample Email Structure

```
📚 Daily English Vocabulary
Date: Monday, August 25, 2025

Word 1: ELOQUENT
──────────────────────────────
[Full Gemini AI explanation including pronunciation, examples, synonyms, etc.]

═══════════════════════════════

Word 2: RESILIENT
──────────────────────────────
[Full Gemini AI explanation...]

[...8 more words...]
```

## 📋 Configuration Reference

### Core Settings

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Application port | `8282` |
| `app.llm-api-key` | Gemini AI API key | Required |
| `app.mail-from` | Sender email | Required |
| `app.mail-to` | Recipient email | Required |
| `app.excel-file-path` | Excel file location | `vocabulary_log.xlsx` |

### Advanced Settings

| Property | Description | Default |
|----------|-------------|---------|
| `spring.task.scheduling.pool.size` | Scheduler thread pool | `2` |
| `logging.level.com.quat.englishService` | Application logging level | `INFO` |
| `spring.jackson.default-property-inclusion` | JSON serialization | `NON_NULL` |

## 🛠️ Development

### Project Structure

```
src/main/java/com/quat/englishService/
├── EnglishServiceApplication.java     # Main Spring Boot application
├── controller/
│   └── VocabularyController.java      # REST API endpoints
├── dto/
│   ├── GeminiRequest.java            # Gemini AI request DTOs
│   └── GeminiResponse.java           # Gemini AI response DTOs
├── model/
│   └── VocabularyWord.java           # Domain model
├── scheduler/
│   └── VocabularyScheduler.java      # Daily scheduling logic
└── service/
    ├── EmailService.java             # Email handling
    ├── ExcelService.java             # Excel file operations
    ├── GeminiClient.java             # AI API client
    └── VocabularyService.java        # Core business logic
```

### Building from Source

```bash
# Compile and run tests
mvn clean compile test

# Create executable JAR
mvn clean package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Adding New Words

1. Edit `VocabularyService.java`
2. Add words to the `vocabularyWords` list
3. Restart the application

### Customizing AI Prompts

Modify the prompt template in `GeminiClient.java`:

```java
private String createPrompt(String word) {
    return String.format("""
        Your custom prompt for word "%s"
        - Add your requirements here
        - Customize the output format
        """, word);
}
```

## 🔍 Troubleshooting

### Common Issues

**1. Email not sending**
- Verify Gmail App Password is correct
- Check 2-Factor Authentication is enabled
- Ensure SMTP settings are correct

**2. Gemini AI errors**
- Verify API key is valid and active
- Check API quotas and limits
- Monitor rate limiting

**3. Excel file not created**
- Check file permissions in project directory
- Verify Apache POI dependencies are loaded
- Check disk space

**4. Scheduler not running**
- Verify `@EnableScheduling` is present
- Check application is running continuously
- Review timezone settings

### Logging

Enable detailed logging by updating `application.properties`:

```properties
# Enable DEBUG logging
logging.level.com.quat.englishService=DEBUG
logging.level.org.springframework.mail=DEBUG
logging.level.org.apache.poi=DEBUG
```

### Health Checks

```bash
# Check if application is running
curl http://localhost:8282/api/vocabulary/trigger

# View application logs
tail -f logs/application.log  # If file logging is configured
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes and test thoroughly
4. Commit your changes: `git commit -am 'Add some feature'`
5. Push to the branch: `git push origin feature-name`
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Google Gemini AI** for providing detailed vocabulary explanations
- **Spring Boot** for the robust application framework
- **Apache POI** for Excel file handling capabilities
- **Jakarta Mail** for email functionality

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#-troubleshooting) section
2. Review application logs for error messages
3. Verify all configuration settings
4. Test with manual API calls

---

**Happy Learning!** 🎓 Improve your English vocabulary one word at a time with AI-powered explanations delivered right to your inbox every morning.
