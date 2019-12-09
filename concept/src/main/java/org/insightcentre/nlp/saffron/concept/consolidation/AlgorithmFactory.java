package org.insightcentre.nlp.saffron.concept.consolidation;

import org.insightcentre.nlp.saffron.config.ConceptConsolidationConfiguration;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;

/**
 * Creates {@link ConceptConsolidation} algorithms
 * 
 * @author Bianca Pereira
 *
 */
public abstract class AlgorithmFactory {

	public static ConceptConsolidation create(ConceptConsolidationConfiguration.Algorithm algorithm){
		switch(algorithm) {
			case simple:
				return new Simple();
			default:
				throw new InvalidValueException("The concept consolidation algorithm is invalid!");
		}
	}
	
	public static ConceptConsolidation create(ConceptConsolidationConfiguration config){
		return create(config.algorithm);
	}
}
