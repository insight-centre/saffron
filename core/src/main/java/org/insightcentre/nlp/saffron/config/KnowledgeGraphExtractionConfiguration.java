package org.insightcentre.nlp.saffron.config;

import org.insightcentre.nlp.saffron.data.SaffronPath;

/**
 * Configuration for the Knowledge Graph Extraction step
 * 
 * @author Bianca Pereira
 *
 */
public class KnowledgeGraphExtractionConfiguration {

	public SaffronPath kerasModelFile = null;
	
	public SaffronPath bertModelFile = null;
	
	public double synonymyThreshold = 0.5;
	
	public double meronomyThreshold = 0.25;

	public boolean enableSynonymyNormalisation = false;
}
