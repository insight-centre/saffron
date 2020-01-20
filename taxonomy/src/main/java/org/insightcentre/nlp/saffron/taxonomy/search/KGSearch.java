package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.taxonomy.metrics.Score;
import org.insightcentre.nlp.saffron.taxonomy.metrics.ScoreFactory;
import org.insightcentre.nlp.saffron.taxonomy.search.testing.KnowledgeGraph;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

public interface KGSearch {
	public default KnowledgeGraph extractTaxonomy(Map<String, Term> termMap) {
        return extractTaxonomyWithDenialAndAllowanceList(termMap, Collections.EMPTY_SET, Collections.EMPTY_SET);
    }
    
    public KnowledgeGraph extractTaxonomyWithDenialAndAllowanceList(Map<String, Term> termMap, 
            Set<TypedLink> allowanceList, Set<TypedLink> denialList);
    
    
    
    public static KGSearch create(TaxonomySearchConfiguration config, 
            MulticlassRelationClassifier<String> classifier, Set<String> terms) {
        return create(config, classifier, terms, new DefaultSaffronListener());                
    }
    
    //FIXME The classifier should be of terms or concepts, not strings
    public static KGSearch create(TaxonomySearchConfiguration config, 
    		MulticlassRelationClassifier<String> classifier, Set<String> terms, SaffronListener log) {
    	
    	final Score score = ScoreFactory.getInstance(config, config.score, classifier, terms);
    	return new GreedyKG(score);
    }
}
