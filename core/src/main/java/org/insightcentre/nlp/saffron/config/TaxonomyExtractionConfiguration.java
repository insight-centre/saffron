package org.insightcentre.nlp.saffron.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * Configuration for supervised taxonomy extraction
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TaxonomyExtractionConfiguration {
    /** The number of negative samples to generate when training */
    public double negSampling = 5;
    @JsonProperty("class")
    /** The class of the Weka classifier */
    public String _class = "weka.classifiers.functions.SMOreg";
    /** Where to put the training data as an ARFF (for debugging with Weka),
     *  or null to not output training data.
     */
    public SaffronPath arffFile = null;
    /** The file containing the GloVe vectors or null if not used */
    public SaffronPath gloveFile = null;
    /** The file containing the SVD matrix or null if not used */
    public SaffronPath svdAveFile = null;
    /** the file containing the SVD matrix or null if not used */
    public SaffronPath svdMinMaxFile = null;
    /** the file containin the Hypernyms */
    public SaffronPath hypernyms = null;
    /** The WEKA binary model */
    public SaffronPath modelFile = null;
    /** The feature selection or null for all features*/
    public FeatureSelection features = null;
    /** The mode (algorithm) to use to derive the tree */
    public Mode mode = Mode.greedy;
    /** A limit on the number of children to be added under one node (does not work in MST mode) */
    public int maxChildren = Integer.MAX_VALUE;

    /**
     * Verify this model
     * @return null is this model is valid, otherwise the error message
     */
    public String verify() {
        if (negSampling <= 0) {
            return "Bad Neg Sampling value";
        }
        if (modelFile == null) {
            return "Model File is required";
        }
        return null;
    }
    /** Minimum threshold to accept for similarity */
    public double simThreshold = 0.0;

    public static class FeatureSelection {
        /** Use the inclusion feature */
        public boolean inclusion = false;
        /** Use the overlap feature */
        public boolean overlap = false;
        /** Use the longest common subsequence feature */
        public boolean lcs = false;
        /** Use the SVD Average Vector Similarity feature */
        public boolean svdSimAve = false;
        /** Use the SVD Minimum-Maximum Vector Similarity feature */
        public boolean svdSimMinMax = false;
        /** Use the Topic Difference feature */
        public boolean topicDiff = false;
        /** Use the relative frequency feature */
        public boolean relFreq = false;
        /** Use direct wordnet */
        public boolean wnDirect = false;
        /** Use indirect wordnet */
        public boolean wnIndirect = false;
    }
    
    public enum Mode {
        greedy, mst, headAndBag
    }

    @Override
    public String toString() {
        return "TaxonomyExtractionConfiguration{" + "negSampling=" + negSampling + ", _class=" + _class + ", arffFile=" + arffFile + ", gloveFile=" + gloveFile + ", svdAveFile=" + svdAveFile + ", svdMinMaxFile=" + svdMinMaxFile + ", modelFile=" + modelFile + ", features=" + features + ", mode=" + mode + ", maxChildren=" + maxChildren + ", simThreshold=" + simThreshold + '}';
    }
    
    
}
