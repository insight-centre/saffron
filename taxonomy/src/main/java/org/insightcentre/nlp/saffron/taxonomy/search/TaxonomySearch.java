package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.config.TaxonomySearchConfiguration;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.taxonomy.metrics.TaxonomyScore;
import org.insightcentre.nlp.saffron.taxonomy.supervised.MSTTaxoExtract;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;

/**
 * A taxonomy search algorithm
 * @author John McCrae
 */
public interface TaxonomySearch {
    public Taxonomy extractTaxonomy(Map<String, Topic> topicMap);
    
    
    
    public static TaxonomySearch create(TaxonomySearchConfiguration config, 
            SupervisedTaxo classifier, Set<String> topics) {
        final TaxonomyScore score = TaxonomyScore.create(config, config.score, classifier, topics);
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
