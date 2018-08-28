package org.insightcentre.nlp.saffron.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * Term extraction configuration
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtractionConfiguration {
    /** Minimum threshold score to extract */
    public double threshold = 0.0;
    /** Maximum number of topics to extract */
    public int maxTopics = 100;
    /** The shortest length of term to consider */
    public int ngramMin = 1;
    /** The longest term to consider */
    public int ngramMax = 4;
    /** The minimum frequency to consider */
    public int minTermFreq = 2;
    /** The maximum number of documents to consider */
    public int maxDocs = Integer.MAX_VALUE;
    /** The Weighting Method to use */
    public WeightingMethod method = WeightingMethod.one;
    /** The features to use */
    public List<Feature> features = java.util.Arrays.asList(Feature.comboBasic, Feature.weirdness, Feature.totalTfIdf, Feature.cValue, Feature.residualIdf);
    /** The corpus  to use (path to this file) */
    public SaffronPath corpus;
    /** The info measure to use */
    //public String infoMeasure;
    /** The path to the word2vec model */
    //public String w2vmodelPath;
    /** The base feature to use */
    public Feature baseFeature = Feature.comboBasic;
    /** The number of threads to use */
    public int numThreads;
    /** The path to the part-of-speech tagger's model */
    public SaffronPath posModel;
    /** The path to the tokenizer's model */
    public SaffronPath tokenizerModel;
    /** The path to the lemmatizer's model */
    public SaffronPath lemmatizerModel;
    /** The path to the list of stop words (one per line) */
    public SaffronPath stopWords;
    /** The set of tags allowed in non-final position in a noun phrase */
    public Set<String> preceedingTokens = new HashSet<>(Arrays.asList("NN", "NNS", "JJ", "NNP"));
    /** The set of tags allowed in non-final position, but not completing */
    public Set<String> middleTokens = new HashSet<>(Arrays.asList("IN"));
    /** The set of final tags allows in a noun phrase */
    public Set<String> headTokens = new HashSet<>(Arrays.asList("NN", "NNS", "CD"));
    /** The position of the head of a noun phrase (true=final) */
    public boolean headTokenFinal = true;
    
    /** The Weighting method to use */
    public enum WeightingMethod {
        one, voting, puatr
    };
    
    /** The features for topic extraction */
    public enum Feature {
        weirdness, avgTermFreq, residualIdf, totalTfIdf, cValue, basic, comboBasic, postRankDC, relevance, /*domainCoherence,*/ /*domainPertinence,*/ novelTopicModel, /*linkProbability, keyConceptRelatedness*/
    };
    
    /** The default English list of stopwords */
    public static final String[] ENGLISH_STOPWORDS = new String[]{
        "i",
        "me",
        "my",
        "myself",
        "we",
        "our",
        "ours",
        "ourselves",
        "you",
        "your",
        "yours",
        "yourself",
        "yourselves",
        "he",
        "him",
        "his",
        "himself",
        "she",
        "her",
        "hers",
        "herself",
        "it",
        "its",
        "itself",
        "they",
        "them",
        "their",
        "theirs",
        "themselves",
        "what",
        "which",
        "who",
        "whom",
        "this",
        "that",
        "these",
        "those",
        "am",
        "is",
        "are",
        "was",
        "were",
        "be",
        "been",
        "being",
        "have",
        "has",
        "had",
        "having",
        "do",
        "does",
        "did",
        "doing",
        "a",
        "an",
        "the",
        "and",
        "but",
        "if",
        "or",
        "because",
        "as",
        "until",
        "while",
        "of",
        "at",
        "by",
        "for",
        "with",
        "about",
        "against",
        "between",
        "into",
        "through",
        "during",
        "before",
        "after",
        "above",
        "below",
        "to",
        "from",
        "up",
        "down",
        "in",
        "out",
        "on",
        "off",
        "over",
        "under",
        "again",
        "further",
        "then",
        "once",
        "here",
        "there",
        "when",
        "where",
        "why",
        "how",
        "all",
        "any",
        "both",
        "each",
        "few",
        "more",
        "most",
        "other",
        "some",
        "such",
        "no",
        "nor",
        "not",
        "only",
        "own",
        "same",
        "so",
        "than",
        "too",
        "very",
        "s",
        "t",
        "can",
        "will",
        "just",
        "don",
        "should",
        "now",
        "d",
        "ll",
        "m",
        "o",
        "re",
        "ve",
        "y",
        "ain",
        "aren",
        "couldn",
        "didn",
        "doesn",
        "hadn",
        "hasn",
        "haven",
        "isn",
        "ma",
        "mightn",
        "mustn",
        "needn",
        "shan",
        "shouldn",
        "wasn",
        "weren",
        "won",
        "wouldn"
    };

}
