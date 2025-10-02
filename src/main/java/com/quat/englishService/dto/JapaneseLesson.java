package com.quat.englishService.dto;

import java.time.LocalDateTime;

/**
 * DTO for Japanese lesson data
 * Contains lesson information from Excel and AI-generated content
 */
public class JapaneseLesson {
    private int day;
    private String phase;
    private String topic;
    private String description;
    private String status;
    private String lessonTitle;
    private String contentHtml;
    private String[] examples;
    private String[] practiceTasks;
    private ListeningPractice listeningPractice;
    private LocalDateTime createdAt;

    // Constructors
    public JapaneseLesson() {
        this.createdAt = LocalDateTime.now();
    }

    public JapaneseLesson(int day, String phase, String topic, String description) {
        this.day = day;
        this.phase = phase;
        this.topic = topic;
        this.description = description;
        this.status = "Open";
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String[] getExamples() { return examples; }
    public void setExamples(String[] examples) { this.examples = examples; }

    public String[] getPracticeTasks() { return practiceTasks; }
    public void setPracticeTasks(String[] practiceTasks) { this.practiceTasks = practiceTasks; }

    public ListeningPractice getListeningPractice() { return listeningPractice; }
    public void setListeningPractice(ListeningPractice listeningPractice) { this.listeningPractice = listeningPractice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JapaneseLesson{" +
                "day=" + day +
                ", phase='" + phase + '\'' +
                ", topic='" + topic + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", lessonTitle='" + lessonTitle + '\'' +
                '}';
    }
}
