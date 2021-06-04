package org.insightcentre.nlp.saffron.config;

import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * Configuration for the Knowledge Graph Extraction step
 * 
 * @author Bianca Pereira
 *
 */
public class KnowledgeGraphExtractionConfiguration {
    public static final double DEFAULT_SYNONYMY_THRESHOLD = 0.5;
	public static final double DEFAULT_MERONYMY_THRESHOLD = 0.25;
	public static final double DEFAULT_GENERIC_THRESHOLD = 0.0;

	public SaffronPath kerasModelFile = null;
	
	public SaffronPath bertModelFile = null;
	
	public int numberOfRelations = 5; //Options available "3" or "5"

	public double synonymyThreshold = DEFAULT_SYNONYMY_THRESHOLD;
	
	public double meronomyThreshold = DEFAULT_MERONYMY_THRESHOLD;

	public double genericThreshold = DEFAULT_GENERIC_THRESHOLD;

	public boolean enableSynonymyNormalisation = false;
}
