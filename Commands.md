# API Commands Reference

This document contains all the curl commands to test the English Learning Service Suite APIs.

## 🚀 Quick Service Status Check

```bash
# Check if the application is running
curl -X GET http://localhost:8282/actuator/health

# Check all service statuses
curl -X GET http://localhost:8282/api/vocabulary/status
curl -X GET http://localhost:8282/api/toeic-vocabulary/status
curl -X GET http://localhost:8282/api/ielts/status
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

## 🧪 Testing Workflows

### Complete Daily Test (All Services)
```bash
# Test all daily services in sequence
echo "Testing Vocabulary Service..."
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily

echo "Testing TOEIC Vocabulary Service..."
curl -X POST http://localhost:8282/api/toeic-vocabulary/trigger-daily

echo "Testing IELTS Reading Service..."
curl -X POST http://localhost:8282/api/ielts/send-reading

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
# Get detailed status for each service
curl -X GET http://localhost:8282/api/vocabulary/status | jq
curl -X GET http://localhost:8282/api/toeic-vocabulary/status | jq
curl -X GET http://localhost:8282/api/ielts/status | jq
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
