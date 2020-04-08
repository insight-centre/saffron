package org.insightcentre.nlp.saffron.concept.consolidation;

import java.util.List;

import org.insightcentre.nlp.saffron.data.Concept;
import org.insightcentre.nlp.saffron.data.Term;

/**
 * Interface for all algorithms used for Concept Consolidation
 * 
 * @author Bianca Pereira
 *
 */
public interface ConceptConsolidation {

	public List<Concept> consolidate(List<Term> terms);
}
