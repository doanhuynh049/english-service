package com.quat.englishService.dto;

public class ParsedVocabularyWord {
    private String word;
    private String pronunciation;
    private String partOfSpeech;
    private String simpleDefinition;
    private String advancedDefinition;
    private String[] exampleSentences;
    private String collocations;
    private String synonyms;
    private String antonyms;
    private String confusedWords;
    private String wordFamily;
    private String vietnameseTranslation;
    private String rawExplanation;

    // Audio URL fields
    private String pronunciationAudioUrl;
    private String exampleAudioUrl;
    private String pronunciationAudioPath;
    private String exampleAudioPath;

    // Constructors
    public ParsedVocabularyWord() {}

    public ParsedVocabularyWord(String word, String rawExplanation) {
        this.word = word;
        this.rawExplanation = rawExplanation;
    }

    // Getters and Setters
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPronunciation() { return pronunciation; }
    public void setPronunciation(String pronunciation) { this.pronunciation = pronunciation; }

    public String getPartOfSpeech() { return partOfSpeech; }
    public void setPartOfSpeech(String partOfSpeech) { this.partOfSpeech = partOfSpeech; }

    public String getSimpleDefinition() { return simpleDefinition; }
    public void setSimpleDefinition(String simpleDefinition) { this.simpleDefinition = simpleDefinition; }

    public String getAdvancedDefinition() { return advancedDefinition; }
    public void setAdvancedDefinition(String advancedDefinition) { this.advancedDefinition = advancedDefinition; }

    public String[] getExampleSentences() { return exampleSentences; }
    public void setExampleSentences(String[] exampleSentences) { this.exampleSentences = exampleSentences; }

    public String getCollocations() { return collocations; }
    public void setCollocations(String collocations) { this.collocations = collocations; }

    public String getSynonyms() { return synonyms; }
    public void setSynonyms(String synonyms) { this.synonyms = synonyms; }

    public String getAntonyms() { return antonyms; }
    public void setAntonyms(String antonyms) { this.antonyms = antonyms; }

    public String getConfusedWords() { return confusedWords; }
    public void setConfusedWords(String confusedWords) { this.confusedWords = confusedWords; }

    public String getWordFamily() { return wordFamily; }
    public void setWordFamily(String wordFamily) { this.wordFamily = wordFamily; }

    public String getVietnameseTranslation() { return vietnameseTranslation; }
    public void setVietnameseTranslation(String vietnameseTranslation) { this.vietnameseTranslation = vietnameseTranslation; }

    public String getRawExplanation() { return rawExplanation; }
    public void setRawExplanation(String rawExplanation) { this.rawExplanation = rawExplanation; }

    // Audio getters and setters
    public String getPronunciationAudioUrl() { return pronunciationAudioUrl; }
    public void setPronunciationAudioUrl(String pronunciationAudioUrl) { this.pronunciationAudioUrl = pronunciationAudioUrl; }

    public String getExampleAudioUrl() { return exampleAudioUrl; }
    public void setExampleAudioUrl(String exampleAudioUrl) { this.exampleAudioUrl = exampleAudioUrl; }

    public String getPronunciationAudioPath() { return pronunciationAudioPath; }
    public void setPronunciationAudioPath(String pronunciationAudioPath) { this.pronunciationAudioPath = pronunciationAudioPath; }

    public String getExampleAudioPath() { return exampleAudioPath; }
    public void setExampleAudioPath(String exampleAudioPath) { this.exampleAudioPath = exampleAudioPath; }

    @Override
    public String toString() {
        return "ParsedVocabularyWord{" +
                "word='" + word + '\'' +
                ", pronunciation='" + pronunciation + '\'' +
                ", partOfSpeech='" + partOfSpeech + '\'' +
                ", simpleDefinition='" + simpleDefinition + '\'' +
                ", advancedDefinition='" + advancedDefinition + '\'' +
                ", vietnameseTranslation='" + vietnameseTranslation + '\'' +
                ", pronunciationAudioUrl='" + pronunciationAudioUrl + '\'' +
                ", exampleAudioUrl='" + exampleAudioUrl + '\'' +
                '}';
    }
}
