//package org.insightcentre.nlp.saffron.topic;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// *
// * @author John McCrae <john@mccr.ae>
// */
//public class Topic {
//    private List<MorphologicalVariation> morphologicalVariations = new ArrayList<>();
//    private String rootSequence;
//    private int numberOfTokens;
//
//    public List<MorphologicalVariation> getMorphologicalVariations() {
//        return morphologicalVariations;
//    }
//
//    public void setMorphologicalVariations(List<MorphologicalVariation> morphologicalVariations) {
//        this.morphologicalVariations = morphologicalVariations;
//    }
//
//    
//
//    public String getRootSequence() {
//        return rootSequence;
//    }
//
//    public void setRootSequence(String rootSequence) {
//        this.rootSequence = rootSequence;
//    }
//
//    public int getNumberOfTokens() {
//        return numberOfTokens;
//    }
//
//    public void setNumberOfTokens(int numberOfTokens) {
//        this.numberOfTokens = numberOfTokens;
//    }
//
//    void addMorphologicalVariation(MorphologicalVariation mv) {
//        this.morphologicalVariations.add(mv);
//    }
//
//    public static class MorphologicalVariation {
//        private String termString;
//        private int extractedTermOccurrences;
//        private String pattern;
//        private String expandedAcronym;
//        private String acronym;
//
//        public String getTermString() {
//            return termString;
//        }
//
//        public void setTermString(String termString) {
//            this.termString = termString;
//        }
//
//        public int getExtractedTermOccurrences() {
//            return extractedTermOccurrences;
//        }
//
//        public void setExtractedTermOccurrences(int extractedTermOccurrences) {
//            this.extractedTermOccurrences = extractedTermOccurrences;
//        }
//
//        public String getPattern() {
//            return pattern;
//        }
//
//        public void setPattern(String pattern) {
//            this.pattern = pattern;
//        }
//
//        public String getExpandedAcronym() {
//            return expandedAcronym;
//        }
//
//        public void setExpandedAcronym(String expandedAcronym) {
//            this.expandedAcronym = expandedAcronym;
//        }
//
//        public String getAcronym() {
//            return acronym;
//        }
//
//        public void setAcronym(String acronym) {
//            this.acronym = acronym;
//        }
//    }
//
//}
