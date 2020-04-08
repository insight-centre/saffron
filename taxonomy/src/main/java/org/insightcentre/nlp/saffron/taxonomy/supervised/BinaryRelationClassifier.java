package org.insightcentre.nlp.saffron.taxonomy.supervised;

/**
 * 
 * Classifier that verifies the probability of a relationship
 * between a source node and a target node.
 * 
 * @author Bianca Pereira
 *
 * @param <T> The type of node being considered
 */
public interface BinaryRelationClassifier<T> {

	/**
	 * Predict the relationship between two nodes
	 * 
	 * @param source The source node
	 * @param target The target node
	 * 
	 * @return the probability of a directed relationship between 
	 * source and target
	 */
	public double predict(T source, T target);
}
