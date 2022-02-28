package org.insightcentre.nlp.saffron.data;

import java.util.Set;

/**
 * Generalizes over knowledge graphs and taxonomies and provides common features
 * 
 * @author John P. McCrae
 * @param <L> The type of link in the graph
 */
public interface SaffronGraph<L extends TypedLink> {
    Set<L> getRelationsByStatus(Status status);
}
