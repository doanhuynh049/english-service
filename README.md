# English Learning & Japanese/Thai Service Suite 📚🎧📖🇯🇵🇹🇭

A comprehensive Spring Boot application that automatically delivers five types of English, Japanese, and Thai learning content via e# Test Japanese lesson service (generates daily lesson from Excel curriculum)
curl -X POST http://localhost:8282/api/japanese/trigger-daily

# Test TOEIC vocabulary service (generates 15 words: 10 new + 5 review for TOEIC 800+)
curl -X POST http://localhost:8282/api/toeic-vocabulary/trigger-daily

# Test IELTS reading service (generates academic passage + explanations)
curl -X POST http://localhost:8282/api/ielts/send-reading

# Test Thai learning service (generates speaking & listening lesson for beginners)
curl -X POST http://localhost:8282/api/thai/trigger-daily

# Test TOEIC listening service (generates collocations + 3 audio passages)
curl -X POST http://localhost:8282/api/toeic/trigger-listeningaily Vocabulary** (5:00 AM): 4 vocabulary words with AI explanations and audio
- **Japanese Lesson** (7:00 AM): Structured Japanese lessons from Excel curriculum (Hiragana, Katakana, Kanji, Grammar)
- **IELTS Reading Practice** (11:00 AM): Academic reading passages with detailed explanations
- **Thai Language Learning** (5:00 PM): Daily Thai lessons focused on speaking and listening for beginners
- **TOEIC Listening Practice** (6:00 PM): Business collocations with audio passages

All powered by Google's Gemini AI and Google Text-to-Speech for immersive, multi-modal learning.

## 🌟 Features

### 📚 Daily Vocabulary Learning (5:00 AM)
- **Smart Word Selection**: 3 new words + 1 review word based on learning history
- **AI-Powered Explanations**: Comprehensive explanations including:
  - IPA pronunciation with slow/normal speech audio
  - Part of speech and detailed definitions
  - Natural example sentences with contextual audio
  - Collocations, synonyms, antonyms, and word families
  - Vietnamese translations with cultural nuance
  - AI-generated monologues for natural context
- **Multi-Modal Content**: Text explanations + pronunciation audio + contextual audio

### 🇯🇵 Japanese Learning Service (7:00 AM)
- **Structured Lessons**: Daily lessons from Excel curriculum covering Hiragana, Katakana, Kanji, and Grammar
- **Multi-Phase Learning**: Organized progression through foundation, intermediate, and advanced levels
- **AI-Generated Content**:
  - Detailed lesson explanations with romaji transliteration
  - Natural example sentences with pronunciation guides
  - Interactive practice tasks for writing and speaking
  - Listening practice with vocabulary and audio
- **Complete Learning Package**:
  - HTML email with lesson content and examples
  - Learning summary saved to Excel for progress tracking
  - Vocabulary entries with Japanese, romaji, and translations
  - Audio files for pronunciation and listening practice
- **Smart Progress Management**: Automatic status tracking from "Open" to "Done" in Excel curriculum

### 📖 IELTS Reading Practice (11:00 AM)
- **Academic Passages**: Authentic IELTS-style reading passages (250-300 words)
- **Diverse Topics**: Science, technology, environment, education, history, health, culture
- **Comprehensive Questions**: 5 multiple-choice questions testing comprehension and inference
- **Detailed Explanations**: AI-generated breakdown including:
  - Main idea and paragraph summaries
  - Key vocabulary definitions with synonyms
  - Implicit vs explicit information analysis
  - Question-answering strategies
  - Academic writing structure insights
- **Professional Format**: Clean HTML email with academic styling

### 🎧 TOEIC Listening Practice (6:00 PM)  
- **Business Collocations**: 10 common TOEIC collocations (score range 600-950)
- **Authentic Practice**: 3 TOEIC Part 4 style listening passages
- **Complete Package**: 
  - Structured collocation explanations with IPA, meanings, and examples
  - Audio passages (150-180 words each) in workplace contexts
  - Multiple-choice comprehension questions with answer keys
  - Practice instructions and listening strategies
  - Professional HTML email template with orange/blue theme

### 🇹🇭 Thai Language Learning (5:00 PM)
- **Beginner-Focused**: Daily Thai lessons designed specifically for absolute beginners
- **Speaking & Listening Emphasis**: Practical conversation skills with pronunciation guidance
- **Comprehensive Content**: AI-generated lessons including:
  - Vocabulary with IPA pronunciation
  - Example sentences with cultural context
  - Listening comprehension exercises
  - Speaking practice drills
  - Interactive quiz questions
- **90-Day Curriculum**: Structured progression from basic greetings to conversational Thai
- **Excel-Based Management**: Automatic lesson tracking and progress monitoring

### 🎯 TOEIC Vocabulary Practice (9:00 AM)
- **Advanced Vocabulary**: 15 TOEIC words daily (10 new + 5 review) targeting score 800+
- **Part 6 & 7 Focus**: Words specifically chosen for Text Completion and Reading Comprehension
- **AI-Generated Content**:
  - Business and academic context definitions
  - Professional example sentences in TOEIC style
  - Common collocations for business communication
  - Vietnamese translations for better understanding
- **Smart Learning System**:
  - Excel logging with automatic word history tracking
  - Intelligent word selection avoiding recent duplicates
  - Progressive difficulty based on TOEIC score targets
- **Professional Email**: Beautiful HTML template with modern design and clear structure

### 🔧 System Features
- **Five Automated Scheduling**: Five daily sessions with different content types and timings
- **Beautiful HTML Emails**: Professional templates for vocabulary, Japanese, Thai, IELTS, and TOEIC content  
- **Audio Generation**: High-quality TTS with different speeds for learning
- **Excel Logging**: Persistent tracking of all vocabulary and progress
- **Manual Testing**: REST API endpoints for immediate testing
- **Comprehensive Attachments**: Audio files and text documents in relevant emails

## 🏗️ Architecture

```
├── VocabularyScheduler       → Daily vocabulary at 5:00 AM (4 words: 3 new + 1 review)
├── JapaneseScheduler         → Daily Japanese lesson at 7:00 AM (from Excel curriculum)
├── IeltsScheduler           → Daily IELTS reading at 11:00 AM (academic passages + explanations)
├── ThaiLearningScheduler    → Daily Thai lesson at 5:00 PM (speaking & listening for beginners)
├── ToeicVocabularyScheduler → Daily TOEIC vocabulary at 9:00 AM (15 words: 10 new + 5 review)
├── ToeicScheduler           → Daily TOEIC listening at 6:00 PM (collocations + passages)
├── GeminiClient             → Google Gemini AI integration for content generation
├── EmailService             → Multi HTML templates for all language services
├── AudioService             → TTS generation with Python/gTTS integration
├── ExcelService             → Progress tracking and word history management
├── VocabularyService        → Core vocabulary processing with AI monologues
├── JapaneseLessonService    → Japanese lesson generation and processing
├── ThaiLearningService      → Thai lesson generation for speaking & listening
├── IeltsReadingService      → IELTS academic reading generation and processing
├── ToeicVocabularyService   → Advanced TOEIC vocabulary for score 800+ (Part 6 & 7)
├── ToeicListeningService    → TOEIC content generation and audio processing
├── VocabularyController     → REST API for vocabulary testing
├── JapaneseController       → REST API for Japanese lesson testing
├── ThaiLearningController   → REST API for Thai lesson testing
├── IeltsController          → REST API for IELTS testing
├── ToeicVocabularyController → REST API for TOEIC vocabulary testing
├── ToeicController          → REST API for TOEIC testing
└── AudioController          → Audio file serving and streaming
```

## 📅 Complete Daily Learning Schedule

The application provides a comprehensive English, Japanese, and Thai learning experience with five automated sessions throughout the day:

| Time | Service | Content | Duration | Focus |
|------|---------|---------|----------|-------|
| **5:00 AM** | 📚 Vocabulary | 4 words (3 new + 1 review) | ~15 min | Basic → Advanced vocabulary |
| **7:00 AM** | 🇯🇵 Japanese | Daily lesson from curriculum | ~20 min | Hiragana/Katakana/Kanji/Grammar |
| **9:00 AM** | 🎯 TOEIC Vocabulary | 15 words (10 new + 5 review) | ~20 min | Advanced TOEIC vocabulary (800+) |
| **11:00 AM** | 📖 IELTS Reading | Academic passage + questions | ~20 min | Reading comprehension |
| **5:00 PM** | 🇹🇭 Thai Learning | Speaking & listening lesson | ~20 min | Practical conversation skills |
| **6:00 PM** | 🎧 TOEIC Listening | Business collocations + audio | ~25 min | Listening skills |

**Total Daily Learning Time**: ~120 minutes of structured English, Japanese, and Thai practice

### Learning Progression
- **Early Morning (5 AM)**: Start with vocabulary foundation building
- **Morning (7 AM)**: Japanese language basics and grammar
- **Mid-Morning (9 AM)**: Advanced TOEIC vocabulary for business contexts
- **Midday (11 AM)**: Academic reading skills for IELTS preparation
- **Evening (5 PM)**: Thai conversation and pronunciation practice
- **Evening (6 PM)**: Business English listening for TOEIC preparation

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

### 4. Test the Services

```bash
# Test vocabulary service (generates 4 words: 3 new + 1 review)
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily

# Test Japanese lesson service (generates daily lesson from Excel curriculum)
curl -X POST http://localhost:8282/api/japanese/trigger-daily

# Test TOEIC vocabulary service (generates 15 words: 10 new + 5 review for TOEIC 800+)
curl -X POST http://localhost:8282/api/toeic-vocabulary/trigger-daily

# Test IELTS reading service (generates academic passage + explanations)
curl -X POST http://localhost:8282/api/ielts/send-reading

# Test TOEIC listening service (generates collocations + 3 audio passages)
curl -X POST http://localhost:8282/api/toeic/trigger-listening
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
curl -X POST http://localhost:8282/api/toeic/trigger-part7

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

The application runs five automated daily sessions:

### 📚 Vocabulary Session (5:00 AM)
```java
@Scheduled(cron = "0 0 5 * * ?") // 5:00 AM daily
public void scheduledVocabularySession() {
    // Processes 4 vocabulary words (3 new + 1 review)
    // Generates AI explanations, monologues, and audio
    // Sends vocabulary email with attachments
}
```

### 🇯🇵 Japanese Lesson Session (7:00 AM)
```java
@Scheduled(cron = "0 0 7 * * ?") // 7:00 AM daily
public void scheduledJapaneseLessonSession() {
    // Processes daily Japanese lesson from Excel curriculum
    // Generates AI explanations, audio, and example sentences
    // Sends Japanese lesson email with attachments
}
```

### 📖 IELTS Reading Session (11:00 AM)
```java
@Scheduled(cron = "0 0 11 * * ?") // 11:00 AM daily
public void scheduledIeltsReadingSession() {
    // Generates academic reading passage (250-300 words)
    // Creates 5 multiple-choice comprehension questions
    // Produces detailed explanations and analysis
    // Sends IELTS email with complete study materials
}
```

### 🇹🇭 Thai Learning Session (5:00 PM)
```java
@Scheduled(cron = "0 0 17 * * ?") // 5:00 PM daily
public void scheduledThaiLearningSession() {
    // Processes daily Thai lesson from Excel curriculum (90 days)
    // Generates AI content focused on speaking and listening
    // Creates vocabulary with IPA pronunciation
    // Produces listening and speaking exercises
    // Sends Thai lesson email with practice materials
}
```

### 🎧 TOEIC Listening Session (6:00 PM)
```java
@Scheduled(cron = "0 0 18 * * ?") // 6:00 PM daily  
public void scheduledToeicListeningSession() {
    // Generates 10 TOEIC collocations
    // Creates 3 Part 4 style listening passages
    // Produces audio files and comprehension questions
    // Sends TOEIC email with practice materials
}
```

### Cron Expression Guide
- `0 0 5 * * ?` = Every day at 5:00:00 AM
- `0 0 7 * * ?` = Every day at 7:00:00 AM
- `0 0 11 * * ?` = Every day at 11:00:00 AM
- `0 0 17 * * ?` = Every day at 5:00:00 PM
- `0 0 18 * * ?` = Every day at 6:00:00 PM
- Modify in respective scheduler files to change times

## 🔧 API Endpoints

### Vocabulary Endpoints

**POST** `/api/vocabulary/trigger-daily`
Manually triggers a vocabulary session (4 words: 3 new + 1 review).

```bash
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily
```

**POST** `/api/vocabulary/process-words`
Process specific custom words.

```bash
curl -X POST http://localhost:8282/api/vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["eloquent", "resilient", "serendipity"]'
```

**GET** `/api/vocabulary/health`
Check vocabulary service health.

```bash
curl http://localhost:8282/api/vocabulary/health
```

### Japanese Learning Endpoints

**POST** `/api/japanese/trigger-daily`
Manually triggers daily Japanese lesson processing from Excel curriculum.

```bash
curl -X POST http://localhost:8282/api/japanese/trigger-daily
```

**POST** `/api/japanese/process-lesson`
Process a specific Japanese lesson with custom content.

```bash
curl -X POST http://localhost:8282/api/japanese/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Hiragana あいう",
    "description": "Learn basic hiragana characters",
    "day": 1
  }'
```

**GET** `/api/japanese/status`
Get service status and configuration details.

```bash
curl http://localhost:8282/api/japanese/status
```

**GET** `/api/japanese/health`
Check Japanese service health.

```bash
curl http://localhost:8282/api/japanese/health
```

### Thai Learning Endpoints

**POST** `/api/thai/trigger-daily`
Manually triggers daily Thai lesson processing for speaking and listening practice.

```bash
curl -X POST http://localhost:8282/api/thai/trigger-daily
```

**POST** `/api/thai/process-lesson`
Process a specific Thai lesson with custom content.

```bash
curl -X POST http://localhost:8282/api/thai/process-lesson \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Basic Greetings and Introductions",
    "day": 1
  }'
```

**GET** `/api/thai/status`
Get Thai service status and configuration details.

```bash
curl http://localhost:8282/api/thai/status
```

**GET** `/api/thai/health`
Check Thai service health.

```bash
curl http://localhost:8282/api/thai/health
```

### IELTS Endpoints

**POST** `/api/ielts/send-reading`
Manually triggers IELTS reading practice generation.

```bash
curl -X POST http://localhost:8282/api/ielts/send-reading
```

### TOEIC Endpoints

**POST** `/api/toeic/trigger-listening`
Manually triggers TOEIC listening practice generation.

```bash
curl -X POST http://localhost:8282/api/toeic/trigger-listening
```

**GET** `/api/toeic/health`
Check TOEIC service health.

```bash
curl http://localhost:8282/api/toeic/health
```

### Audio Endpoints

**GET** `/audio/{date}/{filename}`
Stream or download audio files.

```bash
# Download audio file
curl http://localhost:8282/audio/2025-09-08/eloquent_pronunciation.mp3 -o pronunciation.mp3

# Stream in browser
open http://localhost:8282/audio/2025-09-08/toeic_passage_1.mp3
```

**GET** `/audio/health`
Check audio service health.

```bash
curl http://localhost:8282/audio/health
```

## 📱 Usage Examples

### 1. Daily Automatic Usage

Leave the application running for automated learning:

**5:00 AM Daily:** Vocabulary Email
- 4 vocabulary words (3 new + 1 review word)
- AI explanations with pronunciation and context
- Audio files for pronunciation and examples
- Monologue transcript document
- Excel log updated with learning history

**7:00 AM Daily:** Japanese Lesson Email
- Daily Japanese lesson from Excel curriculum
- Structured content covering Hiragana, Katakana, Kanji, and Grammar
- AI-generated explanations and example sentences
- Audio files for pronunciation and listening practice
- Progress tracked in Excel curriculum

**11:00 AM Daily:** IELTS Reading Email
- Academic reading passage (250-300 words)
- 5 multiple-choice comprehension questions with answers
- Detailed explanations covering main ideas and vocabulary
- Academic writing analysis and question strategies
- Professional formatting for study focus

**5:00 PM Daily:** Thai Learning Email
- Daily Thai lesson from Excel curriculum (90-day program)
- Vocabulary with IPA pronunciation and cultural context
- Practical conversation examples and listening exercises
- Speaking practice drills with tone emphasis
- Interactive quiz questions for comprehension
- Progress tracked in Excel curriculum

**6:00 PM Daily:** TOEIC Listening Email  
- 10 business collocations with explanations
- 3 TOEIC Part 4 style audio passages
- Comprehension questions with answer keys
- Practice instructions and listening tips
- Complete passages text file

### 2. Manual Testing

```bash
# Test vocabulary service immediately
curl -X POST http://localhost:8282/api/vocabulary/trigger-daily

# Test Japanese lesson service immediately
curl -X POST http://localhost:8282/api/japanese/trigger-daily

# Test IELTS reading service immediately
curl -X POST http://localhost:8282/api/ielts/send-reading

# Test Thai learning service immediately
curl -X POST http://localhost:8282/api/thai/trigger-daily

# Test TOEIC listening service immediately  
curl -X POST http://localhost:8282/api/toeic/trigger-part7

# Process custom vocabulary words
curl -X POST http://localhost:8282/api/vocabulary/process-words \
  -H "Content-Type: application/json" \
  -d '["innovative", "meticulous", "collaborate"]'

# Check service health
curl http://localhost:8282/api/vocabulary/health
curl http://localhost:8282/api/japanese/health
curl http://localhost:8282/api/thai/health
curl http://localhost:8282/api/toeic/health
curl http://localhost:8282/audio/health

# Download today's audio files
curl http://localhost:8282/audio/2025-09-08/eloquent_pronunciation.mp3 -o eloquent.mp3
curl http://localhost:8282/audio/2025-09-08/toeic_passage_1.mp3 -o toeic1.mp3
```

### 3. Audio File Access

Audio files are organized by date and accessible via HTTP:

```
http://localhost:8282/audio/2025-09-08/
├── eloquent_pronunciation.mp3      # Word pronunciation (slow speech)
├── eloquent_monologue.mp3          # Contextual monologue (normal speech)
├── toeic_passage_1.mp3             # TOEIC passage 1 with questions
├── toeic_passage_2.mp3             # TOEIC passage 2 with questions
└── toeic_passage_3.mp3             # TOEIC passage 3 with questions
```

## 🎨 Email Templates

The application sends four types of beautiful HTML emails:

### 📚 Vocabulary Email Template (`email-template.html`)
- **Header**: Date and vocabulary session title
- **Word Sections**: Each word with complete AI explanation
- **Audio Player Links**: Direct access to pronunciation and example audio
- **Footer**: Motivational message and service attribution
- **Styling**: Professional CSS with green theme and clean formatting

### 🇯🇵 Japanese Lesson Email Template (`japanese-email-template.html`)
- **Header**: Date and Japanese lesson title
- **Lesson Content**: Structured lesson with Hiragana, Katakana, Kanji, and Grammar
- **Example Sentences**: Natural examples with romaji and audio
- **Practice Tasks**: Interactive writing and speaking tasks
- **Footer**: Study tips and service attribution
- **Styling**: Professional CSS with red theme and clean formatting

### 🇹🇭 Thai Lesson Email Template (`thai-email-template.html`)
- **Header**: Date and Thai lesson title with beginner-friendly design
- **Vocabulary Section**: Thai words with IPA pronunciation and cultural context
- **Example Sentences**: Practical conversations with pronunciation guides
- **Listening Exercises**: Audio-based comprehension activities
- **Speaking Practice**: Pronunciation drills and conversation scenarios
- **Quiz Section**: Interactive questions to test comprehension
- **Footer**: Encouragement and cultural tips
- **Styling**: Professional CSS with Thai-inspired orange/gold theme

### 📖 IELTS Email Template (`ielts-email-template.html`)
- **Header**: IELTS Academic Reading title with date
- **Passage Section**: Complete reading passage with topic introduction
- **Questions Section**: 5 multiple-choice questions with clear formatting
- **Explanations Section**: Detailed breakdown of main ideas, vocabulary, and strategies
- **Answer Key**: Complete answers with reasoning
- **Styling**: Academic blue theme with scholarly design

### 🎧 TOEIC Email Template (`toeic-email-template.html`)
- **Header**: TOEIC Listening Practice title with date
- **Collocations Section**: 10 business collocations with detailed explanations
- **Audio Files Section**: Links to 3 listening passages with descriptions
- **Practice Tips**: Instructions for effective TOEIC preparation
- **Styling**: Orange/blue theme with modern business design

### Sample Vocabulary Email Structure

```
📚 Daily English Vocabulary
Date: Monday, August 25, 2025

Word 1: ELOQUENT
──────────────────────────────
[Full Gemini AI explanation including pronunciation, examples, synonyms, etc.]
🔊 Audio Files:
- Pronunciation (slow speech)
- Example in context (normal speech)

═══════════════════════════════

Word 2: RESILIENT
──────────────────────────────
[Full Gemini AI explanation...]

[...2 more words...]
```

### Sample Japanese Lesson Email Structure

```
🇯🇵 Japanese Lesson: Hiragana あいう
Date: Monday, August 25, 2025

📘 HIRAGANA CHART
─────────────────────
あ い う

📝 LESSON CONTENT
─────────────────────
- Topic: Basic Hiragana Characters
- Description: Learn the first set of hiragana characters.

🔊 AUDIO FILES
─────────────────────
- Pronunciation guide for あ, い, う
- Example words with audio

✏️ PRACTICE TASKS
─────────────────────
1. Write out the characters あ, い, う
2. Practice pronunciation with audio
```

### Sample Thai Lesson Email Structure

```
🇹🇭 Thai Language Learning: Basic Greetings
Date: Monday, August 25, 2025

📚 TODAY'S VOCABULARY
─────────────────────
1. สวัสดี (sà-wàt-dii) /sa˨˩.wat̚˨˩.diː˧/ - Hello/Goodbye
   Cultural Note: Used any time of day, hands in prayer position

2. ขอบคุณ (kɔ̀ɔp-kun) /kʰɔ̀ːp̚˨˩.kʰun˧/ - Thank you
   Usage: Most common way to express gratitude

3. ชื่อ (chɯ̂ɯ) /tɕʰɯ̂ː˥˩/ - Name
   Example: ชื่อของฉันคือ... (My name is...)

📝 EXAMPLE CONVERSATIONS
─────────────────────────
A: สวัสดีครับ ผมชื่อจอห์น (Hello, my name is John)
B: สวัสดีค่ะ ดิฉันชื่อมาลี (Hello, my name is Mali)
A: ยินดีที่ได้รู้จักครับ (Nice to meet you)

🎧 LISTENING EXERCISES
─────────────────────
1. Listen and repeat the greetings
2. Identify the speaker's name in the conversation
3. Practice correct tone pronunciation

🗣️ SPEAKING PRACTICE
─────────────────────
1. Introduce yourself using today's vocabulary
2. Practice the five Thai tones with สวัสดี
3. Role-play meeting someone new

❓ QUIZ QUESTIONS
─────────────────────
1. How do you say "thank you" in Thai?
   a) สวัสดี  b) ขอบคุณ  c) ชื่อ

2. When can you use สวัสดี?
   a) Only morning  b) Only evening  c) Any time of day

💡 CULTURAL TIP
─────────────────────
Always use ครับ (khráp) if you're male or ค่ะ (khâ) if you're female 
at the end of polite sentences!
```

### Sample IELTS Email Structure

```
📖 IELTS Academic Reading Practice
Date: Monday, September 09, 2025

📘 READING PASSAGE
─────────────────────
Topic: The Impact of Artificial Intelligence on Modern Healthcare

[250-300 word academic passage about AI in healthcare...]

📝 COMPREHENSION QUESTIONS
─────────────────────────────
1. According to the passage, what is the primary benefit of AI in medical diagnosis?
   A. Reduced costs for patients
   B. Faster and more accurate diagnosis
   C. Elimination of human doctors
   D. Simplified medical procedures

[...4 more questions...]

📚 DETAILED EXPLANATIONS
───────────────────────
Main Idea: The passage discusses how artificial intelligence is revolutionizing healthcare...

Key Vocabulary:
• sophisticated /səˈfɪstɪkeɪtɪd/ - advanced and complex
• diagnostic /ˌdaɪəɡˈnɒstɪk/ - relating to medical diagnosis
• algorithms /ˈælɡərɪðəmz/ - computer procedures for calculations

[...detailed analysis and strategies...]

✅ ANSWER KEY
─────────────
1. B - Faster and more accurate diagnosis
[...explanations for each answer...]
```

### Sample TOEIC Email Structure

```
🎧 TOEIC Listening Practice
Date: Monday, August 25, 2025

📘 BUSINESS COLLOCATIONS
─────────────────────────
1. make a decision /meɪk ə dɪˈsɪʒən/
   Meaning: To choose between alternatives
   Example: The CEO will make a decision about the merger tomorrow.

[...9 more collocations...]

🎵 LISTENING PASSAGES
─────────────────────
📁 toeic_passage_1.mp3 - Company Meeting (152 words)
📁 toeic_passage_2.mp3 - Product Launch (167 words)  
📁 toeic_passage_3.mp3 - Customer Service (148 words)

📄 Complete passages and questions available in attached document.
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
│   ├── VocabularyController.java      # Vocabulary REST API endpoints
│   ├── JapaneseController.java        # Japanese REST API endpoints
│   ├── IeltsController.java           # IELTS REST API endpoints
│   ├── ToeicController.java           # TOEIC REST API endpoints
│   └── AudioController.java           # Audio streaming endpoints
├── dto/
│   ├── GeminiRequest.java            # Gemini AI request DTOs
│   ├── GeminiResponse.java           # Gemini AI response DTOs
│   └── ParsedVocabularyWord.java     # Vocabulary word DTO
├── model/
│   └── VocabularyWord.java           # Domain model
├── scheduler/
│   ├── VocabularyScheduler.java      # Daily vocabulary scheduling (5:00 AM)
│   ├── JapaneseScheduler.java        # Daily Japanese lesson scheduling (7:00 AM)
│   ├── IeltsScheduler.java           # Daily IELTS scheduling (11:00 AM)
│   └── ToeicScheduler.java           # Daily TOEIC scheduling (6:00 PM)
└── service/
    ├── EmailService.java             # Quad email handling (vocabulary + Japanese + IELTS + TOEIC)
    ├── ExcelService.java             # Excel file operations and logging
    ├── GeminiClient.java             # AI API client with custom prompts
    ├── AudioService.java             # TTS generation with Python/gTTS
    ├── VocabularyService.java        # Core vocabulary business logic
    ├── JapaneseLessonService.java    # Japanese lesson generation and processing
    ├── IeltsReadingService.java      # IELTS academic reading generation and processing
    ├── ToeicListeningService.java    # TOEIC content generation and processing
    └── CollocationHistoryService.java # Smart collocation history management

src/main/resources/
├── application.properties            # Main configuration
├── email-template.html              # Vocabulary email template
├── japanese-email-template.html      # Japanese lesson email template
├── ielts-email-template.html        # IELTS email template
└── toeic-email-template.html        # TOEIC email template
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

**Happy Learning!** 🎓 Improve your English and Japanese skills with AI-powered explanations delivered right to your inbox every morning.
