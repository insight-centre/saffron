package org.insightcentre.nlp.saffron.taxonomy.supervised;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import weka.classifiers.functions.SMOreg;

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
    public String _class = SMOreg.class.getName();
    /** Where to put the training data as an ARFF (for debugging with Weka),
     *  or null to not output training data.
     */
    public File arffFile = null;
    /** The file containing the GloVe vectors or null if not used */
    public File gloveFile = null;
    /** The file containing the SVD matrix or null if not used */
    public File svdAveFile = null;
    /** the file containing the SVD matrix or null if not used */
    public File svdMinMaxFile = null;
    /** The WEKA binary model */
    public File modelFile = null;
    /** The feature selection or null for all features*/
    public FeatureSelection features = null;
    /** The mode (algorithm) to use to derive the tree */
    public Mode mode = Mode.greedy;

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
    }
    
    public enum Mode {
        greedy, mst
    }
}
