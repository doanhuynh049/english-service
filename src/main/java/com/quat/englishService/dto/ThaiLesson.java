package com.quat.englishService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Thai language lesson data
 * Represents a single Thai learning lesson with all content and metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThaiLesson {
    
    private int day;
    private String topic;
    private String status;
    private String completedDay;
    
    // AI-generated content fields
    private String lessonTitle;
    private String contentHtml;
    private ThaiVocabulary[] vocabulary;
    private String[] exampleSentences;
    private ThaiExercise[] listeningExercises;
    private ThaiExercise[] speakingExercises;
    private ThaiQuiz[] quizQuestions;
    
    // Audio files for listening practice
    private String vocabularyAudioPath;
    private String listeningPracticeAudioPath;
    
    // Constructors
    public ThaiLesson() {}
    
    public ThaiLesson(int day, String topic) {
        this.day = day;
        this.topic = topic;
        this.status = "Open";
    }
    
    // Getters and Setters
    public int getDay() {
        return day;
    }
    
    public void setDay(int day) {
        this.day = day;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCompletedDay() {
        return completedDay;
    }
    
    public void setCompletedDay(String completedDay) {
        this.completedDay = completedDay;
    }
    
    public String getLessonTitle() {
        return lessonTitle;
    }
    
    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }
    
    public String getContentHtml() {
        return contentHtml;
    }
    
    public void setContentHtml(String contentHtml) {
        this.contentHtml = contentHtml;
    }
    
    public ThaiVocabulary[] getVocabulary() {
        return vocabulary;
    }
    
    public void setVocabulary(ThaiVocabulary[] vocabulary) {
        this.vocabulary = vocabulary;
    }
    
    public String[] getExampleSentences() {
        return exampleSentences;
    }
    
    public void setExampleSentences(String[] exampleSentences) {
        this.exampleSentences = exampleSentences;
    }
    
    public ThaiExercise[] getListeningExercises() {
        return listeningExercises;
    }
    
    public void setListeningExercises(ThaiExercise[] listeningExercises) {
        this.listeningExercises = listeningExercises;
    }
    
    public ThaiExercise[] getSpeakingExercises() {
        return speakingExercises;
    }
    
    public void setSpeakingExercises(ThaiExercise[] speakingExercises) {
        this.speakingExercises = speakingExercises;
    }
    
    public ThaiQuiz[] getQuizQuestions() {
        return quizQuestions;
    }
    
    public void setQuizQuestions(ThaiQuiz[] quizQuestions) {
        this.quizQuestions = quizQuestions;
    }
    
    public String getVocabularyAudioPath() {
        return vocabularyAudioPath;
    }
    
    public void setVocabularyAudioPath(String vocabularyAudioPath) {
        this.vocabularyAudioPath = vocabularyAudioPath;
    }
    
    public String getListeningPracticeAudioPath() {
        return listeningPracticeAudioPath;
    }
    
    public void setListeningPracticeAudioPath(String listeningPracticeAudioPath) {
        this.listeningPracticeAudioPath = listeningPracticeAudioPath;
    }
    
    @Override
    public String toString() {
        return String.format("ThaiLesson{day=%d, topic='%s', status='%s', lessonTitle='%s'}", 
                           day, topic, status, lessonTitle);
    }
    
    /**
     * Nested class for Thai vocabulary items
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThaiVocabulary {
        private String thai;
        private String ipa;
        private String romanization;
        private String english;
        private String exampleThai;
        private String exampleIpa;
        private String exampleEnglish;
        
        // Audio files
        private String pronunciationAudioPath;
        private String pronunciationAudioUrl;
        private String exampleAudioPath;
        private String exampleAudioUrl;
        
        // Constructors
        public ThaiVocabulary() {}
        
        public ThaiVocabulary(String thai, String ipa, String english) {
            this.thai = thai;
            this.ipa = ipa;
            this.english = english;
        }
        
        // Getters and Setters
        public String getThai() { return thai; }
        public void setThai(String thai) { this.thai = thai; }
        
        public String getIpa() { return ipa; }
        public void setIpa(String ipa) { this.ipa = ipa; }
        
        public String getRomanization() { return romanization; }
        public void setRomanization(String romanization) { this.romanization = romanization; }
        
        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }
        
        public String getExampleThai() { return exampleThai; }
        public void setExampleThai(String exampleThai) { this.exampleThai = exampleThai; }
        
        public String getExampleIpa() { return exampleIpa; }
        public void setExampleIpa(String exampleIpa) { this.exampleIpa = exampleIpa; }
        
        public String getExampleEnglish() { return exampleEnglish; }
        public void setExampleEnglish(String exampleEnglish) { this.exampleEnglish = exampleEnglish; }
        
        public String getPronunciationAudioPath() { return pronunciationAudioPath; }
        public void setPronunciationAudioPath(String pronunciationAudioPath) { this.pronunciationAudioPath = pronunciationAudioPath; }
        
        public String getPronunciationAudioUrl() { return pronunciationAudioUrl; }
        public void setPronunciationAudioUrl(String pronunciationAudioUrl) { this.pronunciationAudioUrl = pronunciationAudioUrl; }
        
        public String getExampleAudioPath() { return exampleAudioPath; }
        public void setExampleAudioPath(String exampleAudioPath) { this.exampleAudioPath = exampleAudioPath; }
        
        public String getExampleAudioUrl() { return exampleAudioUrl; }
        public void setExampleAudioUrl(String exampleAudioUrl) { this.exampleAudioUrl = exampleAudioUrl; }
    }
    
    /**
     * Nested class for Thai exercises (listening/speaking)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThaiExercise {
        private String type; // "listening" or "speaking"
        private String instruction;
        private String thai;
        private String ipa;
        private String english;
        private String audioHint;
        
        // Constructors
        public ThaiExercise() {}
        
        public ThaiExercise(String type, String instruction, String thai, String ipa, String english) {
            this.type = type;
            this.instruction = instruction;
            this.thai = thai;
            this.ipa = ipa;
            this.english = english;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        
        public String getThai() { return thai; }
        public void setThai(String thai) { this.thai = thai; }
        
        public String getIpa() { return ipa; }
        public void setIpa(String ipa) { this.ipa = ipa; }
        
        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }
        
        public String getAudioHint() { return audioHint; }
        public void setAudioHint(String audioHint) { this.audioHint = audioHint; }
    }
    
    /**
     * Nested class for Thai quiz questions
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ThaiQuiz {
        private String question;
        private String[] options;
        private int correctAnswer; // 0-based index
        private String explanation;
        
        // Constructors
        public ThaiQuiz() {}
        
        public ThaiQuiz(String question, String[] options, int correctAnswer, String explanation) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
        }
        
        // Getters and Setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String[] getOptions() { return options; }
        public void setOptions(String[] options) { this.options = options; }
        
        public int getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(int correctAnswer) { this.correctAnswer = correctAnswer; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
