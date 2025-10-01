package com.quat.englishService.dto;

import java.time.LocalDateTime;

/**
 * DTO for Japanese vocabulary entries
 * Contains comprehensive vocabulary information for learning tracking
 */
public class JapaneseVocabulary {
    private int id;
    private String wordKanji;
    private String wordKana;
    private String romaji;
    private String partOfSpeech;
    private String definition;
    private String vietnamese;
    private String exampleSentenceJp;
    private String exampleSentenceEn;
    private String collocations;
    private String synonyms;
    private String confusableWords;
    private String notes;
    private int lessonDay;
    private String lessonTopic;
    private LocalDateTime createdAt;

    // Constructors
    public JapaneseVocabulary() {
        this.createdAt = LocalDateTime.now();
    }

    public JapaneseVocabulary(String wordKanji, String wordKana, String romaji, 
                             String partOfSpeech, String definition, String vietnamese) {
        this();
        this.wordKanji = wordKanji;
        this.wordKana = wordKana;
        this.romaji = romaji;
        this.partOfSpeech = partOfSpeech;
        this.definition = definition;
        this.vietnamese = vietnamese;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getWordKanji() { return wordKanji; }
    public void setWordKanji(String wordKanji) { this.wordKanji = wordKanji; }

    public String getWordKana() { return wordKana; }
    public void setWordKana(String wordKana) { this.wordKana = wordKana; }

    public String getRomaji() { return romaji; }
    public void setRomaji(String romaji) { this.romaji = romaji; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getVietnamese() { return vietnamese; }
    public void setVietnamese(String vietnamese) { this.vietnamese = vietnamese; }

    public String getExampleSentenceJp() { return exampleSentenceJp; }
    public void setExampleSentenceJp(String exampleSentenceJp) { this.exampleSentenceJp = exampleSentenceJp; }

    public String getExampleSentenceEn() { return exampleSentenceEn; }
    public void setExampleSentenceEn(String exampleSentenceEn) { this.exampleSentenceEn = exampleSentenceEn; }

    public String getCollocations() { return collocations; }
    public void setCollocations(String collocations) { this.collocations = collocations; }

    public String getSynonyms() { return synonyms; }
    public void setSynonyms(String synonyms) { this.synonyms = synonyms; }

    public String getConfusableWords() { return confusableWords; }
    public void setConfusableWords(String confusableWords) { this.confusableWords = confusableWords; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getLessonDay() { return lessonDay; }
    public void setLessonDay(int lessonDay) { this.lessonDay = lessonDay; }

    public String getLessonTopic() { return lessonTopic; }
    public void setLessonTopic(String lessonTopic) { this.lessonTopic = lessonTopic; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "JapaneseVocabulary{" +
                "id=" + id +
                ", wordKanji='" + wordKanji + '\'' +
                ", wordKana='" + wordKana + '\'' +
                ", romaji='" + romaji + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", definition='" + definition + '\'' +
                ", lessonDay=" + lessonDay +
                ", lessonTopic='" + lessonTopic + '\'' +
                '}';
    }
}
