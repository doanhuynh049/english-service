# API Commands Reference

This document contains all the curl commands to test the English Learning Service Suite APIs.

## 🚀 Quick Service Status Check

```bash
# Check if the application is running
curl -X GET http://localhost:8282/actuator/health

# Check all service statuses
curl -X GET http://localhost:8282/api/vocabulary/status
curl -X GET http://localhost:8282/api/japanese/status
curl -X GET http://localhost:8282/api/toeic-vocabulary/status
curl -X GET http://localhost:8282/api/ielts/status
curl -X GET http://localhost:8282/api/thai/status
curl -X GET http://localhost:8282/api/toeic/status
```

## 📚 Vocabulary Service (Daily Vocabulary - 5:00 AM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily
```

### Process Specific Words
```bash
curl -X POST http://localhost:8282/api/vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["eloquent", "resilient", "paradigm", "inevitable"]'
```

### Process Words with Email
```bash
curl -X POST http://localhost:8282/api/vocabulary/process-words-email \
  -H "Content-Type: application/json" \
  -d '["sophisticated", "comprehensive", "fundamental"]'
```

### Generate TOEIC Vocabulary
```bash
curl -X GET "http://localhost:8282/api/vocabulary/generate-toeic?count=5"
```

## 🎯 TOEIC Vocabulary Service (Advanced Vocabulary - 9:00 AM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/toeic-vocabulary/trigger-daily
```

### Generate New Words
```bash
curl -X GET http://localhost:8282/api/toeic-vocabulary/generate-new
```

### Process Specific Words
```bash
curl -X POST http://localhost:8282/api/toeic-vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["alleviate", "comply", "facilitate", "implement", "optimize"]'
```

### Send Test Email
```bash
curl -X POST http://localhost:8282/api/toeic-vocabulary/send-test-email \
  -H "Content-Type: application/json" \
  -d '["disseminate", "mitigate", "pivotal", "concurrently", "incentivize"]'
```

### Service Status
```bash
curl -X GET http://localhost:8282/api/toeic-vocabulary/status
```

## 📖 IELTS Reading Service (Academic Reading - 11:00 AM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/ielts/send-reading
```

### Generate Specific Topic
```bash
curl -X POST http://localhost:8282/api/ielts/generate-topic \
  -H "Content-Type: application/json" \
  -d '{"topic": "climate change", "difficulty": "advanced"}'
```

### Service Status
```bash
curl -X GET http://localhost:8282/api/ielts/status
```

## 🎧 TOEIC Listening Service (Business Listening - 6:00 PM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/toeic/trigger-listening
```

### Trigger TOEIC Part 7
```bash
curl -X POST http://localhost:8282/api/toeic/trigger-part7
```

### Service Status
```bash
curl -X GET http://localhost:8282/api/toeic/status
```

## 🎵 Audio Service

### Stream Audio File
```bash
curl -X GET "http://localhost:8282/api/audio/stream?filename=word_pronunciation.mp3"
```

### List Available Audio Files
```bash
curl -X GET http://localhost:8282/api/audio/list
```

## 🇯🇵 Japanese Learning Service (Daily Lessons - 7:00 AM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/japanese/trigger-daily
```

### Process Specific Lesson
```bash
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Hiragana 1",
    "description": "Learn Hiragana: あ, い, う. Practice writing, reading, and 2 example words.",
    "day": 1,
    "phase": "Month 1: Hiragana + Basics"
  }'
```

### Test with Different Topics
```bash
# Hiragana lesson
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Hiragana Characters",
    "description": "Master the basic hiragana syllabary",
    "day": 5
  }'

# Katakana lesson
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Katakana Practice",
    "description": "Learn katakana for foreign words",
    "day": 15
  }'

# Basic Kanji lesson
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Basic Kanji",
    "description": "Introduction to essential kanji characters",
    "day": 30
  }'

# Grammar lesson
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Present Tense Verbs",
    "description": "Learn present tense verb conjugation patterns",
    "day": 45
  }'
```

### Service Status and Health
```bash
# Get detailed service information
curl -X GET http://localhost:8282/api/japanese/status

# Health check
curl -X GET http://localhost:8282/api/japanese/health
```

### Excel Curriculum Format
The Japanese service reads from an Excel file with the following structure:
- **Column A**: Day (numeric)
- **Column B**: Phase (e.g., "Month 1: Hiragana + Basics")
- **Column C**: Topic (e.g., "Hiragana 1")
- **Column D**: Description (lesson details)
- **Column E**: Status ("Open" or "Done")

Example Excel content:
```bash
# View expected Excel format via API
curl -X GET http://localhost:8282/api/japanese/status | jq '.excelFormat'
```

## 🇹🇭 Thai Learning Service (Daily Thai Lessons - 5:00 PM)

### Trigger Daily Processing
```bash
curl -X POST http://localhost:8282/api/thai/trigger-daily
```

### Process Specific Lesson
```bash
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Basic Greetings and Introductions",
    "day": 1
  }'
```

### Test with Different Topics
```bash
# Basic greetings lesson
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Basic Greetings and Introductions",
    "day": 1
  }'

# Numbers lesson
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Numbers 1-10",
    "day": 5
  }'

# Colors and basic adjectives
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Colors and Basic Adjectives",
    "day": 10
  }'

# Food and drinks
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Food and Drinks",
    "day": 15
  }'

# Directions and locations
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Asking for Directions",
    "day": 30
  }'

# Shopping conversation
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Shopping and Bargaining",
    "day": 45
  }'

# Transportation
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Transportation and Travel",
    "day": 60
  }'

# Daily routine conversation
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Daily Routines and Time",
    "day": 75
  }'
```

### Service Status and Health
```bash
# Get detailed service information
curl -X GET http://localhost:8282/api/thai/status

# Health check
curl -X GET http://localhost:8282/api/thai/health
```

### Excel Curriculum Format
The Thai learning service reads from an Excel file with the following structure:
- **Column A**: Day (numeric) - 1 to 90 days
- **Column B**: Topic (lesson topic focused on speaking and listening)
- **Column C**: Status ("Open" or "Done")
- **Column D**: Completed Day (timestamp when completed)

Example Excel content:
```bash
# View expected Excel format via API
curl -X GET http://localhost:8282/api/thai/status | jq '.excelFormat'
```

### Learning Features
The Thai service focuses on practical conversation skills:
- **IPA Pronunciation**: All Thai vocabulary includes IPA transcription
- **Cultural Context**: Lessons include cultural notes and proper usage
- **Speaking Exercises**: Pronunciation drills and conversation practice
- **Listening Comprehension**: Audio-based exercises for beginners
- **Interactive Quizzes**: Multiple choice and fill-in-the-blank questions

## 🧪 Testing Workflows

### Complete Daily Test (All Services)
```bash
# Test all daily services in sequence
echo "Testing Vocabulary Service..."
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily

echo "Testing Japanese Learning Service..."
curl -X POST http://localhost:8282/api/japanese/trigger-daily

echo "Testing TOEIC Vocabulary Service..."
curl -X POST http://localhost:8282/api/toeic-vocabulary/trigger-daily

echo "Testing IELTS Reading Service..."
curl -X POST http://localhost:8282/api/ielts/send-reading

echo "Testing Thai Learning Service..."
curl -X POST http://localhost:8282/api/thai/trigger-daily

echo "Testing TOEIC Listening Service..."
curl -X POST http://localhost:8282/api/toeic/trigger-listening
```

### Quick Word Processing Test
```bash
# Test vocabulary processing with custom words
curl -X POST http://localhost:8282/api/vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["innovation", "collaboration", "sustainability"]'

# Test TOEIC vocabulary processing
curl -X POST http://localhost:8282/api/toeic-vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["leverage", "streamline", "optimize"]'
```

### Email Testing
```bash
# Send test emails for each service
curl -X POST http://localhost:8282/api/vocabulary/process-words-email \
  -H "Content-Type: application/json" \
  -d '["test", "example"]'

curl -X POST http://localhost:8282/api/toeic-vocabulary/send-test-email \
  -H "Content-Type: application/json" \
  -d '["evaluate", "implement"]'
```

## 📊 Monitoring and Health Checks

### Application Health
```bash
# Basic health check
curl -X GET http://localhost:8282/actuator/health

# Detailed health information
curl -X GET http://localhost:8282/actuator/info
```

### Service-Specific Status
```bash
# Get detailed status for each service including Japanese and Thai
curl -X GET http://localhost:8282/api/vocabulary/status | jq
curl -X GET http://localhost:8282/api/japanese/status | jq
curl -X GET http://localhost:8282/api/toeic-vocabulary/status | jq
curl -X GET http://localhost:8282/api/ielts/status | jq
curl -X GET http://localhost:8282/api/thai/status | jq
curl -X GET http://localhost:8282/api/toeic/status | jq
```

## 🛠️ Development and Debugging

### Generate Content Only (No Email)
```bash
# Generate vocabulary words without sending email
curl -X GET http://localhost:8282/api/toeic-vocabulary/generate-new

# Process words without email
curl -X POST http://localhost:8282/api/vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["debug", "test"]'
```

### Custom Parameters
```bash
# Generate specific number of TOEIC words
curl -X GET "http://localhost:8282/api/vocabulary/generate-toeic?count=3"

# Test with specific difficulty or topic (if supported)
curl -X POST http://localhost:8282/api/ielts/generate-topic \
  -H "Content-Type: application/json" \
  -d '{"topic": "technology", "difficulty": "intermediate"}'
```

## 📝 Response Examples

### Successful Response
```json
{
  "success": true,
  "message": "Daily TOEIC vocabulary processing completed successfully",
  "data": {
    "wordsGenerated": 10,
    "wordsSelected": 15,
    "emailSent": true
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": "Failed to generate new TOEIC words",
  "details": "API rate limit exceeded"
}
```

## 🔧 Troubleshooting Commands

### Check Application Logs
```bash
# If using Docker or systemd, check logs
journalctl -u english-service -f

# Or check application log files
tail -f logs/application.log
```

### Test Individual Components
```bash
# Test AI API connectivity
curl -X POST http://localhost:8282/api/toeic-vocabulary/generate-new

# Test email service
curl -X POST http://localhost:8282/api/toeic-vocabulary/send-test-email \
  -H "Content-Type: application/json" \
  -d '["test"]'

# Test Excel service by checking word generation
curl -X GET http://localhost:8282/api/toeic-vocabulary/status
```

---

## 📚 Notes

- Replace `localhost:8282` with your actual server address and port
- Make sure the application is running before executing commands
- Use `jq` for prettier JSON output: `curl ... | jq`
- Check application logs for detailed error information
- All services are configured to run automatically at scheduled times
