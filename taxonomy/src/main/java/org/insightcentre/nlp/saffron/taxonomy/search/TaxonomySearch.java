package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.taxonomy.metrics.Score;
import org.insightcentre.nlp.saffron.taxonomy.metrics.ScoreFactory;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MSTTaxoExtract;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 * A taxonomy search algorithm
 * @author John McCrae
 */
public interface TaxonomySearch {
    public default Taxonomy extractTaxonomy(Map<String, Term> termMap) {
        return extractTaxonomyWithBlackWhiteList(termMap, Collections.EMPTY_SET, Collections.EMPTY_SET);
    }
    
    public Taxonomy extractTaxonomyWithBlackWhiteList(Map<String, Term> termMap, 
            Set<TaxoLink> whiteList, Set<TaxoLink> blackList);
    
    
    
    public static TaxonomySearch create(TaxonomySearchConfiguration config, 
            SupervisedTaxo classifier, Set<String> terms) {
        return create(config, classifier, terms, new DefaultSaffronListener());                
    }
    
    public static TaxonomySearch create(TaxonomySearchConfiguration config, 
            SupervisedTaxo classifier, Set<String> terms, SaffronListener log) {
        final Score score = ScoreFactory.getInstance(config, config.score, classifier, terms);
        switch(config.algorithm) {
            case greedy:
                return new Greedy(score);
            case beam:
                return new BeamSearch(score, config.beamSize);
            case mst:
                return new MSTTaxoExtract(classifier);
        }
        throw new IllegalArgumentException("Unknown algorithm");
    }
}
