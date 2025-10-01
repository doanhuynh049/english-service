package com.quat.englishService.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for daily learning summary
 * Contains all learning activities for a specific day
 */
public class LearningDaySummary {
    private LocalDate date;
    private String vocabularyWords;
    private String japaneseLesson;
    private String toeicPractice;
    private String ieltsReading;
    private String keyLearningPoints;
    private String practiceActivities;
    private String recommendedFocus;
    private LocalDateTime createdAt;

    // Constructors
    public LearningDaySummary() {
        this.createdAt = LocalDateTime.now();
    }

    public LearningDaySummary(LocalDate date) {
        this.date = date;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getVocabularyWords() { return vocabularyWords; }
    public void setVocabularyWords(String vocabularyWords) { this.vocabularyWords = vocabularyWords; }

    public String getJapaneseLesson() { return japaneseLesson; }
    public void setJapaneseLesson(String japaneseLesson) { this.japaneseLesson = japaneseLesson; }

    public String getToeicPractice() { return toeicPractice; }
    public void setToeicPractice(String toeicPractice) { this.toeicPractice = toeicPractice; }

    public String getIeltsReading() { return ieltsReading; }
    public void setIeltsReading(String ieltsReading) { this.ieltsReading = ieltsReading; }

    public String getKeyLearningPoints() { return keyLearningPoints; }
    public void setKeyLearningPoints(String keyLearningPoints) { this.keyLearningPoints = keyLearningPoints; }

    public String getPracticeActivities() { return practiceActivities; }
    public void setPracticeActivities(String practiceActivities) { this.practiceActivities = practiceActivities; }

    public String getRecommendedFocus() { return recommendedFocus; }
    public void setRecommendedFocus(String recommendedFocus) { this.recommendedFocus = recommendedFocus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "LearningDaySummary{" +
                "date=" + date +
                ", vocabularyWords='" + vocabularyWords + '\'' +
                ", japaneseLesson='" + japaneseLesson + '\'' +
                ", toeicPractice='" + toeicPractice + '\'' +
                ", ieltsReading='" + ieltsReading + '\'' +
                ", keyLearningPoints='" + keyLearningPoints + '\'' +
                ", practiceActivities='" + practiceActivities + '\'' +
                ", recommendedFocus='" + recommendedFocus + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
