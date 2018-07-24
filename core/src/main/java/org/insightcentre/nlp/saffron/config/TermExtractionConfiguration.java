package org.insightcentre.nlp.saffron.config;

import java.util.List;
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
    /** The Weighting Method to use */
    public WeightingMethod method = WeightingMethod.one;
    /** The features to use */
    public List<Feature> features = java.util.Arrays.asList(Feature.novelTopicModel, Feature.cValue, Feature.relevance, Feature.linkProbability, Feature.domainCoherence, Feature.keyConceptRelatedness);
    /** The corpus  to use (path to this file) */
    public String corpus;
    /** The info measure to use */
    public String infoMeasure;
    /** The path to the word2vec model */
    public String w2vmodelPath;
    /** The base feature to use */
    public Feature baseFeature = Feature.comboBasic;
    /** The number of threads to use */
    public int numThreads;
    /** The path to the part-of-speech tagger's model */
    public SaffronPath posModel;
    /** The path to the tokenizer's model */
    public SaffronPath tokenizerModel;
    
    /** The Weighting method to use */
    public enum WeightingMethod {
        one, voting, puatr
    };
    
    /** The features for topic extraction */
    public enum Feature {
        weirdness, avgTermFreq, residualIdf, totalTfIdf, cValue, basic, comboBasic, postRankDC, relevance, domainCoherence, domainPertinence, novelTopicModel, linkProbability, keyConceptRelatedness
    };
}
