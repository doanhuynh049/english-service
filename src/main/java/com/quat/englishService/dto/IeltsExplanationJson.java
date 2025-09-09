package com.quat.englishService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class IeltsExplanationJson {
    
    @JsonProperty("mainIdea")
    private String mainIdea;
    
    @JsonProperty("paragraphSummary")
    private List<ParagraphSummary> paragraphSummary;
    
    @JsonProperty("keyVocabulary")
    private List<VocabularyItem> keyVocabulary;
    
    @JsonProperty("implicitVsExplicit")
    private ImplicitVsExplicit implicitVsExplicit;
    
    @JsonProperty("questionStrategy")
    private List<String> questionStrategy;
    
    // Constructors
    public IeltsExplanationJson() {}
    
    public IeltsExplanationJson(String mainIdea, List<ParagraphSummary> paragraphSummary, 
                               List<VocabularyItem> keyVocabulary, ImplicitVsExplicit implicitVsExplicit,
                               List<String> questionStrategy) {
        this.mainIdea = mainIdea;
        this.paragraphSummary = paragraphSummary;
        this.keyVocabulary = keyVocabulary;
        this.implicitVsExplicit = implicitVsExplicit;
        this.questionStrategy = questionStrategy;
    }
    
    // Getters and Setters
    public String getMainIdea() {
        return mainIdea;
    }
    
    public void setMainIdea(String mainIdea) {
        this.mainIdea = mainIdea;
    }
    
    public List<ParagraphSummary> getParagraphSummary() {
        return paragraphSummary;
    }
    
    public void setParagraphSummary(List<ParagraphSummary> paragraphSummary) {
        this.paragraphSummary = paragraphSummary;
    }
    
    public List<VocabularyItem> getKeyVocabulary() {
        return keyVocabulary;
    }
    
    public void setKeyVocabulary(List<VocabularyItem> keyVocabulary) {
        this.keyVocabulary = keyVocabulary;
    }
    
    public ImplicitVsExplicit getImplicitVsExplicit() {
        return implicitVsExplicit;
    }
    
    public void setImplicitVsExplicit(ImplicitVsExplicit implicitVsExplicit) {
        this.implicitVsExplicit = implicitVsExplicit;
    }
    
    public List<String> getQuestionStrategy() {
        return questionStrategy;
    }
    
    public void setQuestionStrategy(List<String> questionStrategy) {
        this.questionStrategy = questionStrategy;
    }
    
    // Nested classes
    public static class ParagraphSummary {
        @JsonProperty("paragraph")
        private int paragraph;
        
        @JsonProperty("summary")
        private String summary;
        
        public ParagraphSummary() {}
        
        public ParagraphSummary(int paragraph, String summary) {
            this.paragraph = paragraph;
            this.summary = summary;
        }
        
        public int getParagraph() {
            return paragraph;
        }
        
        public void setParagraph(int paragraph) {
            this.paragraph = paragraph;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
    }
    
    public static class VocabularyItem {
        @JsonProperty("term")
        private String term;
        
        @JsonProperty("definition")
        private String definition;
        
        @JsonProperty("example")
        private String example;
        
        @JsonProperty("synonyms")
        private List<String> synonyms;
        
        public VocabularyItem() {}
        
        public VocabularyItem(String term, String definition, String example, List<String> synonyms) {
            this.term = term;
            this.definition = definition;
            this.example = example;
            this.synonyms = synonyms;
        }
        
        public String getTerm() {
            return term;
        }
        
        public void setTerm(String term) {
            this.term = term;
        }
        
        public String getDefinition() {
            return definition;
        }
        
        public void setDefinition(String definition) {
            this.definition = definition;
        }
        
        public String getExample() {
            return example;
        }
        
        public void setExample(String example) {
            this.example = example;
        }
        
        public List<String> getSynonyms() {
            return synonyms;
        }
        
        public void setSynonyms(List<String> synonyms) {
            this.synonyms = synonyms;
        }
    }
    
    public static class ImplicitVsExplicit {
        @JsonProperty("explicit")
        private List<String> explicit;
        
        @JsonProperty("implicit")
        private List<String> implicit;
        
        public ImplicitVsExplicit() {}
        
        public ImplicitVsExplicit(List<String> explicit, List<String> implicit) {
            this.explicit = explicit;
            this.implicit = implicit;
        }
        
        public List<String> getExplicit() {
            return explicit;
        }
        
        public void setExplicit(List<String> explicit) {
            this.explicit = explicit;
        }
        
        public List<String> getImplicit() {
            return implicit;
        }
        
        public void setImplicit(List<String> implicit) {
            this.implicit = implicit;
        }
    }
}
