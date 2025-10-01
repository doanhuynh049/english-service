package com.quat.englishService.dto;

import java.time.LocalDateTime;

/**
 * DTO for learning summary data
 * Used to store daily learning knowledge in Excel format
 */
public class LearningSummary {
    private int day;
    private String phase;
    private String topic;
    private String keyKnowledge;
    private String examples;
    private String notes;
    private String serviceType; // "Japanese", "TOEIC", "Vocabulary", etc.
    private LocalDateTime createdAt;

    // Constructors
    public LearningSummary() {
        this.createdAt = LocalDateTime.now();
    }

    public LearningSummary(int day, String phase, String topic, String serviceType) {
        this.day = day;
        this.phase = phase;
        this.topic = topic;
        this.serviceType = serviceType;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getKeyKnowledge() { return keyKnowledge; }
    public void setKeyKnowledge(String keyKnowledge) { this.keyKnowledge = keyKnowledge; }

    public String getExamples() { return examples; }
    public void setExamples(String examples) { this.examples = examples; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "LearningSummary{" +
                "day=" + day +
                ", phase='" + phase + '\'' +
                ", topic='" + topic + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
