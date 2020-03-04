package org.insightcentre.nlp.saffron.taxonomy.metrics;

import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.search.Solution;

/**
 * An interface for scorers for solutions. Each instance can only provide the
 * score for one change
 *
 * @author John McCrae
 */
public interface HierarchicalScore extends Score<TaxoLink> {

	@Override
	HierarchicalScore next(TaxoLink link, Solution soln); 
	
}
