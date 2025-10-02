package com.quat.englishService.dto;

import java.util.List;

/**
 * DTO for Japanese listening practice data
 */
public class ListeningPractice {
    private List<Word> words;
    private Paragraph listeningParagraph;
    
    // Constructors
    public ListeningPractice() {}
    
    public ListeningPractice(List<Word> words, Paragraph listeningParagraph) {
        this.words = words;
        this.listeningParagraph = listeningParagraph;
    }
    
    // Getters and Setters
    public List<Word> getWords() { return words; }
    public void setWords(List<Word> words) { this.words = words; }
    
    public Paragraph getListeningParagraph() { return listeningParagraph; }
    public void setListeningParagraph(Paragraph listeningParagraph) { this.listeningParagraph = listeningParagraph; }
    
    /**
     * Word class for individual vocabulary items
     */
    public static class Word {
        private String japanese;
        private String romaji;
        private String english;
        private String vietnamese;
        private String exampleSentence;
        private String exampleRomaji;
        private String exampleEnglish;
        
        // Audio URLs (populated after audio generation)
        private String wordAudioUrl;
        private String exampleAudioUrl;
        
        // Constructors
        public Word() {}
        
        public Word(String japanese, String romaji, String english, String vietnamese,
                   String exampleSentence, String exampleRomaji, String exampleEnglish) {
            this.japanese = japanese;
            this.romaji = romaji;
            this.english = english;
            this.vietnamese = vietnamese;
            this.exampleSentence = exampleSentence;
            this.exampleRomaji = exampleRomaji;
            this.exampleEnglish = exampleEnglish;
        }
        
        // Getters and Setters
        public String getJapanese() { return japanese; }
        public void setJapanese(String japanese) { this.japanese = japanese; }
        
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }
        
        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }
        
        public String getVietnamese() { return vietnamese; }
        public void setVietnamese(String vietnamese) { this.vietnamese = vietnamese; }
        
        public String getExampleSentence() { return exampleSentence; }
        public void setExampleSentence(String exampleSentence) { this.exampleSentence = exampleSentence; }
        
        public String getExampleRomaji() { return exampleRomaji; }
        public void setExampleRomaji(String exampleRomaji) { this.exampleRomaji = exampleRomaji; }
        
        public String getExampleEnglish() { return exampleEnglish; }
        public void setExampleEnglish(String exampleEnglish) { this.exampleEnglish = exampleEnglish; }
        
        public String getWordAudioUrl() { return wordAudioUrl; }
        public void setWordAudioUrl(String wordAudioUrl) { this.wordAudioUrl = wordAudioUrl; }
        
        public String getExampleAudioUrl() { return exampleAudioUrl; }
        public void setExampleAudioUrl(String exampleAudioUrl) { this.exampleAudioUrl = exampleAudioUrl; }
    }
    
    /**
     * Paragraph class for listening practice text
     */
    public static class Paragraph {
        private String japanese;
        private String romaji;
        private String english;
        private String audioUrl; // Audio URL for the paragraph
        
        // Constructors
        public Paragraph() {}
        
        public Paragraph(String japanese, String romaji, String english) {
            this.japanese = japanese;
            this.romaji = romaji;
            this.english = english;
        }
        
        // Getters and Setters
        public String getJapanese() { return japanese; }
        public void setJapanese(String japanese) { this.japanese = japanese; }
        
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }
        
        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }
        
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    }
}
