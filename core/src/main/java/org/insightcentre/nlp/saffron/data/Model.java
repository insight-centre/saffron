package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.insightcentre.nlp.saffron.config.TaxonomyExtractionConfiguration;

/**
 * A trained Saffron model
 * 
 * @author John McCrae
 */
public class Model {
    /**
     * The average SVD matrix
     */
    public double[][] svdAve;
    /**
     * The min-max SVD matrix
     */
    public double[][] svdMinMax;
    /**
     * The classifier data
     */
    public String classifierData;
    /**
     * The configured features used in this run
     */
    public TaxonomyExtractionConfiguration.FeatureConfiguration features;
    
}
