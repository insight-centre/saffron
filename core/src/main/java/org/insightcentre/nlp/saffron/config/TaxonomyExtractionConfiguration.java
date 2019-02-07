package org.insightcentre.nlp.saffron.config;

import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * Configuration for supervised taxonomy extraction
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TaxonomyExtractionConfiguration {

    /**
     * The number of negative samples to generate when training
     */
    public double negSampling = 5;
    /**
     * The configuration of the features
     */
    public FeatureConfiguration features;
    /**
     * The model to be trained
     */
    public SaffronPath modelFile = null;
    /**
     * The mode (algorithm) to use to derive the tree
     */
    public TaxonomySearchConfiguration search = new TaxonomySearchConfiguration();
    /**
     * A limit on the number of children to be added under one node (does not
     * work in MST mode)
     */
    public int maxChildren = Integer.MAX_VALUE;

    /**
     * Verify this model
     *
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
    /**
     * Minimum threshold to accept for similarity
     */
    public double simThreshold = 0.0;

    /** The features to be used by this taxonomy extractor */
    public static class FeatureSelection {

        /**
         * Use the inclusion feature
         */
        public boolean inclusion = false;
        /**
         * Use the overlap feature
         */
        public boolean overlap = false;
        /**
         * Use the longest common subsequence feature
         */
        public boolean lcs = false;
        /**
         * Use the SVD Average Vector Similarity feature
         */
        public boolean svdSimAve = false;
        /**
         * Use the SVD Minimum-Maximum Vector Similarity feature
         */
        public boolean svdSimMinMax = false;
        /**
         * Use the Topic Difference feature
         */
        public boolean topicDiff = false;
        /**
         * Use the relative frequency feature
         */
        public boolean relFreq = false;
        /**
         * Use direct wordnet
         */
        public boolean wnDirect = false;
        /**
         * Use indirect wordnet
         */
        public boolean wnIndirect = false;
    }

    @Override
    public String toString() {
        return "TaxonomyExtractionConfiguration{" + "negSampling=" + negSampling + ", features=" + features + ", modelFile=" + modelFile + ", search=" + search + ", maxChildren=" + maxChildren + ", simThreshold=" + simThreshold + '}';
    }

    /** Configuration of the feature extraction */
    public static class FeatureConfiguration {

        /**
         * The file containing the GloVe vectors or null if not used
         */
        public SaffronPath gloveFile = null;
        /**
         * the file containin the Hypernyms
         */
        public SaffronPath hypernyms = null;
        /**
         * The feature selection or null for all features
         */
        public FeatureSelection featureSelection = null;

        @Override
        public String toString() {
            return "FeatureConfiguration{" + "gloveFile=" + gloveFile + ", hypernyms=" + hypernyms + ", features=" + featureSelection + '}';
        }

    }
}
