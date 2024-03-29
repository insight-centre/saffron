package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.KnowledgeGraphExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.TypedLink;
import org.insightcentre.nlp.saffron.taxonomy.metrics.Score;
import org.insightcentre.nlp.saffron.taxonomy.metrics.ScoreFactory;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MulticlassRelationClassifier;

public interface KGSearch {
	public default KnowledgeGraph extractKnowledgeGraph(Map<String, Term> termMap, Set<TypedLink.Type> relationTypes) {
        return extractKnowledgeGraphWithDenialAndAllowanceList(termMap, Collections.EMPTY_SET, Collections.EMPTY_SET, relationTypes);
    }
    
    public KnowledgeGraph extractKnowledgeGraphWithDenialAndAllowanceList(Map<String, Term> termMap, 
            Set<TypedLink> allowanceList, Set<TypedLink> denialList, final Set<TypedLink.Type> relationTypes);
    
    
    
    public static KGSearch create(TaxonomySearchConfiguration configTaxo, 
    		KnowledgeGraphExtractionConfiguration configKG,
            MulticlassRelationClassifier<String> classifier, Set<String> terms) {
        return create(configTaxo, configKG, classifier, terms, new DefaultSaffronListener());                
    }
    
    //FIXME The classifier should be of terms or concepts, not strings
    public static KGSearch create(TaxonomySearchConfiguration configTaxo,
    		KnowledgeGraphExtractionConfiguration configKG,
    		MulticlassRelationClassifier<String> classifier, Set<String> terms, SaffronListener log) {
    	
    	final Score score = ScoreFactory.getInstance(configTaxo, configKG, configTaxo.score, classifier, terms);
    	return new GreedyKG(score, configKG, log);
    }
}
