package com.quat.englishService.dto;

import java.time.LocalDateTime;

/**
 * DTO for TOEIC vocabulary words with rich information
 * Used for advanced TOEIC vocabulary targeting score 800+
 */
public class ToeicVocabularyWord {
    private String word;
    private String partOfSpeech;
    private String pronunciation;  // IPA pronunciation
    private String definition;
    private String example;
    private String[] collocations;
    private String vietnameseTranslation;
    private LocalDateTime createdAt;

    // Constructors
    public ToeicVocabularyWord() {
        this.createdAt = LocalDateTime.now();
    }

    public ToeicVocabularyWord(String word, String partOfSpeech, String definition) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.definition = definition;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }

    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public String[] getCollocations() { return collocations; }
    public void setCollocations(String[] collocations) { this.collocations = collocations; }

    public String getVietnameseTranslation() { return vietnameseTranslation; }
    public void setVietnameseTranslation(String vietnameseTranslation) { this.vietnameseTranslation = vietnameseTranslation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ToeicVocabularyWord{" +
                "word='" + word + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", definition='" + definition + '\'' +
                '}';
    }
}
