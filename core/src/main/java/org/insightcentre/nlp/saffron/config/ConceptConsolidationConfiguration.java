package org.insightcentre.nlp.saffron.config;

/**
 * The configuration for Concept Consolidation
 * 
 * @author Bianca Pereira
 *
 */
public class ConceptConsolidationConfiguration {

	/** Algorithm for concept consolidation **/
	public Algorithm algorithm = Algorithm.simple;
	
	/** Enumeration of the algorithms available for use:
	 * 
	 *  simple - Each term becomes a separate concept. Assumes 
	 *     monosemy and no synonymy.
	 **/
    public enum Algorithm { simple };
}
