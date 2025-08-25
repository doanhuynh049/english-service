package com.quat.englishService.model;

import java.time.LocalDateTime;

public class VocabularyWord {
    private String word;
    private String explanation;
    private LocalDateTime createdAt;

    public VocabularyWord() {
    }

    public VocabularyWord(String word, String explanation) {
        this.word = word;
        this.explanation = explanation;
        this.createdAt = LocalDateTime.now();
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
