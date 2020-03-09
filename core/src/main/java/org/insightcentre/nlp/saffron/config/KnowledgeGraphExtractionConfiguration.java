package org.insightcentre.nlp.saffron.config;

/**
 * Configuration for the Knowledge Graph Extraction step
 * 
 * @author Bianca Pereira
 *
 */
public class KnowledgeGraphExtractionConfiguration {

	public String kerasModelFile = null;
	
	public String bertModelFile = null;
	
	public double synonymyThreshold = 0.5;
	
	public double meronomyThreshold = 0.25;

	public boolean enableSynonymyNormalisation = false;
}
