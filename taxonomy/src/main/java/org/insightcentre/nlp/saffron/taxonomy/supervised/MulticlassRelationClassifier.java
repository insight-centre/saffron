package org.insightcentre.nlp.saffron.taxonomy.supervised;

import java.util.Map;

import org.insightcentre.nlp.saffron.taxonomy.search.TypedLink;

/**
 * Classifier that verifies the probability of multiple relationship
 * types between a source node and a target node.
 * 
 * @author Bianca Pereira
 *
 * @param <T> The type of node being considered
 */
public interface MulticlassRelationClassifier<T> {

	/**
	 * Predict the relationship between two nodes
	 * 
	 * @param source The source node
	 * @param target The target node
	 * 
	 * @return the probability of multiple directed types of
	 * relationships between source and target. 
	 * key: relation label, value: probability
	 */
	public Map<TypedLink.Type, Double> predict(T source, T target);
	
}
