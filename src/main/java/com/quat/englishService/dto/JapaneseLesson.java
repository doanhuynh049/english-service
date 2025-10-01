package com.quat.englishService.dto;

import java.time.LocalDateTime;

/**
 * DTO for Japanese lesson data
 * Contains lesson information from Excel and AI-generated content
 */
public class JapaneseLesson {
    private String topic;
    private String description;
    private int day;
    private String lessonTitle;
    private String contentHtml;
    private String[] examples;
    private String[] practiceTasks;
    private LocalDateTime createdAt;

    // Constructors
    public JapaneseLesson() {
        this.createdAt = LocalDateTime.now();
    }

    public JapaneseLesson(String topic, String description, int day) {
        this.topic = topic;
        this.description = description;
        this.day = day;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String[] getExamples() { return examples; }
    public void setExamples(String[] examples) { this.examples = examples; }

    public String[] getPracticeTasks() { return practiceTasks; }
    public void setPracticeTasks(String[] practiceTasks) { this.practiceTasks = practiceTasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JapaneseLesson{" +
                "topic='" + topic + '\'' +
                ", description='" + description + '\'' +
                ", day=" + day +
                ", lessonTitle='" + lessonTitle + '\'' +
                '}';
    }
}
